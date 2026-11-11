package ph.edu.lendly;

import org.springframework.dao.DataAccessException;

import ph.edu.lendly.Models.Account;
import ph.edu.lendly.Models.Loan;

import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

final class MilestoneWindow extends JFrame {
    private final AccountService accounts;
    private final LoanService loans;
    private Account signedIn;

    MilestoneWindow(AccountService accounts, LoanService loans) {
        super("Lending Ledger and Saving Circle | Milestone 1");
        this.accounts = accounts;
        this.loans = loans;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(690, 430));
        setSize(880, 540);
        setLocationRelativeTo(null);
        showSignIn();
    }

    private void showSignIn() {
        signedIn = null;
        var page = page("Lending Ledger and Saving Circle", "Milestone 1");
        var email = new JTextField(24);
        var password = new JPasswordField(24);
        var signIn = new JButton("Sign in");
        var register = new JButton("Create an account");
        var fields = new JPanel(new GridLayout(0, 2, 10, 9));
        field(fields, "Email address", email);
        field(fields, "Password", password);
        var buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 0));
        buttons.add(signIn);
        buttons.add(register);
        var form = new JPanel(new BorderLayout(0, 14));
        form.add(fields, BorderLayout.NORTH);
        form.add(buttons, BorderLayout.CENTER);
        var body = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        body.add(form);
        page.add(body, BorderLayout.CENTER);

        signIn.addActionListener(
                e ->
                        attempt(
                                () -> {
                                    signedIn =
                                            accounts.signIn(
                                                    email.getText(), readPassword(password));
                                    showLedger(0);
                                }));
        register.addActionListener(e -> attempt(() -> registerAccount(email)));
        setContentPane(page);
        getRootPane().setDefaultButton(signIn);
        revalidate();
        repaint();
    }

    private void registerAccount(JTextField loginEmail) {
        var name = new JTextField(22);
        var email = new JTextField(22);
        var password = new JPasswordField(22);
        var confirm = new JPasswordField(22);
        var form = new JPanel(new GridLayout(0, 2, 10, 9));
        field(form, "Full name", name);
        field(form, "Email address", email);
        field(form, "Password (at least 12 characters)", password);
        field(form, "Confirm password", confirm);
        try {
            if (showForm(form, "Create an account")) {
                Account account =
                        accounts.register(
                                name.getText(),
                                email.getText(),
                                readPassword(password),
                                readPassword(confirm));
                loginEmail.setText(account.email());
                JOptionPane.showMessageDialog(this, "Account created. You can now sign in.");
            }
        } finally {
            password.setText("");
            confirm.setText("");
        }
    }

    private void showLedger(int selectedTab) {
        long actor = signedIn.id();
        var lending = loans.list(actor, "lending");
        var borrowing = loans.list(actor, "borrowing");
        var page = page("Loan proposals", signedIn.name() + "  |  " + signedIn.email());
        var tabs = new JTabbedPane();
        tabs.addTab("Lending (" + lending.size() + ")", proposalTab("lending", lending));
        tabs.addTab("Borrowing (" + borrowing.size() + ")", proposalTab("borrowing", borrowing));
        tabs.setSelectedIndex(selectedTab);
        page.add(tabs, BorderLayout.CENTER);
        var footer = new JPanel(new BorderLayout());
        footer.add(
                new JLabel("Proposals record planned exchanges outside the app."),
                BorderLayout.WEST);
        var signOut = new JButton("Sign out");
        signOut.addActionListener(e -> showSignIn());
        footer.add(signOut, BorderLayout.EAST);
        page.add(footer, BorderLayout.SOUTH);
        setContentPane(page);
        getRootPane().setDefaultButton(null);
        revalidate();
        repaint();
    }

    private JPanel proposalTab(String role, java.util.List<Loan> records) {
        boolean lending = "lending".equals(role);
        var columns =
                new String[] {
                    "ID",
                    "Money or item",
                    lending ? "Borrower" : "Lender",
                    "Start date",
                    "Due date",
                    "Status"
                };
        var model =
                new DefaultTableModel(columns, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
        for (Loan loan : records) {
            model.addRow(
                    new Object[] {
                        loan.id(),
                        description(loan),
                        lending ? loan.borrowerName() : loan.lenderName(),
                        loan.terms().startDate(),
                        loan.terms().dueDate(),
                        "Pending terms"
                    });
        }
        var table = new JTable(model);
        var renderer = new DefaultTableCellRenderer();
        renderer.putClientProperty("html.disable", Boolean.TRUE);
        table.setDefaultRenderer(Object.class, renderer);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setPreferredWidth(38);
        table.getColumnModel().getColumn(1).setPreferredWidth(215);

        var create = new JButton(lending ? "Propose a loan" : "Request a loan");
        create.addActionListener(e -> attempt(() -> createProposal(role)));
        var detail = new JButton("View proposal");
        detail.setEnabled(false);
        table.getSelectionModel()
                .addListSelectionListener(e -> detail.setEnabled(table.getSelectedRow() >= 0));
        detail.addActionListener(
                e ->
                        attempt(
                                () -> {
                                    int row = table.getSelectedRow();
                                    if (row >= 0)
                                        showDetails(
                                                loans.get(signedIn.id(), records.get(row).id()));
                                }));
        var buttons = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 4));
        buttons.add(create);
        buttons.add(detail);
        if (records.isEmpty()) buttons.add(new JLabel("No proposals in this view yet."));
        var panel = new JPanel(new BorderLayout());
        panel.add(buttons, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void createProposal(String role) {
        var email = new JTextField(22);
        var kind = new JComboBox<>(new String[] {"Money", "Item"});
        var amount = new JTextField(22);
        var currency = new JTextField("PHP", 22);
        var item = new JTextField(22);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Manila"));
        var start = new JTextField(today.toString(), 22);
        var due = new JTextField(today.plusDays(7).toString(), 22);
        var acknowledge = new JCheckBox("I acknowledge the proposed terms and dates.");
        boolean lending = "lending".equals(role);

        var fields = new JPanel(new GridLayout(0, 2, 10, 9));
        field(fields, lending ? "Borrower's registered email" : "Lender's registered email", email);
        field(fields, "Proposal type", kind);
        field(fields, "Amount", amount);
        field(fields, "Currency", currency);
        field(fields, "Item description", item);
        field(fields, "Start date (YYYY-MM-DD)", start);
        field(fields, "Due date (YYYY-MM-DD)", due);
        item.setEnabled(false);
        kind.addActionListener(
                e -> {
                    boolean money = kind.getSelectedIndex() == 0;
                    amount.setEnabled(money);
                    currency.setEnabled(money);
                    item.setEnabled(!money);
                });
        var form = new JPanel(new BorderLayout(0, 12));
        form.add(fields, BorderLayout.CENTER);
        form.add(acknowledge, BorderLayout.SOUTH);
        String title = lending ? "Propose a loan" : "Request a loan";
        while (showForm(form, title)) {
            boolean money = kind.getSelectedIndex() == 0;
            try {
                long id =
                        loans.create(
                                signedIn.id(),
                                email.getText(),
                                role,
                                money ? "MONEY" : "ITEM",
                                money ? amount.getText() : "",
                                money ? currency.getText() : "",
                                money ? "" : item.getText(),
                                start.getText().trim(),
                                due.getText().trim(),
                                acknowledge.isSelected());
                showLedger(lending ? 0 : 1);
                showDetails(loans.get(signedIn.id(), id));
                return;
            } catch (Rules.InvalidInput ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void showDetails(Loan loan) {
        var text =
                new JTextArea(
                        """
                        %s
                        Lender: %s
                        Borrower: %s
                        Start date: %s
                        Due date: %s
                        Status: Pending terms
                        """
                                .formatted(
                                        description(loan),
                                        loan.lenderName(),
                                        loan.borrowerName(),
                                        loan.terms().startDate(),
                                        loan.terms().dueDate()),
                        7,
                        38);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(
                this,
                new JScrollPane(text),
                "Proposal #" + loan.id(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private static String description(Loan loan) {
        var terms = loan.terms();
        return "ITEM".equals(terms.kind())
                ? terms.itemDescription()
                : terms.currency() + " " + terms.amount().toPlainString();
    }

    private static JPanel page(String title, String subtitle) {
        var content = new JPanel(new BorderLayout(0, 18));
        content.setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 24));
        var header = new JPanel(new GridLayout(0, 1, 0, 5));
        var heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 25f));
        header.add(heading);
        var subtitleLabel = new JLabel();
        subtitleLabel.putClientProperty("html.disable", Boolean.TRUE);
        subtitleLabel.setText(subtitle);
        header.add(subtitleLabel);
        content.add(header, BorderLayout.NORTH);
        return content;
    }

    private static void field(JPanel form, String name, JComponent input) {
        var label = new JLabel(name);
        label.setLabelFor(input);
        form.add(label);
        form.add(input);
    }

    private static String readPassword(JPasswordField field) {
        char[] value = field.getPassword();
        try {
            return new String(value);
        } finally {
            Arrays.fill(value, '\0');
            field.setText("");
        }
    }

    private boolean showForm(JPanel form, String title) {
        return JOptionPane.showConfirmDialog(
                        this, form, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                == JOptionPane.OK_OPTION;
    }

    private void attempt(Runnable action) {
        try {
            action.run();
        } catch (Rules.InvalidInput | Rules.NotFound ex) {
            showError(ex.getMessage());
        } catch (DataAccessException ex) {
            showError("Could not access the ledger. Check that its data folder is writable.");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Check the details", JOptionPane.WARNING_MESSAGE);
    }
}
