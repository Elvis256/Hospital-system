package com.divudi.core.util;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the monetary scale and division helpers added to
 * {@link BigDecimalUtil} for the money-precision migration.
 */
@DisplayName("BigDecimalUtil money scale & division")
public class BigDecimalUtilMoneyTest {

    @Test
    @DisplayName("money() normalises to scale 4 and rounds HALF_UP")
    void moneyNormalises() {
        assertEquals(0, new BigDecimal("1.5000").compareTo(BigDecimalUtil.money(new BigDecimal("1.5"))));
        assertEquals(4, BigDecimalUtil.money(new BigDecimal("1.5")).scale());
        assertEquals(0, new BigDecimal("1.2346").compareTo(BigDecimalUtil.money(new BigDecimal("1.23455"))));
        assertEquals(0, new BigDecimal("1.2345").compareTo(BigDecimalUtil.money(new BigDecimal("1.23454"))));
    }

    @Test
    @DisplayName("money(null) is zero at the money scale")
    void moneyNull() {
        assertEquals(0, BigDecimal.ZERO.compareTo(BigDecimalUtil.money(null)));
        assertEquals(4, BigDecimalUtil.money(null).scale());
    }

    @Test
    @DisplayName("divide() rounds a non-terminating quotient HALF_UP to 4 decimals")
    void divideRounds() {
        BigDecimal r = BigDecimalUtil.divide(new BigDecimal("10"), new BigDecimal("3"));
        assertEquals(0, new BigDecimal("3.3333").compareTo(r));
        assertEquals(4, r.scale());
    }

    @Test
    @DisplayName("divide() by null or zero yields zero and never throws")
    void divideByZeroIsSafe() {
        assertEquals(0, BigDecimal.ZERO.compareTo(BigDecimalUtil.divide(new BigDecimal("10"), null)));
        assertEquals(0, BigDecimal.ZERO.compareTo(BigDecimalUtil.divide(new BigDecimal("10"), BigDecimal.ZERO)));
        assertEquals(0, BigDecimal.ZERO.compareTo(BigDecimalUtil.divide(null, new BigDecimal("5"))));
    }
}
