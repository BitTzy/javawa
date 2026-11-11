package ph.edu.lendly;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Currency;
import java.util.Locale;

public final class Rules {
    private Rules() {}

    public static void require(boolean condition, String message) {
        if (!condition) throw new InvalidInput(message);
    }

    public static String text(String value, int max, String field) {
        String result = value == null ? "" : value.trim();
        require(result.length() <= max, field + " is too long.");
        return result;
    }

    public static String required(String value, int max, String field) {
        String result = text(value, max, field);
        require(!result.isBlank(), "Enter " + field.toLowerCase(Locale.ROOT) + ".");
        return result;
    }

    public static String email(String value) {
        String result = required(value, 254, "Email").toLowerCase(Locale.ROOT);
        require(result.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"), "Enter a valid email address.");
        return result;
    }

    public static BigDecimal money(String value) {
        String decimal = required(value, 20, "Amount");
        require(
                decimal.matches("[0-9]{1,12}(\\.[0-9]{1,2})?"),
                "Use a plain positive amount with at most 12 digits and two decimal places.");
        try {
            BigDecimal amount = new BigDecimal(decimal).setScale(2, RoundingMode.UNNECESSARY);
            require(
                    amount.signum() > 0 && amount.compareTo(new BigDecimal("999999999999.99")) <= 0,
                    "Amount must be positive and no more than 999,999,999,999.99.");
            return amount;
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new InvalidInput("Enter a positive amount with at most two decimal places.");
        }
    }

    public static String currency(String value) {
        String result = required(value, 3, "Currency").toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(result);
        } catch (IllegalArgumentException ex) {
            throw new InvalidInput("Choose a valid three-letter currency, such as PHP.");
        }
        return result;
    }

    public static LocalDate date(String value) {
        try {
            LocalDate date = LocalDate.parse(value == null ? "" : value);
            require(
                    date.getYear() >= 1900 && date.getYear() <= 2100,
                    "Use a date between 1900 and 2100.");
            return date;
        } catch (DateTimeParseException ex) {
            throw new InvalidInput("Enter a valid calendar date.");
        }
    }

    public static class InvalidInput extends RuntimeException {
        public InvalidInput(String message) {
            super(message);
        }
    }

    public static class NotFound extends RuntimeException {
        public NotFound() {
            super("The record does not exist or is not available to your account.");
        }
    }
}
