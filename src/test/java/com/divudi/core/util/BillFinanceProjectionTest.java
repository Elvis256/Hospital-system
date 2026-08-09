package com.divudi.core.util;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reconciliation tests for the OPD/bill-level BigDecimal write-path
 * ({@link BillFinanceProjection}). Proves the projected BigDecimal totals match
 * the legacy double totals within tolerance — the pattern later phases roll out.
 */
@DisplayName("BillFinanceProjection bill-level reconciliation")
public class BillFinanceProjectionTest {

    private Bill bill(double net, double gross, double discount, double vat) {
        Bill b = new Bill();
        b.setNetTotal(net);
        b.setTotal(gross);
        b.setDiscount(discount);
        b.setVat(vat);
        return b;
    }

    @Test
    @DisplayName("projected BigDecimal totals reconcile with the legacy double totals")
    void projectedTotalsReconcile() {
        Bill b = bill(1234.5678, 1334.69, 100.1222, 33.33);
        BillFinanceDetails d = BillFinanceProjection.fromBillTotals(b);

        assertTrue(MoneyReconciliation.reconciles(b.getNetTotal(), d.getNetTotal()), "net");
        assertTrue(MoneyReconciliation.reconciles(b.getTotal(), d.getGrossTotal()), "gross");
        assertTrue(MoneyReconciliation.reconciles(b.getDiscount(), d.getBillDiscount()), "discount");
        assertTrue(MoneyReconciliation.reconciles(b.getVat(), d.getBillTaxValue()), "tax");
        assertEquals(4, d.getNetTotal().scale(), "money scale");
    }

    @Test
    @DisplayName("mirror fields agree (net==billNet, gross==billGross, discount==totalDiscount)")
    void mirrorFieldsAgree() {
        BillFinanceDetails d = BillFinanceProjection.fromBillTotals(bill(500.0, 550.0, 50.0, 0.0));
        assertEquals(0, d.getNetTotal().compareTo(d.getBillNetTotal()));
        assertEquals(0, d.getGrossTotal().compareTo(d.getBillGrossTotal()));
        assertEquals(0, d.getBillDiscount().compareTo(d.getTotalDiscount()));
    }

    @Test
    @DisplayName("a null bill yields empty finance details and does not throw")
    void nullBillIsSafe() {
        BillFinanceDetails d = BillFinanceProjection.fromBillTotals(null);
        // getters stay nullable, so unset totals read as null (established contract)
        assertNull(d.getNetTotal());
    }
}
