package ph.edu.lendly;

import static ph.edu.lendly.Models.*;
import static ph.edu.lendly.Rules.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Service
public class LoanService {
    private static final String SELECT_LOAN =
            """
            SELECT l.*, u.name lender_name, b.name borrower_name, t.*
            FROM loan l JOIN app_user u ON u.id=l.lender_id JOIN app_user b ON b.id=l.borrower_id
            JOIN loan_terms t ON t.loan_id=l.id AND t.version=l.current_version
            """;
    private static final String PARTICIPANT = "(l.lender_id=? OR l.borrower_id=?)";
    private final JdbcTemplate jdbc;
    private final Database db;
    private final AccountService accounts;

    public LoanService(JdbcTemplate jdbc, Database db, AccountService accounts) {
        this.jdbc = jdbc;
        this.db = db;
        this.accounts = accounts;
    }

    @Transactional
    public long create(
            long actor,
            String email,
            String role,
            String kind,
            String amount,
            String currency,
            String item,
            String start,
            String due,
            boolean acknowledgement) {
        require(acknowledgement, "Confirm the proposed terms and dates before submitting.");
        require("borrowing".equals(role) || "lending".equals(role), "Choose Lending or Borrowing.");
        Account counterpart;
        try {
            counterpart = accounts.byEmail(email);
        } catch (NotFound ex) {
            throw new InvalidInput(
                    "No registered friend has that email. Ask them to register first.");
        }
        require(actor != counterpart.id(), "You cannot create a loan with yourself.");
        Terms terms = validateTerms(1, actor, kind, amount, currency, item, start, due);
        long lender = role.equals("lending") ? actor : counterpart.id();
        long borrower = role.equals("borrowing") ? actor : counterpart.id();
        long id =
                db.insert(
                        "INSERT INTO"
                            + " loan(lender_id,borrower_id,initiator_id,status,current_version)"
                            + " VALUES(?,?,?,'PENDING_TERMS',1)",
                        lender,
                        borrower,
                        actor);
        insertTerms(id, terms);
        return id;
    }

    public Loan get(long actor, long id) {
        var rows =
                jdbc.query(
                        SELECT_LOAN + " WHERE l.id=? AND " + PARTICIPANT,
                        this::mapLoan,
                        id,
                        actor,
                        actor);
        if (rows.isEmpty()) throw new NotFound();
        return rows.getFirst();
    }

    public List<Loan> list(long actor, String role) {
        return switch (role) {
            case "lending" ->
                    jdbc.query(
                            SELECT_LOAN + " WHERE l.lender_id=? ORDER BY l.id DESC",
                            this::mapLoan,
                            actor);
            case "borrowing" ->
                    jdbc.query(
                            SELECT_LOAN + " WHERE l.borrower_id=? ORDER BY l.id DESC",
                            this::mapLoan,
                            actor);
            default ->
                    jdbc.query(
                            SELECT_LOAN + " WHERE " + PARTICIPANT + " ORDER BY l.id DESC",
                            this::mapLoan,
                            actor,
                            actor);
        };
    }

    private Terms validateTerms(
            int version,
            long actor,
            String kind,
            String amount,
            String currency,
            String item,
            String start,
            String due) {
        require("MONEY".equals(kind) || "ITEM".equals(kind), "Choose Money or Item.");
        LocalDate startDate = Rules.date(start);
        LocalDate dueDate = Rules.date(due);
        require(!startDate.isAfter(dueDate), "The due date must be on or after the start date.");
        if (kind.equals("MONEY")) {
            require(
                    text(item, 200, "Item description").isBlank(),
                    "Money loans cannot also contain an item.");
            return new Terms(
                    version,
                    kind,
                    money(amount),
                    Rules.currency(currency),
                    null,
                    startDate,
                    dueDate,
                    actor);
        }
        require(
                text(amount, 20, "Amount").isBlank() && text(currency, 3, "Currency").isBlank(),
                "Item loans cannot also contain money.");
        return new Terms(
                version,
                kind,
                null,
                null,
                required(item, 200, "Item description"),
                startDate,
                dueDate,
                actor);
    }

    private void insertTerms(long id, Terms terms) {
        jdbc.update(
                "INSERT INTO"
                    + " loan_terms(loan_id,version,kind,amount,currency,item_description,start_date,due_date,proposed_by)"
                    + " VALUES(?,?,?,?,?,?,?,?,?)",
                id,
                terms.version(),
                terms.kind(),
                terms.amount(),
                terms.currency(),
                terms.itemDescription(),
                terms.startDate(),
                terms.dueDate(),
                terms.proposedBy());
    }

    private Terms mapTerms(ResultSet row) throws SQLException {
        return new Terms(
                row.getInt("version"),
                row.getString("kind"),
                row.getBigDecimal("amount"),
                row.getString("currency"),
                row.getString("item_description"),
                row.getObject("start_date", LocalDate.class),
                row.getObject("due_date", LocalDate.class),
                row.getLong("proposed_by"));
    }

    private Loan mapLoan(ResultSet row, int index) throws SQLException {
        return new Loan(
                row.getLong("id"),
                row.getLong("lender_id"),
                row.getLong("borrower_id"),
                row.getLong("initiator_id"),
                row.getString("status"),
                row.getInt("current_version"),
                row.getString("lender_name"),
                row.getString("borrower_name"),
                mapTerms(row));
    }
}
