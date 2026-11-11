package ph.edu.lendly;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class InputValidationTest {
    @Test
    void amountsUsePlainBoundedDecimals() {
        assertEquals(new BigDecimal("100.50"), Rules.money("100.50"));
        assertEquals(new BigDecimal("0.01"), Rules.money("0.01"));
        for (String value :
                new String[] {"1e2", "1e999999999", "0", "-1", "NaN", "1.001", "1000000000000"}) {
            assertThrows(Rules.InvalidInput.class, () -> Rules.money(value), value);
        }
    }

    @Test
    void datesAndEmailAreValidatedAndNormalised() {
        assertEquals("maya@example.test", Rules.email(" MAYA@Example.Test "));
        assertThrows(Rules.InvalidInput.class, () -> Rules.email("not-an-email"));
        assertThrows(Rules.InvalidInput.class, () -> Rules.date("2026-02-30"));
        assertThrows(Rules.InvalidInput.class, () -> Rules.currency("AAA"));
    }
}
