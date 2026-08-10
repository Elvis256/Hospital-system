/*
 * Open Hospital Management Information System
 */
package com.divudi.core.util;

import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import java.math.BigDecimal;

/**
 * Projects a {@link BillItem}'s legacy line-level {@code double} amounts into its
 * {@link BillItemFinanceDetails} as {@link BigDecimal} (money-precision
 * migration, GitHub issue #12437).
 *
 * <p>This is the item-level counterpart of {@link BillFinanceProjection}, and it
 * differs from it in one important way: a bill item's finance details may
 * <em>already exist and already be populated</em> by the pharmacy costing paths
 * (which write {@code valueAtCostRate}, {@code valueAtRetailRate} and friends).
 * This class therefore <em>updates in place</em> via
 * {@link BillItem#getBillItemFinanceDetails()} — whose get-or-create also wires
 * the back-reference — and touches only the line amounts it owns. Costing fields
 * written elsewhere are left untouched.
 *
 * <p><b>Which fields are written.</b> The legacy {@code double} model records
 * only the <em>combined</em> discount and tax for a line; it does not record the
 * line-level/bill-level split that {@code BillItemFinanceDetails} can express
 * ({@code lineDiscount} + {@code billDiscount} → {@code totalDiscount}). So the
 * combined legacy values map to the {@code total*} fields, and the {@code line*}
 * and {@code bill*} decompositions are deliberately left null — null meaning "not
 * migrated", as distinct from "migrated as zero".
 *
 * <p>Amounts are converted with {@link BigDecimal#valueOf(double)} (the canonical
 * decimal form, unlike {@code new BigDecimal(double)}) and normalised to the
 * money scale via {@link BigDecimalUtil#money(BigDecimal)}. Persistence is the
 * caller's responsibility; {@code BillItem} cascades ALL to its finance details.
 */
public final class BillItemFinanceProjection {

    private BillItemFinanceProjection() {
    }

    /**
     * Populates the bill item's {@link BillItemFinanceDetails} line amounts from
     * its legacy {@code double} values, creating the finance details if the item
     * does not have one yet.
     *
     * @param billItem the priced bill item (may be null)
     * @return the item's finance details with the line amounts written, or an
     *         empty (all-null) finance-details if the bill item is null
     */
    public static BillItemFinanceDetails applyLineTotals(BillItem billItem) {
        if (billItem == null) {
            return new BillItemFinanceDetails();
        }
        BillItemFinanceDetails details = billItem.getBillItemFinanceDetails();

        // getQty() is a nullable Double; the rest are primitives.
        Double qty = billItem.getQty();
        details.setQuantity(money(qty == null ? 0.0 : qty));

        details.setGrossRate(money(billItem.getRate()));
        details.setNetRate(money(billItem.getNetRate()));
        details.setGrossTotal(money(billItem.getGrossValue()));
        details.setNetTotal(money(billItem.getNetValue()));
        details.setTotalDiscount(money(billItem.getDiscount()));
        details.setTotalTax(money(billItem.getVat()));

        return details;
    }

    private static BigDecimal money(double value) {
        return BigDecimalUtil.money(BigDecimal.valueOf(value));
    }
}
