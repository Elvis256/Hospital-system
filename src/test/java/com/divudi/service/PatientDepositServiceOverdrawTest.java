package com.divudi.service;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.entity.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for the patient-deposit overdraw guard.
 *
 * The guard in {@link PatientDepositService#handleOPDBill} and
 * {@link PatientDepositService#handleOutPayment} rejects a utilization that
 * would take the deposit balance negative, throwing
 * {@link InsufficientDepositBalanceException} before any persistence happens.
 * Because the guard runs before the injected facades are touched, it can be
 * exercised on a plain instance without a container.
 */
@DisplayName("PatientDepositService overdraw guard")
public class PatientDepositServiceOverdrawTest {

    private final PatientDepositService service = new PatientDepositService();

    private Bill bill(double netTotal) {
        Bill b = new Bill();
        b.setNetTotal(netTotal);
        return b;
    }

    private Payment payment(double paidValue) {
        Payment p = new Payment();
        p.setPaidValue(paidValue);
        return p;
    }

    private PatientDeposit deposit(Double balance) {
        PatientDeposit pd = new PatientDeposit();
        pd.setBalance(balance);
        return pd;
    }

    @Test
    @DisplayName("handleOPDBill blocks an OPD bill that exceeds the deposit balance")
    void handleOPDBill_blocksWhenInsufficient() {
        InsufficientDepositBalanceException ex = assertThrows(
                InsufficientDepositBalanceException.class,
                () -> service.handleOPDBill(bill(500.0), deposit(0.0)));
        assertEquals(0.0, ex.getAvailable(), 0.0001);
        assertEquals(500.0, ex.getRequired(), 0.0001);
    }

    @Test
    @DisplayName("handleOPDBill treats a null balance as zero and blocks")
    void handleOPDBill_blocksWhenNullBalance() {
        assertThrows(InsufficientDepositBalanceException.class,
                () -> service.handleOPDBill(bill(100.0), deposit(null)));
    }

    @Test
    @DisplayName("handleOutPayment blocks a payment that exceeds the deposit balance")
    void handleOutPayment_blocksWhenInsufficient() {
        InsufficientDepositBalanceException ex = assertThrows(
                InsufficientDepositBalanceException.class,
                () -> service.handleOutPayment(payment(200.0), deposit(50.0)));
        assertEquals(50.0, ex.getAvailable(), 0.0001);
        assertEquals(200.0, ex.getRequired(), 0.0001);
    }

    @Test
    @DisplayName("the guard does not fire when the balance is sufficient")
    void handleOPDBill_allowsWhenSufficient() {
        // With a sufficient balance the guard must pass. The method then reaches
        // its (unmocked) facade calls and fails with a different error, so the
        // only assertion here is that the overdraw exception is NOT thrown.
        try {
            service.handleOPDBill(bill(500.0), deposit(1000.0));
        } catch (InsufficientDepositBalanceException e) {
            fail("Overdraw guard fired even though the balance was sufficient");
        } catch (Exception expected) {
            // e.g. NullPointerException from the null facade: the guard passed,
            // which is what this test asserts.
        }
    }
}
