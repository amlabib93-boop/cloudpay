package com.cloudpay.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An amount in a {@link Currency}.
 *
 * <p>The amount is always stored at {@code currency.scale()} decimal places, rounded {@link RoundingMode#HALF_EVEN}.
 * The incoming scale is never trusted. Negative amounts are allowed; non-negative balances are the owning service's
 * rule.
 *
 * <p>{@code equals} and {@code hashCode} compare by numeric value and currency, not by {@link BigDecimal} scale.
 */
public record Money(BigDecimal amount, Currency currency) {

    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        amount = amount.setScale(currency.scale(), ROUNDING);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    /**
     * Creates money from a decimal string such as {@code "10.50"}. Use this for literals instead of any floating-point
     * value.
     *
     * @throws NumberFormatException if the string is not a valid decimal
     */
    public static Money of(String amount, Currency currency) {
        Objects.requireNonNull(amount, "amount must not be null");
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Money that && currency == that.currency && amount.compareTo(that.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.name();
    }
}
