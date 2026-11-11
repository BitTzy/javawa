package ph.edu.lendly;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/** The desktop frontend's per-account savings tracker. */
record SavingsProgress(String title, BigDecimal target, BigDecimal saved, List<Contribution> recent) {
    private static final int RECENT_LIMIT = 10;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final String STORE = "/ph/edu/lendly/frontend/savings";

    record Contribution(LocalDate date, BigDecimal amount) {}

    boolean hasGoal() {
        return target.signum() > 0;
    }

    int percent() {
        return saved.multiply(BigDecimal.valueOf(100))
                .divide(target, 0, RoundingMode.DOWN)
                .min(BigDecimal.valueOf(100))
                .intValueExact();
    }

    SavingsProgress withGoal(String name, BigDecimal amount) {
        return new SavingsProgress(name, amount, saved, recent);
    }

    SavingsProgress withContribution(LocalDate date, BigDecimal amount) {
        var activity = new ArrayList<Contribution>();
        activity.add(new Contribution(date, amount));
        activity.addAll(recent.stream().limit(RECENT_LIMIT - 1).toList());
        return new SavingsProgress(title, target, saved.add(amount), List.copyOf(activity));
    }

    static SavingsProgress load(String email) {
        var raw = Preferences.userRoot().node(STORE).get(key(email), null);
        if (raw == null) return new SavingsProgress("", ZERO, ZERO, List.of());

        try {
            var fields = raw.split("\\|", -1);
            var name = new String(Base64.getUrlDecoder().decode(fields[0]), StandardCharsets.UTF_8);
            var activity = new ArrayList<Contribution>();
            for (int i = 3; i < fields.length; i++) {
                var entry = fields[i].split(",", -1);
                activity.add(new Contribution(LocalDate.parse(entry[0]), new BigDecimal(entry[1])));
            }
            return new SavingsProgress(name, new BigDecimal(fields[1]), new BigDecimal(fields[2]), List.copyOf(activity));
        } catch (IllegalArgumentException | java.time.DateTimeException | IndexOutOfBoundsException ex) {
            throw new Rules.InvalidInput("Could not read the savings goal stored on this computer.");
        }
    }

    void save(String email) {
        var prefs = Preferences.userRoot().node(STORE);
        var key = key(email);
        var previous = prefs.get(key, null);
        var name = Base64.getUrlEncoder().encodeToString(title.getBytes(StandardCharsets.UTF_8));
        var activity = recent.stream().map(entry -> "|" + entry.date() + "," + entry.amount().toPlainString())
                .collect(Collectors.joining());
        prefs.put(key, name + "|" + target.toPlainString() + "|" + saved.toPlainString() + activity);
        try {
            prefs.flush();
        } catch (BackingStoreException ex) {
            if (previous == null) prefs.remove(key);
            else prefs.put(key, previous);
            throw new Rules.InvalidInput("Could not save your savings progress on this computer.");
        }
    }

    private static String key(String email) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(email.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
