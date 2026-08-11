package com.divudi.core.util;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.Payment;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the migration read path ({@link MoneyRead}) used by the reporting
 * cutover: migrated values are preferred, unmigrated rows fall back to the
 * legacy double, reading never creates a companion entity, and BigDecimal
 * accumulation reconciles with (and is more accurate than) double accumulation.
 */
@DisplayName("MoneyRead migration read path")
public class MoneyReadTest {

    private BillItem item(double net, double discount) {
        BillItem bi = new BillItem();
        bi.setNetValue(net);
        bi.setDiscount(discount);
        return bi;
    }

    @Test
    @DisplayName("an unmigrated bill item falls back to the legacy double")
    void fallsBackToLegacy() {
        BillItem bi = item(1234.56, 10.5);

        assertEquals(0, new BigDecimal("1234.5600").compareTo(MoneyRead.netTotal(bi)), "net");
        assertEquals(0, new BigDecimal("10.5000").compareTo(MoneyRead.discount(bi)), "discount");
        assertFalse(MoneyRead.isMigrated(bi), "not migrated");
    }

    @Test
    @DisplayName("a migrated bill item is served from the BigDecimal field")
    void prefersMigratedValue() {
        BillItem bi = item(1234.56, 10.5);
        // A deliberately different migrated value proves which one is read.
        BillItemFinanceDetails d = bi.getBillItemFinanceDetails();
        d.setNetTotal(new BigDecimal("999.1234"));
        d.setTotalDiscount(new BigDecimal("1.2345"));

        assertEquals(0, new BigDecimal("999.1234").compareTo(MoneyRead.netTotal(bi)), "net");
        assertEquals(0, new BigDecimal("1.2345").compareTo(MoneyRead.discount(bi)), "discount");
        assertTrue(MoneyRead.isMigrated(bi), "migrated");
    }

    @Test
    @DisplayName("a finance-details with null amounts still falls back to legacy")
    void partiallyMigratedFallsBack() {
        BillItem bi = item(500.0, 25.0);
        bi.getBillItemFinanceDetails(); // exists, but every amount is null

        assertEquals(0, new BigDecimal("500.0000").compareTo(MoneyRead.netTotal(bi)), "net");
        assertEquals(0, new BigDecimal("25.0000").compareTo(MoneyRead.discount(bi)), "discount");
        assertFalse(MoneyRead.isMigrated(bi), "null amount is not migrated");
    }

    @Test
    @DisplayName("reading does not create a finance-details entity")
    void readingNeverCreates() {
        BillItem bi = item(100.0, 0.0);
        MoneyRead.netTotal(bi);
        MoneyRead.discount(bi);
        MoneyRead.isMigrated(bi);

        // The lazily-creating getter would have attached one; peek must not.
        assertNull(bi.peekBillItemFinanceDetails(),
                "reading must not attach a companion entity - cascade ALL would persist it");
    }

    @Test
    @DisplayName("a null bill item or bill reads as zero rather than throwing")
    void nullsAreSafe() {
        assertEquals(0, BigDecimal.ZERO.compareTo(MoneyRead.netTotal((BillItem) null)));
        assertEquals(0, BigDecimal.ZERO.compareTo(MoneyRead.discount((BillItem) null)));
        assertEquals(0, BigDecimal.ZERO.compareTo(MoneyRead.netTotal((Bill) null)));
        assertEquals(0, BigDecimal.ZERO.compareTo(MoneyRead.discount((Bill) null)));
        assertEquals(0, BigDecimal.ZERO.compareTo(MoneyRead.grossTotal(null)));
        assertEquals(0, BigDecimal.ZERO.compareTo(MoneyRead.paidValue(null)));
        assertFalse(MoneyRead.isMigrated(null));
    }

    @Test
    @DisplayName("bill gross total and discount prefer the migrated fields")
    void billGrossAndDiscount() {
        Bill b = new Bill();
        b.setTotal(900.0);
        b.setDiscount(50.0);
        assertEquals(0, new BigDecimal("900.0000").compareTo(MoneyRead.grossTotal(b)), "legacy gross");
        assertEquals(0, new BigDecimal("50.0000").compareTo(MoneyRead.discount(b)), "legacy discount");

        b.getBillFinanceDetails().setGrossTotal(new BigDecimal("800.5000"));
        b.getBillFinanceDetails().setBillDiscount(new BigDecimal("12.2500"));
        assertEquals(0, new BigDecimal("800.5000").compareTo(MoneyRead.grossTotal(b)), "migrated gross");
        assertEquals(0, new BigDecimal("12.2500").compareTo(MoneyRead.discount(b)), "migrated discount");
    }

    @Test
    @DisplayName("payment paid value converts at money scale")
    void paymentPaidValue() {
        Payment p = new Payment();
        p.setPaidValue(333.333333);
        assertEquals(0, new BigDecimal("333.3333").compareTo(MoneyRead.paidValue(p)));
        assertEquals(4, MoneyRead.paidValue(p).scale(), "money scale");
    }

    @Test
    @DisplayName("bill-level read prefers the migrated total and falls back otherwise")
    void billLevelRead() {
        Bill b = new Bill();
        b.setNetTotal(750.25);
        assertEquals(0, new BigDecimal("750.2500").compareTo(MoneyRead.netTotal(b)), "legacy");

        b.getBillFinanceDetails().setNetTotal(new BigDecimal("111.1111"));
        assertEquals(0, new BigDecimal("111.1111").compareTo(MoneyRead.netTotal(b)), "migrated");
    }

    @Test
    @DisplayName("BigDecimal accumulation reconciles with the legacy double sum")
    void accumulationReconciles() {
        double[] amounts = {0.1, 0.2, 0.3, 1234.56, 0.7, 99.99, 0.01};
        double legacySum = 0.0;
        BigDecimal migratedSum = BigDecimal.ZERO;
        for (double a : amounts) {
            legacySum += a;
            migratedSum = migratedSum.add(MoneyRead.netTotal(item(a, 0.0)));
        }

        assertTrue(MoneyReconciliation.reconciles(legacySum, migratedSum),
                "legacy " + legacySum + " vs migrated " + migratedSum);
    }

    @Test
    @DisplayName("BigDecimal accumulation avoids the drift double accumulation shows")
    void accumulationIsExact() {
        // 0.1 + 0.2 is the classic case: 0.30000000000000004 in binary floating
        // point, exactly 0.30 in decimal.
        double legacySum = 0.0;
        BigDecimal migratedSum = BigDecimal.ZERO;
        for (double a : new double[]{0.1, 0.2}) {
            legacySum += a;
            migratedSum = migratedSum.add(MoneyRead.netTotal(item(a, 0.0)));
        }

        assertEquals(0, new BigDecimal("0.3000").compareTo(migratedSum), "exact in decimal");
        assertTrue(legacySum != 0.3, "the legacy double sum is not exactly 0.3");
    }
}
