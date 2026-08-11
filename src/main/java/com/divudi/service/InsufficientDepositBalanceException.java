package com.divudi.service;

/**
 * Thrown when a patient-deposit utilization would overdraw the account.
 *
 * It is an unchecked exception so that, when thrown inside a container-managed
 * transaction, the transaction is marked for rollback and the settlement that
 * would have overdrawn the deposit is aborted (no bill, no balance change).
 */
public class InsufficientDepositBalanceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final double available;
    private final double required;

    public InsufficientDepositBalanceException(double available, double required) {
        super("Insufficient patient deposit balance: available " + available + ", required " + required);
        this.available = available;
        this.required = required;
    }

    public double getAvailable() {
        return available;
    }

    public double getRequired() {
        return required;
    }
}
