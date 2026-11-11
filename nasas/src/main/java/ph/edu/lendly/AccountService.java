package ph.edu.lendly;

import static ph.edu.lendly.Models.*;
import static ph.edu.lendly.Rules.*;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class AccountService {
    private final JdbcTemplate jdbc;
    private final Database db;
    private final PasswordEncoder passwords;

    public AccountService(JdbcTemplate jdbc, Database db, PasswordEncoder passwords) {
        this.jdbc = jdbc;
        this.db = db;
        this.passwords = passwords;
    }

    public Account register(String name, String email, String password, String confirmation) {
        name = required(name, 80, "Name");
        email = Rules.email(email);
        require(
                password != null
                        && password.length() >= 12
                        && password.getBytes(StandardCharsets.UTF_8).length <= 72,
                "Use a password of at least 12 characters and no more than 72 UTF-8 bytes.");
        require(password.equals(confirmation), "The passwords do not match.");
        try {
            long id =
                    db.insert(
                            "INSERT INTO app_user(name,email,password_hash) VALUES(?,?,?)",
                            name,
                            email,
                            passwords.encode(password));
            return new Account(id, name, email);
        } catch (DuplicateKeyException ex) {
            throw new InvalidInput("An account already uses this email. Sign in instead.");
        }
    }

    public Account byEmail(String email) {
        var found =
                jdbc.query(
                        "SELECT id,name,email FROM app_user WHERE email=?",
                        (rs, n) -> new Account(rs.getLong(1), rs.getString(2), rs.getString(3)),
                        Rules.email(email));
        if (found.isEmpty()) throw new NotFound();
        return found.getFirst();
    }

    public Account signIn(String email, String password) {
        String error = "Incorrect email or password.";
        Account account;
        try {
            account = byEmail(email);
        } catch (NotFound | InvalidInput ex) {
            throw new InvalidInput(error);
        }
        if (password == null
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || !passwords.matches(password, passwordHash(account.email()))) {
            throw new InvalidInput(error);
        }
        return account;
    }

    public Account byId(long id) {
        var found =
                jdbc.query(
                        "SELECT id,name,email FROM app_user WHERE id=?",
                        (rs, n) -> new Account(rs.getLong(1), rs.getString(2), rs.getString(3)),
                        id);
        if (found.isEmpty()) throw new NotFound();
        return found.getFirst();
    }

    public String passwordHash(String email) {
        return jdbc.queryForObject(
                "SELECT password_hash FROM app_user WHERE email=?",
                String.class,
                Rules.email(email));
    }
}
