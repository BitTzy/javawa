package ph.edu.lendly;

import static ph.edu.lendly.LendlyView.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.springframework.dao.DataAccessException;
import ph.edu.lendly.Models.Account;
import ph.edu.lendly.Models.Loan;

final class MilestoneWindow extends JFrame {
    MilestoneWindow(AccountService accounts, LoanService loans) {
        super("Lendly | Lending Ledger and Saving Circle");
        setRootPane(new LendlyWorkspace(accounts, loans));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(850, 560));
        setSize(1040, 700);
        setLocationRelativeTo(null);
    }
}

final class LendlyWorkspace extends JRootPane {
    private final AccountService accounts;
    private final LoanService loans;
    private Account signedIn;
    private String section = "Overview";

    LendlyWorkspace(AccountService accounts, LoanService loans) {
        this.accounts = accounts;
        this.loans = loans;
        showSignIn("", "");
    }

    private void showSignIn(String rememberedEmail, String message) {
        signedIn = null;
        var email = input(rememberedEmail);
        var password = input(new JPasswordField());
        var page = new FormPage("Welcome back", "Sign in to your lending ledger.", null, true);
        if (!message.isEmpty()) page.add(note(message, GREEN), 12);
        page.add(field("Email address", email), 18);
        page.add(field("Password", password), 14);
        page.add(label("Use your registered email and password.", 11, false, MUTED), 12);
        page.primary("Sign in", () -> {
            String address = email.getText();
            String secret = password(password);
            page.authenticate(() -> accounts.signIn(address, secret), account -> {
                signedIn = account;
                try {
                    showDashboard();
                } catch (DataAccessException ex) {
                    signedIn = null;
                    throw ex;
                }
            });
        });
        page.secondary("Explore the demo", this::showDemo);
        page.footer("Your money stays with you. Lendly only keeps records.",
                "Create an account", () -> showRegistration(email.getText()));
        page.show(email);
    }

    private void showRegistration(String rememberedEmail) {
        var name = input("");
        var email = input(rememberedEmail);
        var password = input(new JPasswordField());
        var confirmation = input(new JPasswordField());
        var page = new FormPage("Create an account",
                "Use your registered email to exchange proposals with friends.", null, true);
        page.add(field("Full name", name), 18);
        page.add(field("Email address", email), 13);
        page.add(field("Password (at least 12 characters)", password), 13);
        page.add(field("Confirm password", confirmation), 13);
        page.primary("Create account", () -> {
            String fullName = name.getText();
            String address = email.getText();
            String secret = password(password);
            String repeated = password(confirmation);
            page.authenticate(() -> accounts.register(fullName, address, secret, repeated),
                    account -> showSignIn(account.email(), "Account created. Sign in to open your ledger."));
        });
        page.secondary("Back to sign in", () -> showSignIn(email.getText(), ""));
        page.show(name);
    }

    private void showDemo() {
        var body = card(WHITE);
        append(body, label("Explore the demo", 30, true, INK), 0);
        append(body, note("Open an example record to see what a proposal looks like in the ledger.", MUTED), 8);
        var example = card(MINT);
        append(example, label("USB-C charger", 19, true, INK), 0);
        append(example, note("Alex Rivera · Item loan", MUTED), 6);
        append(example, button("View example proposal", true, this::showDemoProposal), 12);
        append(body, example, 20);
        append(body, row(8,
                button("Back to sign in", false, () -> showSignIn("", "")),
                button("Create an account", false, () -> showRegistration(""))), 18);
        showPage(scroll(top(body)), null, null);
    }

    private void showDemoProposal() {
        var page = form("Example proposal", "Sample item loan", null);
        page.add(summary("Item", "USB-C charger"), 16);
        page.add(pair(summary("Lender", "Alex Rivera"), summary("Borrower", "Sam Reyes")), 10);
        page.add(summary("Status", "Pending terms"), 10);
        page.primary("Back to demo", this::showDemo);
        page.show(page.primary);
    }

    private void showDashboard() {
        var all = loans.list(signedIn.id(), "all");
        section = "Overview";
        long lending = all.stream().filter(loan -> loan.lenderId() == signedIn.id()).count();
        LocalDate today = today();
        long dueToday = all.stream().filter(loan -> loan.terms().dueDate().equals(today)).count();
        long upcoming = all.stream().filter(loan -> loan.terms().dueDate().isAfter(today)).count();
        var body = stack();
        var title = new JPanel(new BorderLayout(14, 0));
        title.setOpaque(false);
        var greeting = stack();
        append(greeting, label("A little overview", 30, true, INK), 0);
        append(greeting, note("Welcome, " + signedIn.name() + ".", MUTED), 6);
        title.add(greeting, BorderLayout.CENTER);
        title.add(top(button("Propose a loan", true, () -> {
            section = Role.LENDING.label;
            showParticipant(new Draft(Role.LENDING));
        })), BorderLayout.EAST);
        append(body, title, 0);
        var metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setOpaque(false);
        metrics.add(overviewCard("Lending", lending));
        metrics.add(overviewCard("Borrowing", all.size() - lending));
        metrics.add(overviewCard("Due today", dueToday));
        metrics.add(overviewCard("Upcoming", upcoming));
        append(body, metrics, 18);

        var recent = card(WHITE);
        append(recent, label("Recent proposals", 18, true, INK), 0);
        if (all.isEmpty()) append(recent, note("New proposals will appear here.", MUTED), 12);
        all.stream().limit(4).forEach(loan -> addLoanLink(recent, loan));
        var reminders = card(MINT);
        append(reminders, label("Coming up", 18, true, INK), 0);
        append(reminders, note((upcoming + dueToday) + " proposed due dates from today onward.", MUTED, 220), 9);
        append(reminders, button("View reminders", false, () -> navigate(this::showReminders)), 12);
        append(body, pair(recent, reminders), 16);
        showPage(scroll(top(body)), null, null);
    }

    private JPanel overviewCard(String title, long count) {
        var card = card(WHITE);
        card.setBorder(new EmptyBorder(15, 17, 15, 17));
        append(card, label(title, 15, true, INK), 0);
        append(card, label(Long.toString(count), 29, true, GREEN), 6);
        return card;
    }

    private void showLedger(Role role) {
        var records = loans.list(signedIn.id(), role.key);
        section = role.label;
        var card = card(WHITE);
        append(card, label("Your " + role.key + " ledger", 30, true, INK), 0);
        append(card, note(role == Role.LENDING ? "Proposals to lend money or an item."
                : "Proposals to borrow money or an item.", MUTED), 7);
        var create = button(role.action, true, () -> showParticipant(new Draft(role)));
        if (records.isEmpty()) {
            append(card, note("You have no " + role.key + " proposals yet.", MUTED), 23);
            append(card, create, 18);
            showPage(scroll(top(card)), create, create);
            return;
        }

        var table = proposalTable(role, records);
        Runnable open = () -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                long id = records.get(table.convertRowIndexToModel(row)).id();
                navigate(() -> showDetails(role, id));
            }
        };
        var details = button("View proposal", false, open);
        details.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(event -> details.setEnabled(table.getSelectedRow() >= 0));
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && table.rowAtPoint(event.getPoint()) >= 0) open.run();
            }
        });
        table.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "open");
        table.getActionMap().put("open", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { open.run(); }
        });
        append(card, row(8, create, details), 18);
        append(card, ledgerFilters(table, records), 14);
        var tableScroll = new JScrollPane(table);
        tableScroll.setColumnHeaderView(table.getTableHeader());
        tableScroll.setBorder(BorderFactory.createLineBorder(LINE));
        tableScroll.getViewport().setBackground(WHITE);
        tableScroll.setPreferredSize(new Dimension(550, 300));
        append(card, tableScroll, 18);
        showPage(scroll(top(card)), create, create);
    }

    private JPanel ledgerFilters(JTable table, List<Loan> records) {
        var search = input("");
        var kind = new JComboBox<>(new String[] {"All types", "Money", "Item"});
        var sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        Runnable apply = () -> {
            String query = search.getText().strip().toLowerCase(Locale.ROOT);
            String type = switch (kind.getSelectedIndex()) {
                case 1 -> "MONEY";
                case 2 -> "ITEM";
                default -> "";
            };
            sorter.setRowFilter(new RowFilter<>() {
                @Override public boolean include(Entry<? extends TableModel, ? extends Integer> row) {
                    var loan = records.get(row.getIdentifier());
                    if (!type.isEmpty() && !type.equals(loan.terms().kind())) return false;
                    return row.getStringValue(0).toLowerCase(Locale.ROOT).contains(query)
                            || row.getStringValue(1).toLowerCase(Locale.ROOT).contains(query);
                }
            });
        };
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { apply.run(); }
            @Override public void removeUpdate(DocumentEvent event) { apply.run(); }
            @Override public void changedUpdate(DocumentEvent event) { apply.run(); }
        });
        kind.addActionListener(event -> apply.run());
        return pair(field("Search proposals", search), field("Proposal type", kind));
    }

    private JTable proposalTable(Role role, List<Loan> records) {
        var columns = new String[] {"Money or item", role.party, "Start date", "Due date", "Status"};
        var model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        for (Loan loan : records) {
            model.addRow(new Object[] {
                    description(loan), role == Role.LENDING ? loan.borrowerName() : loan.lenderName(),
                    loan.terms().startDate(), loan.terms().dueDate(), status(loan)
            });
        }
        var table = new JTable(model);
        table.getAccessibleContext().setAccessibleName(role.label + " proposals");
        var renderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                setToolTipText(getText());
                return this;
            }
        };
        renderer.putClientProperty("html.disable", Boolean.TRUE);
        renderer.setBorder(new EmptyBorder(0, 7, 0, 7));
        table.setDefaultRenderer(Object.class, renderer);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(34);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        table.setForeground(INK);
        table.setSelectionForeground(INK);
        table.setSelectionBackground(MINT);
        table.setGridColor(LINE);
        table.setShowVerticalLines(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        return table;
    }

    private void showParticipant(Draft draft) {
        var page = form(draft.role.action, "Choose a registered friend and what the proposal covers.", "1 / 4");
        var email = input(draft.email);
        page.add(field(draft.role.party + "’s registered email", email), 18);
        var money = new JRadioButton("Money", draft.money);
        var item = new JRadioButton("Item", !draft.money);
        var kinds = new ButtonGroup();
        kinds.add(money);
        kinds.add(item);
        money.setOpaque(false);
        item.setOpaque(false);
        money.setForeground(INK);
        item.setForeground(INK);
        page.add(label("Proposal type", 13, true, INK), 15);
        page.add(row(12, money, item), 6);
        page.primary("Continue", () -> {
            Account friend;
            try {
                friend = accounts.byEmail(email.getText());
            } catch (Rules.NotFound ex) {
                throw new Rules.InvalidInput("No registered friend has that email. Ask them to register first.");
            }
            Rules.require(friend.id() != signedIn.id(), "You cannot create a loan with yourself.");
            draft.email = friend.email();
            draft.friend = friend.name();
            draft.money = money.isSelected();
            showLoanDetails(draft);
        });
        page.secondary("Cancel", () -> navigate(() -> showLedger(draft.role)));
        page.show(email);
    }

    private void showLoanDetails(Draft draft) {
        var page = form(draft.money ? "Money details" : "Item details",
                draft.money ? "Enter the proposed amount and currency." : "Describe the item being exchanged.", "2 / 4");
        var amount = input(draft.amount);
        var currency = input(draft.currency);
        var item = input(draft.item);
        if (draft.money) {
            page.add(field("Amount", amount), 18);
            page.add(field("Currency code (e.g. PHP)", currency), 14);
        } else {
            page.add(field("Item description", item), 18);
        }
        page.primary("Continue", () -> {
            if (draft.money) {
                draft.amount = Rules.money(amount.getText()).toPlainString();
                draft.currency = Rules.currency(currency.getText());
            } else {
                draft.item = Rules.required(item.getText(), 200, "Item description");
            }
            showSchedule(draft);
        });
        page.secondary("Back", () -> {
            if (draft.money) {
                draft.amount = amount.getText();
                draft.currency = currency.getText();
            } else {
                draft.item = item.getText();
            }
            showParticipant(draft);
        });
        page.show(draft.money ? amount : item);
    }

    private void showSchedule(Draft draft) {
        var page = form("Set the dates", "Choose a start date and the date the money or item is due.", "3 / 4");
        var start = input(draft.start);
        var due = input(draft.due);
        page.add(field("Start date (YYYY-MM-DD)", start), 18);
        page.add(field("Due date (YYYY-MM-DD)", due), 14);
        page.primary("Review proposal", () -> {
            var startDate = Rules.date(start.getText().trim());
            var dueDate = Rules.date(due.getText().trim());
            Rules.require(!startDate.isAfter(dueDate), "The due date must be on or after the start date.");
            draft.start = startDate.toString();
            draft.due = dueDate.toString();
            showReview(draft);
        });
        page.secondary("Back", () -> {
            draft.start = start.getText();
            draft.due = due.getText();
            showLoanDetails(draft);
        });
        page.show(start);
    }

    private void showReview(Draft draft) {
        var page = form("Review proposal", "Check the terms before saving them to your ledger.", "4 / 4");
        page.add(summary(draft.role.party, draft.friend), 16);
        page.add(summary(draft.money ? "Amount" : "Item", draft.money ? draft.currency + " " + draft.amount : draft.item), 10);
        page.add(pair(summary("Start date", draft.start), summary("Due date", draft.due)), 10);
        var acknowledge = new JCheckBox("I acknowledge the proposed terms and dates.");
        acknowledge.setOpaque(false);
        acknowledge.setForeground(INK);
        page.add(acknowledge, 16);
        page.primary("Save proposal", () -> {
            long id = loans.create(signedIn.id(), draft.email, draft.role.key,
                    draft.money ? "MONEY" : "ITEM", draft.money ? draft.amount : "",
                    draft.money ? draft.currency : "", draft.money ? "" : draft.item,
                    draft.start, draft.due, acknowledge.isSelected());
            showSaved(draft.role, id);
        });
        page.secondary("Back", () -> showSchedule(draft));
        page.show(acknowledge);
    }

    private void showDetails(Role role, long id) {
        var loan = loans.get(signedIn.id(), id);
        section = role.label;
        var page = form("Proposal #" + loan.id(), status(loan), null);
        page.add(summary("Money or item", description(loan)), 16);
        page.add(pair(summary("Lender", loan.lenderName(), 218), summary("Borrower", loan.borrowerName(), 218)), 10);
        page.add(pair(summary("Start date", loan.terms().startDate().toString()),
                summary("Due date", loan.terms().dueDate().toString())), 10);
        page.add(note("Participants arrange the exchange outside Lendly.", MUTED), 15);
        page.primary("Back to " + role.key, () -> showLedger(role));
        page.show(page.primary);
    }

    private void addLoanLink(JPanel panel, Loan loan) {
        var role = loan.lenderId() == signedIn.id() ? Role.LENDING : Role.BORROWING;
        var entry = stack();
        append(entry, note(description(loan), INK, 225), 0);
        append(entry, label(loan.terms().dueDate() + " · " + status(loan), 12, false, MUTED), 4);
        append(entry, button("View proposal #" + loan.id(), false, () -> navigate(() -> showDetails(role, loan.id()))), 5);
        append(panel, entry, 12);
    }

    private void showReminders() {
        var records = loans.list(signedIn.id(), "all").stream()
                .sorted(java.util.Comparator.comparing(loan -> loan.terms().dueDate()))
                .toList();
        section = "Reminders";
        var body = stack();
        append(body, label("A friendly follow-up", 30, true, INK), 0);
        append(body, note("Keep track of the dates entered in your proposals.", MUTED), 6);
        var upcoming = card(MINT);
        var past = card(WHITE);
        append(upcoming, label("Today and upcoming", 17, true, INK), 0);
        append(past, label("Past proposed dates", 17, true, INK), 0);
        LocalDate today = today();
        for (Loan loan : records) addLoanLink(loan.terms().dueDate().isBefore(today) ? past : upcoming, loan);
        if (upcoming.getComponentCount() == 1) append(upcoming, note("No upcoming dates.", MUTED, 220), 9);
        if (past.getComponentCount() == 1) append(past, note("No past dates.", MUTED, 220), 9);
        append(body, pair(upcoming, past), 18);
        showPage(scroll(top(body)), null, null);
    }

    private void showSavings() {
        var savings = SavingsProgress.load(signedIn.email());
        section = "Savings";
        var body = stack();
        append(body, label("Your savings", 30, true, INK), 0);
        append(body, note("Set a goal and track your contributions on this computer.", MUTED), 6);

        var progress = card(MINT);
        var goal = note(savings.hasGoal() ? savings.title() : "Savings goal", INK, 225);
        goal.setFont(goal.getFont().deriveFont(Font.BOLD, 18f));
        append(progress, goal, 0);
        if (savings.hasGoal()) {
            append(progress, label("PHP " + savings.saved().toPlainString(), 28, true, GREEN), 8);
            var bar = new JProgressBar(0, 100);
            bar.setValue(savings.percent());
            bar.setStringPainted(true);
            bar.setForeground(GREEN);
            bar.setBackground(WHITE);
            bar.getAccessibleContext().setAccessibleName("Savings goal progress");
            append(progress, bar, 12);
            append(progress, note("Target · PHP " + savings.target().toPlainString(), MUTED, 220), 8);
        } else {
            append(progress, note("Set a target to start tracking contributions.", MUTED, 220), 8);
        }
        var actions = card(WHITE);
        append(actions, label("Manage savings", 18, true, INK), 0);
        append(actions, button(savings.hasGoal() ? "Edit savings goal" : "Set a savings goal", true, this::showSavingsGoal), 12);
        if (savings.hasGoal()) {
            append(actions, button("Record a contribution", false, this::showContribution), 10);
        }
        append(body, pair(progress, actions), 16);
        var activity = card(WHITE);
        append(activity, label("Recent contributions", 18, true, INK), 0);
        if (savings.recent().isEmpty()) append(activity, note("No contributions recorded yet.", MUTED), 8);
        for (var contribution : savings.recent()) {
            append(activity, label(contribution.date() + "  ·  PHP " + contribution.amount().toPlainString(), 14, false, INK), 9);
        }
        append(body, activity, 16);
        showPage(scroll(top(body)), null, null);
    }

    private void showSavingsGoal() {
        var savings = SavingsProgress.load(signedIn.email());
        var title = input(savings.title());
        var target = input(savings.hasGoal() ? savings.target().toPlainString() : "");
        var page = form("Set a savings goal", "Name your goal and enter the target amount.", null);
        page.add(field("Goal name", title), 18);
        page.add(field("Target amount (PHP)", target), 14);
        page.primary("Save goal", () -> {
            var name = Rules.required(title.getText(), 60, "Goal name");
            var amount = Rules.money(target.getText());
            SavingsProgress.load(signedIn.email()).withGoal(name, amount).save(signedIn.email());
            showSavings();
        });
        page.secondary("Cancel", () -> navigate(this::showSavings));
        page.show(title);
    }

    private void showContribution() {
        var amount = input("");
        var page = form("Record a contribution", "Record money you’ve already set aside for your goal.", null);
        page.add(field("Contribution amount (PHP)", amount), 18);
        page.primary("Save contribution", () -> {
            var savings = SavingsProgress.load(signedIn.email());
            Rules.require(savings.hasGoal(), "Set a savings goal first.");
            var value = Rules.money(amount.getText());
            savings.withContribution(today(), value).save(signedIn.email());
            showSavings();
        });
        page.secondary("Cancel", () -> navigate(this::showSavings));
        page.show(amount);
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Manila"));
    }

    private void showSaved(Role role, long id) {
        var page = form("Proposal saved", "Proposal #" + id + " is now in your " + role.key + " ledger.", null);
        page.add(summary("Status", "Pending terms"), 16);
        page.primary("View proposal", () -> showDetails(role, id));
        page.secondary("Back to " + role.key, () -> navigate(() -> showLedger(role)));
        page.show(page.primary);
    }

    private static JPanel summary(String title, String value) {
        return summary(title, value, 420);
    }

    private static JPanel summary(String title, String value, int wrapWidth) {
        var summary = stack();
        append(summary, label(title, 13, true, MUTED), 0);
        append(summary, note(value, INK, wrapWidth), 3);
        return summary;
    }

    private static String description(Loan loan) {
        var terms = loan.terms();
        return "ITEM".equals(terms.kind()) ? terms.itemDescription()
                : terms.currency() + " " + terms.amount().toPlainString();
    }

    private static String status(Loan loan) {
        return "PENDING_TERMS".equals(loan.status()) ? "Pending terms" : loan.status();
    }

    private FormPage form(String title, String hint, String step) {
        return new FormPage(title, hint, step, false);
    }

    private void showAuthPage(JPanel content, JButton defaultButton, JComponent focus) {
        var left = new JPanel(new BorderLayout());
        left.setBackground(INK);
        left.setBorder(new EmptyBorder(42, 43, 25, 43));
        var story = stack();
        append(story, row(5, badge("L", GREEN, WHITE), label("lendly", 16, true, WHITE)), 0);
        append(story, label("PERSONAL LENDING, MADE SIMPLE", 10, false, MINT), 54);
        append(story, label("Good things", 34, true, WHITE), 15);
        append(story, label("come back.", 34, true, WHITE), 0);
        append(story, label("Keep track of what you lend.", 15, false, WHITE), 19);
        append(story, label("Make shared goals easier.", 15, false, WHITE), 0);
        var reminder = card(WHITE);
        reminder.setBorder(new EmptyBorder(16, 17, 16, 17));
        append(reminder, label("A little nudge, right on time", 11, true, INK), 0);
        var reminderTitle = new JPanel(new BorderLayout(8, 0));
        reminderTitle.setOpaque(false);
        reminderTitle.setBorder(new EmptyBorder(0, 0, 0, 18));
        reminderTitle.add(label("USB-C charger", 15, true, INK), BorderLayout.WEST);
        reminderTitle.add(badge("Due today", new Color(0xFFF2D9), new Color(0x765414)), BorderLayout.EAST);
        append(reminder, reminderTitle, 19);
        append(reminder, label("Alex Rivera · Expected back today", 10, false, MUTED), 4);
        append(reminder, label("One shared record. Fewer forgotten returns.", 10, false, MUTED), 14);
        append(story, reminder, 17);
        append(story, label("Money, books, chargers, umbrellas.", 11, false, MINT), 17);
        append(story, label("Keep the friendship. Remember the details.", 11, false, MINT), 0);
        left.add(scroll(top(story)), BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(425, 1));

        var aligned = new JPanel(new GridBagLayout());
        aligned.setBackground(WHITE);
        var center = new GridBagConstraints();
        center.insets = new Insets(25, 15, 25, 15);
        aligned.add(content, center);
        var right = scroll(aligned);
        right.setBackground(WHITE);
        right.setOpaque(true);
        right.setPreferredSize(new Dimension(565, 1));

        var shell = new JPanel(new GridBagLayout());
        var column = new GridBagConstraints();
        column.weightx = 0.43;
        column.weighty = 1;
        column.fill = GridBagConstraints.BOTH;
        shell.add(left, column);
        column.gridx = 1;
        column.weightx = 0.57;
        shell.add(right, column);
        display(shell, defaultButton, focus);
    }

    private void showPage(JComponent content, JButton defaultButton, JComponent focus) {
        var shell = new JPanel(new BorderLayout());
        shell.setBackground(CANVAS);
        if (signedIn != null) {
            var sidebar = new JPanel(new BorderLayout());
            sidebar.setBackground(WHITE);
            sidebar.setBorder(new EmptyBorder(22, 15, 17, 15));
            sidebar.setPreferredSize(new Dimension(158, 1));
            var links = stack();
            append(links, row(4, badge("L", GREEN, WHITE), brand()), 0);
            append(links, navButton("Overview", this::showDashboard), 22);
            append(links, navButton("Lending", () -> showLedger(Role.LENDING)), 7);
            append(links, navButton("Borrowing", () -> showLedger(Role.BORROWING)), 7);
            append(links, navButton("Savings", this::showSavings), 7);
            append(links, navButton("Reminders", this::showReminders), 7);
            sidebar.add(links, BorderLayout.NORTH);
            var account = stack();
            append(account, note(signedIn.name(), MUTED, 124), 0);
            append(account, navButton("Sign out", () -> showSignIn("", "")), 7);
            sidebar.add(account, BorderLayout.SOUTH);
            shell.add(sidebar, BorderLayout.WEST);
        }
        var work = new JPanel(new BorderLayout(0, 18));
        work.setOpaque(false);
        work.setBorder(new EmptyBorder(21, 24, 20, 24));
        work.add(signedIn == null ? brand() : label(section, 13, true, MUTED), BorderLayout.NORTH);
        work.add(content, BorderLayout.CENTER);
        shell.add(work, BorderLayout.CENTER);
        display(shell, defaultButton, focus);
    }

    private JButton navButton(String title, Runnable open) {
        var button = button(title, false, !section.equals(title), () -> navigate(open));
        button.setHorizontalAlignment(SwingConstants.LEADING);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return button;
    }

    private void display(JPanel shell, JButton defaultButton, JComponent focus) {
        setContentPane(shell);
        setDefaultButton(defaultButton);
        revalidate();
        repaint();
        if (focus != null) SwingUtilities.invokeLater(focus::requestFocusInWindow);
    }

    private void navigate(Runnable action) {
        try {
            action.run();
        } catch (Rules.InvalidInput | Rules.NotFound | DataAccessException ex) {
            JOptionPane.showMessageDialog(this, message(ex), "Lendly", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static String password(JPasswordField field) {
        char[] characters = field.getPassword();
        try {
            return new String(characters);
        } finally {
            Arrays.fill(characters, '\0');
            field.setText("");
        }
    }

    private static String message(Throwable exception) {
        if (exception instanceof Rules.InvalidInput || exception instanceof Rules.NotFound) return exception.getMessage();
        return "Could not access the ledger. Check that its data folder is writable and try again.";
    }

    private final class FormPage {
        private final JPanel card;
        private final JTextArea error;
        private final JPanel buttons = row(8);
        private final boolean auth;
        private JButton primary;
        private JButton secondary;
        private JButton footerAction;
        private String footerNote;

        private FormPage(String title, String hint, String step, boolean auth) {
            this.auth = auth;
            card = card(WHITE, auth ? 318 : 440);
            if (auth) card.setBorder(new EmptyBorder(0, 0, 0, 0));
            error = note("", ERROR_COLOR, auth ? 305 : 420);
            if (step != null) add(label("Step " + step, 13, true, GREEN), 0);
            add(label(title, auth ? 23 : 31, true, INK), step == null ? 0 : 6);
            add(note(hint, MUTED, auth ? 305 : 420), 8);
        }

        private void add(JComponent item, int gap) {
            LendlyView.append(card, item, gap);
        }

        private void authButton(JButton button, int height) {
            button.setMinimumSize(new Dimension(90, height));
            button.setPreferredSize(new Dimension(button.getPreferredSize().width, height));
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        }

        private void primary(String title, Runnable action) {
            primary = button(title, true, () -> {
                error.setText("");
                try {
                    action.run();
                } catch (Rules.InvalidInput | Rules.NotFound | DataAccessException ex) {
                    error.setText(message(ex));
                }
            });
            if (!auth) buttons.add(primary);
        }

        private void secondary(String title, Runnable action) {
            secondary = button(title, false, auth, action);
            if (!auth) buttons.add(secondary);
        }

        private void footer(String text, String actionLabel, Runnable action) {
            footerNote = text;
            footerAction = button(actionLabel, false, true, action);
        }

        private void show(JComponent focus) {
            add(error, 7);
            if (auth) {
                authButton(primary, 34);
                authButton(secondary, 34);
                add(primary, 5);
                add(secondary, 11);
                if (footerAction != null) {
                    add(label(footerNote, 11, false, MUTED), 13);
                    authButton(footerAction, 29);
                    footerAction.setMaximumSize(footerAction.getPreferredSize());
                    add(footerAction, 8);
                }
                showAuthPage(card, primary, focus);
                return;
            }
            add(buttons, 5);
            var aligned = new JPanel(new GridBagLayout());
            aligned.setOpaque(false);
            var constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.HORIZONTAL;
            if (signedIn != null) {
                constraints.weightx = 0.69;
                aligned.add(card, constraints);
                constraints.gridx = 1;
                constraints.weightx = 0.31;
                constraints.anchor = GridBagConstraints.NORTH;
                constraints.insets = new Insets(0, 15, 0, 0);
                var guidance = card(MINT, 175);
                guidance.setBorder(new EmptyBorder(20, 17, 20, 17));
                boolean savings = section.equals("Savings");
                LendlyView.append(guidance, label(savings ? "Your progress" : "A shared record", 15, true, INK), 0);
                LendlyView.append(guidance, note(savings
                        ? "Savings entries are stored on this computer."
                        : "Both registered participants can view a saved proposal in their own ledger.", MUTED, 130), 8);
                aligned.add(guidance, constraints);
            } else {
                aligned.add(card, constraints);
            }
            showPage(scroll(top(aligned)), primary, focus);
        }

        private void authenticate(Supplier<Account> action, Consumer<Account> success) {
            primary.setEnabled(false);
            secondary.setEnabled(false);
            if (footerAction != null) footerAction.setEnabled(false);
            new SwingWorker<Account, Void>() {
                @Override protected Account doInBackground() { return action.get(); }

                @Override protected void done() {
                    primary.setEnabled(true);
                    secondary.setEnabled(true);
                    if (footerAction != null) footerAction.setEnabled(true);
                    try {
                        success.accept(get());
                    } catch (DataAccessException ex) {
                        error.setText(message(ex));
                    } catch (ExecutionException ex) {
                        error.setText(message(ex.getCause()));
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        error.setText("The account request was interrupted. Please try again.");
                    }
                }
            }.execute();
        }
    }

    private enum Role {
        LENDING("lending", "Lending", "Borrower", "Propose a loan"),
        BORROWING("borrowing", "Borrowing", "Lender", "Request a loan");

        private final String key;
        private final String label;
        private final String party;
        private final String action;

        Role(String key, String label, String party, String action) {
            this.key = key;
            this.label = label;
            this.party = party;
            this.action = action;
        }
    }

    private static final class Draft {
        private final Role role;
        private String email = "";
        private String friend = "";
        private boolean money = true;
        private String amount = "";
        private String currency = "PHP";
        private String item = "";
        private String start;
        private String due;

        private Draft(Role role) {
            this.role = role;
            var today = today();
            start = today.toString();
            due = today.plusDays(7).toString();
        }
    }
}
