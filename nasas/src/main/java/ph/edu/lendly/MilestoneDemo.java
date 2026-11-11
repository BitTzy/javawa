package ph.edu.lendly;

import ph.edu.lendly.Models.Loan;

final class MilestoneDemo {
    private MilestoneDemo() {}

    static void run(AccountService accounts, LoanService loans) {
        String password = "MilestoneDemo21";
        String anaEmail = "ana@example.test";
        String benEmail = "ben@example.test";
        accounts.register("Ana", anaEmail, password, password);
        accounts.register("Ben", benEmail, password, password);
        var reviewer = accounts.register("Reviewer", "reviewer@example.test", password, password);
        System.out.println("PASS: Registration for both participants");

        var ana = accounts.signIn(anaEmail, password);
        var ben = accounts.signIn(benEmail, password);
        expectInvalid(() -> accounts.signIn(anaEmail, "incorrect"));
        System.out.println("PASS: Sign-in and incorrect password rejection");

        String start = "2026-11-12";
        String due = "2026-11-19";
        long money =
                loans.create(
                        ana.id(),
                        ben.email(),
                        "lending",
                        "MONEY",
                        "250.00",
                        "PHP",
                        "",
                        start,
                        due,
                        true);
        long item =
                loans.create(
                        ben.id(),
                        ana.email(),
                        "borrowing",
                        "ITEM",
                        "",
                        "",
                        "Black umbrella",
                        start,
                        due,
                        true);
        System.out.println("PASS: Money proposal and item request saved");

        var lending = loans.list(ana.id(), "lending");
        var borrowing = loans.list(ben.id(), "borrowing");
        require(
                lending.equals(borrowing)
                        && lending.size() == 2
                        && lending.stream().allMatch(loan -> "PENDING_TERMS".equals(loan.status()))
                        && loans.list(ana.id(), "borrowing").isEmpty()
                        && loans.list(ben.id(), "lending").isEmpty(),
                "Incorrect proposal lists");
        for (Loan loan : lending) {
            var terms = loan.terms();
            String description =
                    "ITEM".equals(terms.kind())
                            ? terms.itemDescription()
                            : terms.currency() + " " + terms.amount().toPlainString();
            System.out.printf(
                    "#%d %s | %s lends to %s | %s to %s | %s%n",
                    loan.id(),
                    description,
                    loan.lenderName(),
                    loan.borrowerName(),
                    terms.startDate(),
                    terms.dueDate(),
                    loan.status());
        }
        require(loans.get(ana.id(), item).equals(loans.get(ben.id(), item)), "Detail mismatch");
        require(loans.list(reviewer.id(), "all").isEmpty(), "Nonparticipant can list proposals");
        try {
            loans.get(reviewer.id(), money);
            throw new IllegalStateException("Nonparticipant can read a proposal");
        } catch (Rules.NotFound expected) {
            System.out.println("PASS: Both participants can view proposals; other accounts cannot");
        }
        expectInvalid(
                () ->
                        loans.create(
                                ben.id(),
                                ana.email(),
                                "borrowing",
                                "ITEM",
                                "",
                                "",
                                "Book",
                                due,
                                start,
                                true));
        require(loans.list(ben.id(), "all").size() == 2, "Invalid proposal was saved");
        System.out.println("PASS: Reversed dates rejected");
    }

    private static void expectInvalid(Runnable operation) {
        try {
            operation.run();
            throw new IllegalStateException("Expected validation to reject the operation");
        } catch (Rules.InvalidInput expected) {
            // The operation was rejected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
