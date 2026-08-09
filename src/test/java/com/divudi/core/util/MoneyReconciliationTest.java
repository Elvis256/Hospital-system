package com.divudi.core.util;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the legacy-double vs BigDecimal reconciliation harness used
 * throughout the money-precision migration.
 */
@DisplayName("MoneyReconciliation")
public class MoneyReconciliationTest {

    @Test
    @DisplayName("equal amounts reconcile with zero difference")
    void equalAmountsReconcile() {
        assertTrue(MoneyReconciliation.reconciles(100.10, new BigDecimal("100.10")));
        assertEquals(0.0, MoneyReconciliation.difference(100.10, new BigDecimal("100.10")), 1e-6);
    }

    @Test
    @DisplayName("rounding noise within one cent still reconciles")
    void roundingNoiseReconciles() {
        assertTrue(MoneyReconciliation.reconciles(100.1000004, new BigDecimal("100.10")));
    }

    @Test
    @DisplayName("a divergence beyond tolerance is flagged")
    void divergenceIsFlagged() {
        assertFalse(MoneyReconciliation.reconciles(100.10, new BigDecimal("100.20")));
        assertEquals(0.10, MoneyReconciliation.difference(100.10, new BigDecimal("100.20")), 1e-6);
    }

    @Test
    @DisplayName("a null migrated value is treated as zero")
    void nullMigratedIsZero() {
        assertEquals(50.0, MoneyReconciliation.difference(50.0, null), 1e-6);
        assertTrue(MoneyReconciliation.reconciles(0.0, null));
    }
}
