/*
 * Open Hospital Management Information System
 */
package com.divudi.core.util;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import java.math.BigDecimal;

/**
 * Projects a {@link Bill}'s legacy bill-level {@code double} totals into a
 * {@link BillFinanceDetails} carrying the same amounts as {@link BigDecimal}
 * (money-precision migration, GitHub issue #12437).
 *
 * <p>This is the bill-level write-path for non-pharmacy bills (e.g. OPD): once a
 * settlement has computed the legacy double totals, this produces the parallel
 * BigDecimal representation that reporting can migrate to. Item-level pharmacy
 * costing is handled separately by {@code PharmacyCostingService}.
 *
 * <p>Amounts are converted with {@link BigDecimal#valueOf(double)} (the
 * canonical decimal form, unlike {@code new BigDecimal(double)}) and normalised
 * to the money scale via {@link BigDecimalUtil#money(BigDecimal)}. The result is
 * a transient entity; persistence is the caller's responsibility.
 */
public final class BillFinanceProjection {

    private BillFinanceProjection() {
    }

    /**
     * Builds a {@link BillFinanceDetails} from a bill's computed bill-level
     * totals (net, gross, discount, tax). A null bill yields an empty (all-null)
     * finance-details rather than throwing.
     *
     * @param bill the settled bill (may be null)
     * @return a BillFinanceDetails whose BigDecimal totals mirror the bill's doubles
     */
    public static BillFinanceDetails fromBillTotals(Bill bill) {
        BillFinanceDetails details = new BillFinanceDetails();
        if (bill == null) {
            return details;
        }
        BigDecimal net = BigDecimalUtil.money(BigDecimal.valueOf(bill.getNetTotal()));
        BigDecimal gross = BigDecimalUtil.money(BigDecimal.valueOf(bill.getTotal()));
        BigDecimal discount = BigDecimalUtil.money(BigDecimal.valueOf(bill.getDiscount()));
        BigDecimal tax = BigDecimalUtil.money(BigDecimal.valueOf(bill.getVat()));

        details.setNetTotal(net);
        details.setBillNetTotal(net);
        details.setGrossTotal(gross);
        details.setBillGrossTotal(gross);
        details.setBillDiscount(discount);
        details.setTotalDiscount(discount);
        details.setBillTaxValue(tax);
        return details;
    }
}
