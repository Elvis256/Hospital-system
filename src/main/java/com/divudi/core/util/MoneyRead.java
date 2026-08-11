/*
 * Open Hospital Management Information System
 */
package com.divudi.core.util;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.Payment;
import java.math.BigDecimal;

/**
 * The read side of the money-precision migration (GitHub issue #12437): returns
 * an amount as {@link BigDecimal}, preferring the migrated
 * {@code *FinanceDetails} value and falling back to the legacy {@code double}
 * when the amount has not been migrated for that row.
 *
 * <p><b>Why a fallback rather than a straight switch.</b> Only some paths write
 * BigDecimal so far — cashier income bills at bill level, OPD at line level — and
 * the backfill migrations only cover rows that existed when they ran. A report
 * that read the BigDecimal fields alone would silently report zero for every
 * pharmacy, inward, lab or channelling row written since. Falling back keeps
 * reports correct during coexistence; once a module's write path lands and its
 * backfill has run, the fallback simply stops being reached.
 *
 * <p><b>Reading never writes.</b> These methods use
 * {@link Bill#peekBillFinanceDetails()} and
 * {@link BillItem#peekBillItemFinanceDetails()}, never the lazily-creating
 * getters: on a managed entity those attach a new companion that
 * {@code cascade = ALL} would persist on flush, so a report would write empty
 * rows just by reading.
 *
 * <p>All amounts come back normalised to the money scale via
 * {@link BigDecimalUtil#money(BigDecimal)}.
 */
public final class MoneyRead {

    private MoneyRead() {
    }

    /**
     * A bill item's net total — migrated value if present, else the legacy
     * {@code netValue}.
     *
     * @param billItem the bill item (may be null)
     * @return the net total at money scale; zero for a null bill item
     */
    public static BigDecimal netTotal(BillItem billItem) {
        if (billItem == null) {
            return zero();
        }
        BillItemFinanceDetails details = billItem.peekBillItemFinanceDetails();
        if (details != null && details.getNetTotal() != null) {
            return BigDecimalUtil.money(details.getNetTotal());
        }
        return money(billItem.getNetValue());
    }

    /**
     * A bill item's discount — migrated value if present, else the legacy
     * {@code discount}.
     *
     * @param billItem the bill item (may be null)
     * @return the discount at money scale; zero for a null bill item
     */
    public static BigDecimal discount(BillItem billItem) {
        if (billItem == null) {
            return zero();
        }
        BillItemFinanceDetails details = billItem.peekBillItemFinanceDetails();
        if (details != null && details.getTotalDiscount() != null) {
            return BigDecimalUtil.money(details.getTotalDiscount());
        }
        return money(billItem.getDiscount());
    }

    /**
     * A bill's net total — migrated value if present, else the legacy
     * {@code netTotal}.
     *
     * @param bill the bill (may be null)
     * @return the net total at money scale; zero for a null bill
     */
    public static BigDecimal netTotal(Bill bill) {
        if (bill == null) {
            return zero();
        }
        BillFinanceDetails details = bill.peekBillFinanceDetails();
        if (details != null && details.getNetTotal() != null) {
            return BigDecimalUtil.money(details.getNetTotal());
        }
        return money(bill.getNetTotal());
    }

    /**
     * A bill's gross total — migrated value if present, else the legacy
     * {@code total}.
     *
     * @param bill the bill (may be null)
     * @return the gross total at money scale; zero for a null bill
     */
    public static BigDecimal grossTotal(Bill bill) {
        if (bill == null) {
            return zero();
        }
        BillFinanceDetails details = bill.peekBillFinanceDetails();
        if (details != null && details.getGrossTotal() != null) {
            return BigDecimalUtil.money(details.getGrossTotal());
        }
        return money(bill.getTotal());
    }

    /**
     * A bill's discount — migrated value if present, else the legacy
     * {@code discount}.
     *
     * @param bill the bill (may be null)
     * @return the discount at money scale; zero for a null bill
     */
    public static BigDecimal discount(Bill bill) {
        if (bill == null) {
            return zero();
        }
        BillFinanceDetails details = bill.peekBillFinanceDetails();
        if (details != null && details.getBillDiscount() != null) {
            return BigDecimalUtil.money(details.getBillDiscount());
        }
        return money(bill.getDiscount());
    }

    /**
     * A payment's paid value as {@link BigDecimal}.
     *
     * <p><b>This is a conversion, not yet a migrated read.</b> {@code Payment}
     * has no companion {@code *FinanceDetails} entity, so there is nothing to
     * prefer over the legacy {@code double} — the only gain today is that
     * callers can accumulate in {@code BigDecimal} instead of compounding
     * floating-point error. It lives here so that when Payment does gain a
     * migrated amount, every report picks it up from one place.
     *
     * @param payment the payment (may be null)
     * @return the paid value at money scale; zero for a null payment
     */
    public static BigDecimal paidValue(Payment payment) {
        if (payment == null) {
            return zero();
        }
        return money(payment.getPaidValue());
    }

    /**
     * Whether this bill item's net total is being served from the migrated
     * BigDecimal field rather than the legacy double. Useful for reconciliation
     * reporting and for telling "migrated as zero" from "not migrated".
     *
     * @param billItem the bill item (may be null)
     * @return true if the migrated value is present and in use
     */
    public static boolean isMigrated(BillItem billItem) {
        if (billItem == null) {
            return false;
        }
        BillItemFinanceDetails details = billItem.peekBillItemFinanceDetails();
        return details != null && details.getNetTotal() != null;
    }

    private static BigDecimal money(double value) {
        return BigDecimalUtil.money(BigDecimal.valueOf(value));
    }

    private static BigDecimal zero() {
        return BigDecimalUtil.money(BigDecimal.ZERO);
    }
}
