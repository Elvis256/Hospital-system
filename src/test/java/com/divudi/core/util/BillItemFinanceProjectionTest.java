package com.divudi.core.util;

import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reconciliation tests for the item-level BigDecimal write-path
 * ({@link BillItemFinanceProjection}) — the counterpart of
 * {@link BillFinanceProjectionTest} at line level.
 */
@DisplayName("BillItemFinanceProjection line-level reconciliation")
public class BillItemFinanceProjectionTest {

    private BillItem billItem(double qty, double rate, double netRate,
            double gross, double net, double discount, double vat) {
        BillItem bi = new BillItem();
        bi.setQty(qty);
        bi.setRate(rate);
        bi.setNetRate(netRate);
        bi.setGrossValue(gross);
        bi.setNetValue(net);
        bi.setDiscount(discount);
        bi.setVat(vat);
        return bi;
    }

    @Test
    @DisplayName("projected line amounts reconcile with the legacy double amounts")
    void projectedLineAmountsReconcile() {
        BillItem bi = billItem(3.0, 450.1234, 420.5678, 1350.37, 1261.7034, 88.6666, 12.55);
        BillItemFinanceDetails d = BillItemFinanceProjection.applyLineTotals(bi);

        assertTrue(MoneyReconciliation.reconciles(bi.getQty(), d.getQuantity()), "quantity");
        assertTrue(MoneyReconciliation.reconciles(bi.getRate(), d.getGrossRate()), "gross rate");
        assertTrue(MoneyReconciliation.reconciles(bi.getNetRate(), d.getNetRate()), "net rate");
        assertTrue(MoneyReconciliation.reconciles(bi.getGrossValue(), d.getGrossTotal()), "gross total");
        assertTrue(MoneyReconciliation.reconciles(bi.getNetValue(), d.getNetTotal()), "net total");
        assertTrue(MoneyReconciliation.reconciles(bi.getDiscount(), d.getTotalDiscount()), "discount");
        assertTrue(MoneyReconciliation.reconciles(bi.getVat(), d.getTotalTax()), "tax");
        assertEquals(4, d.getNetTotal().scale(), "money scale");
    }

    @Test
    @DisplayName("the line/bill discount and tax split stays null - the legacy model has no split")
    void unmigratedSplitStaysNull() {
        BillItemFinanceDetails d = BillItemFinanceProjection.applyLineTotals(
                billItem(1.0, 100.0, 90.0, 100.0, 90.0, 10.0, 5.0));

        assertNull(d.getLineDiscount(), "lineDiscount");
        assertNull(d.getBillDiscount(), "billDiscount");
        assertNull(d.getLineTax(), "lineTax");
        assertNull(d.getBillTax(), "billTax");
    }

    @Test
    @DisplayName("existing pharmacy costing values survive the projection")
    void costingValuesArePreserved() {
        BillItem bi = billItem(2.0, 200.0, 180.0, 400.0, 360.0, 40.0, 0.0);
        BillItemFinanceDetails existing = bi.getBillItemFinanceDetails();
        existing.setValueAtCostRate(new BigDecimal("123.4500"));
        existing.setValueAtRetailRate(new BigDecimal("456.7800"));

        BillItemFinanceDetails d = BillItemFinanceProjection.applyLineTotals(bi);

        assertSame(existing, d, "must update in place, not replace");
        assertEquals(0, new BigDecimal("123.4500").compareTo(d.getValueAtCostRate()), "cost rate value");
        assertEquals(0, new BigDecimal("456.7800").compareTo(d.getValueAtRetailRate()), "retail rate value");
        assertEquals(0, new BigDecimal("360.0000").compareTo(d.getNetTotal()), "net total still written");
    }

    @Test
    @DisplayName("a null quantity is treated as zero rather than throwing")
    void nullQuantityIsSafe() {
        BillItem bi = billItem(0.0, 100.0, 100.0, 100.0, 100.0, 0.0, 0.0);
        bi.setQty(null);

        BillItemFinanceDetails d = BillItemFinanceProjection.applyLineTotals(bi);

        assertNotNull(d.getQuantity());
        assertEquals(0, BigDecimal.ZERO.compareTo(d.getQuantity()), "null qty reads as zero");
    }

    @Test
    @DisplayName("a null bill item yields empty finance details and does not throw")
    void nullBillItemIsSafe() {
        BillItemFinanceDetails d = BillItemFinanceProjection.applyLineTotals(null);
        assertNull(d.getNetTotal());
    }
}
