package com.cloudpay.shared;

/**
 * Thrown when an operation combines or compares {@link Money} in two different currencies. CloudPay never converts
 * implicitly; FX is always an explicit step with a quote.
 */
public class CurrencyMismatchException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final Currency expected;
    private final Currency actual;

    public CurrencyMismatchException(Currency expected, Currency actual) {
        super("Currency mismatch: expected " + expected + " but got " + actual);
        this.expected = expected;
        this.actual = actual;
    }

    public Currency expected() {
        return expected;
    }

    public Currency actual() {
        return actual;
    }
}
