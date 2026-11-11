package ph.edu.lendly;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Models {
    private Models() {}

    public record Account(long id, String name, String email) {}

    public record Terms(
            int version,
            String kind,
            BigDecimal amount,
            String currency,
            String itemDescription,
            LocalDate startDate,
            LocalDate dueDate,
            long proposedBy) {}

    public record Loan(
            long id,
            long lenderId,
            long borrowerId,
            long initiatorId,
            String status,
            int currentVersion,
            String lenderName,
            String borrowerName,
            Terms terms) {}
}
