package ph.edu.lendly;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Component
public class Database {
    private final JdbcTemplate jdbc;

    public Database(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(String sql, Object... values) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement statement =
                            connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    for (int index = 0; index < values.length; index++)
                        statement.setObject(index + 1, values[index]);
                    return statement;
                },
                keys);
        return ((Number) keys.getKeys().get("ID")).longValue();
    }
}
