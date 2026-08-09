/*
 * Open Hospital Management Information System
 */
package com.divudi.core.util;

import java.math.BigDecimal;

/**
 * Helpers for reconciling legacy primitive-{@code double} monetary values
 * against their {@link BigDecimal} counterparts during the money-precision
 * migration (GitHub issue #12437).
 *
 * <p>While the migration is in progress every amount lives in two places — a
 * legacy {@code double} field and a migrated {@code BigDecimal} field — and the
 * two must never diverge. These methods provide the single, consistent check
 * used by unit tests today and by a scheduled reconciliation job later, so a
 * divergence is caught the moment it appears rather than in a patient invoice.
 */
public final class MoneyReconciliation {

    /** Default tolerance for a legacy-vs-BigDecimal comparison: one cent. */
    public static final double DEFAULT_TOLERANCE = 0.01;

    private MoneyReconciliation() {
    }

    /**
     * Absolute difference between a legacy {@code double} amount and its
     * migrated {@link BigDecimal} counterpart. A null BigDecimal is treated as
     * zero (consistent with {@link BigDecimalUtil#valueOrZero(BigDecimal)}).
     *
     * @param legacy   the legacy double amount
     * @param migrated the migrated BigDecimal amount (may be null)
     * @return the absolute difference between the two
     */
    public static double difference(double legacy, BigDecimal migrated) {
        double migratedValue = migrated == null ? 0.0 : migrated.doubleValue();
        return Math.abs(legacy - migratedValue);
    }

    /**
     * Whether the legacy and migrated amounts agree within {@code tolerance}.
     *
     * @param legacy    the legacy double amount
     * @param migrated  the migrated BigDecimal amount (may be null)
     * @param tolerance the maximum allowed absolute difference
     * @return true if the two amounts reconcile within the tolerance
     */
    public static boolean reconciles(double legacy, BigDecimal migrated, double tolerance) {
        return difference(legacy, migrated) <= tolerance;
    }

    /**
     * Whether the amounts agree within {@link #DEFAULT_TOLERANCE}.
     *
     * @param legacy   the legacy double amount
     * @param migrated the migrated BigDecimal amount (may be null)
     * @return true if the two amounts reconcile within one cent
     */
    public static boolean reconciles(double legacy, BigDecimal migrated) {
        return reconciles(legacy, migrated, DEFAULT_TOLERANCE);
    }
}
