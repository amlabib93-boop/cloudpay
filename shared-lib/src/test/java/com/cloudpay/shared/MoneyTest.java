package com.cloudpay.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyTest {

    @ParameterizedTest
    @CsvSource({
        "10, USD, 10.0000",
        "10.5, USD, 10.5000",
        "0.12345, USD, 0.1234",
        "0.12355, USD, 0.1236",
        "-0.12345, USD, -0.1234",
        "1E+3, EUR, 1000.0000",
        "0.123456785, BTC, 0.12345678",
        "0.123456775, BTC, 0.12345678",
        "1, XAU, 1.00000000"
    })
    void should_normalise_scale_half_even_when_created(String input, Currency currency, String expected) {
        Money money = Money.of(new BigDecimal(input), currency);

        assertThat(money.amount()).isEqualByComparingTo(expected);
        assertThat(money.amount().scale()).isEqualTo(currency.scale());
        assertThat(money.amount().toPlainString()).isEqualTo(expected);
    }

    @Test
    void should_normalise_scale_when_constructed_directly() {
        Money money = new Money(new BigDecimal("1.5"), Currency.ETH);

        assertThat(money.amount().scale()).isEqualTo(Currency.ETH.scale());
    }

    @Test
    void should_create_same_money_when_using_string_factory() {
        assertThat(Money.of("10.50", Currency.USD)).isEqualTo(Money.of(new BigDecimal("10.50"), Currency.USD));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1,000", "", "10 USD"})
    void should_throw_NumberFormatException_when_string_not_decimal(String amount) {
        assertThatThrownBy(() -> Money.of(amount, Currency.USD)).isInstanceOf(NumberFormatException.class);
    }

    @ParameterizedTest
    @EnumSource(Currency.class)
    void should_be_zero_at_currency_scale_when_zero_factory(Currency currency) {
        Money zero = Money.zero(currency);

        assertThat(zero.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(zero.amount().scale()).isEqualTo(currency.scale());
        assertThat(zero.currency()).isEqualTo(currency);
    }

    @Test
    void should_throw_NullPointerException_when_amount_null() {
        assertThatNullPointerException()
                .isThrownBy(() -> Money.of((BigDecimal) null, Currency.USD))
                .withMessageContaining("amount");
        assertThatNullPointerException()
                .isThrownBy(() -> Money.of((String) null, Currency.USD))
                .withMessageContaining("amount");
    }

    @Test
    void should_throw_NullPointerException_when_currency_null() {
        assertThatNullPointerException().isThrownBy(() -> Money.of("1", null)).withMessageContaining("currency");
    }

    @Test
    void should_be_equal_with_same_hashCode_when_scale_differs() {
        Money oneDecimal = Money.of("1.0", Currency.USD);
        Money twoDecimals = Money.of("1.00", Currency.USD);

        assertThat(oneDecimal).isEqualTo(twoDecimals);
        assertThat(twoDecimals).isEqualTo(oneDecimal);
        assertThat(oneDecimal).hasSameHashCodeAs(twoDecimals);
    }

    @Test
    void should_be_equal_with_same_hashCode_when_zero_in_different_forms() {
        Money zero = Money.zero(Currency.BTC);
        Money parsedZero = Money.of("0.000", Currency.BTC);

        assertThat(zero).isEqualTo(parsedZero);
        assertThat(zero).hasSameHashCodeAs(parsedZero);
    }

    @Test
    void should_not_be_equal_when_currency_differs() {
        assertThat(Money.of("1", Currency.USD)).isNotEqualTo(Money.of("1", Currency.EUR));
    }

    @Test
    void should_not_be_equal_when_amount_differs() {
        assertThat(Money.of("1.0001", Currency.USD)).isNotEqualTo(Money.of("1", Currency.USD));
    }

    @Test
    void should_be_equal_to_itself_and_not_to_null_or_other_types() {
        Money money = Money.of("1", Currency.USD);

        assertThat(money.equals(money)).isTrue();
        assertThat(money.equals(null)).isFalse();
        assertThat(money.equals(new BigDecimal("1"))).isFalse();
    }

    @Test
    void should_print_plain_amount_and_code_when_toString() {
        assertThat(Money.of("1E+3", Currency.USD)).hasToString("1000.0000 USD");
        assertThat(Money.of("-0.5", Currency.BTC)).hasToString("-0.50000000 BTC");
    }

    @Test
    void should_add_amounts_when_same_currency() {
        Money sum = Money.of("10.25", Currency.USD).add(Money.of("0.0001", Currency.USD));

        assertThat(sum).isEqualTo(Money.of("10.2501", Currency.USD));
        assertThat(sum.amount().scale()).isEqualTo(Currency.USD.scale());
    }

    @Test
    void should_subtract_to_negative_when_subtrahend_larger() {
        Money difference = Money.of("1", Currency.BTC).subtract(Money.of("1.00000001", Currency.BTC));

        assertThat(difference).isEqualTo(Money.of("-0.00000001", Currency.BTC));
        assertThat(difference.isNegative()).isTrue();
    }

    @Test
    void should_return_zero_when_subtracting_equal_amount() {
        Money money = Money.of("42.4242", Currency.AED);

        assertThat(money.subtract(money).isZero()).isTrue();
    }

    @Test
    void should_not_change_operands_when_adding() {
        Money left = Money.of("1", Currency.EUR);
        Money right = Money.of("2", Currency.EUR);

        left.add(right);

        assertThat(left).isEqualTo(Money.of("1", Currency.EUR));
        assertThat(right).isEqualTo(Money.of("2", Currency.EUR));
    }

    @Test
    void should_flip_sign_when_negate() {
        assertThat(Money.of("5", Currency.GBP).negate()).isEqualTo(Money.of("-5", Currency.GBP));
        assertThat(Money.zero(Currency.GBP).negate().isZero()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"0, true, false, false", "0.0001, false, true, false", "-0.0001, false, false, true"})
    void should_report_sign_when_checked(String amount, boolean zero, boolean positive, boolean negative) {
        Money money = Money.of(amount, Currency.USD);

        assertThat(money.isZero()).isEqualTo(zero);
        assertThat(money.isPositive()).isEqualTo(positive);
        assertThat(money.isNegative()).isEqualTo(negative);
    }

    @Test
    void should_order_by_amount_when_same_currency() {
        Money small = Money.of("9.9999", Currency.USD);
        Money large = Money.of("10", Currency.USD);

        assertThat(small).isLessThan(large);
        assertThat(small.isLessThan(large)).isTrue();
        assertThat(large.isGreaterThan(small)).isTrue();
        assertThat(small.isGreaterThan(large)).isFalse();
        assertThat(large.isLessThan(small)).isFalse();
    }

    @Test
    void should_compare_as_zero_exactly_when_equal() {
        Money oneDecimal = Money.of("1.0", Currency.USD);
        Money twoDecimals = Money.of("1.00", Currency.USD);

        assertThat(oneDecimal.compareTo(twoDecimals)).isZero();
        assertThat(oneDecimal).isEqualTo(twoDecimals);
        assertThat(oneDecimal.isGreaterThan(twoDecimals)).isFalse();
        assertThat(oneDecimal.isLessThan(twoDecimals)).isFalse();
    }

    @Test
    void should_throw_CurrencyMismatchException_when_currencies_differ() {
        Money usd = Money.of("1", Currency.USD);
        Money eur = Money.of("1", Currency.EUR);

        assertThatThrownBy(() -> usd.add(eur))
                .isInstanceOf(CurrencyMismatchException.class)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currency mismatch: expected USD but got EUR")
                .satisfies(thrown -> {
                    CurrencyMismatchException mismatch = (CurrencyMismatchException) thrown;
                    assertThat(mismatch.expected()).isEqualTo(Currency.USD);
                    assertThat(mismatch.actual()).isEqualTo(Currency.EUR);
                });
        assertThatThrownBy(() -> usd.subtract(eur)).isInstanceOf(CurrencyMismatchException.class);
        assertThatThrownBy(() -> usd.compareTo(eur)).isInstanceOf(CurrencyMismatchException.class);
        assertThatThrownBy(() -> usd.isGreaterThan(eur)).isInstanceOf(CurrencyMismatchException.class);
        assertThatThrownBy(() -> usd.isLessThan(eur)).isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void should_throw_CurrencyMismatchException_when_sorting_mixed_currencies() {
        List<Money> mixed = new ArrayList<>(List.of(Money.of("2", Currency.USD), Money.of("1", Currency.BTC)));

        assertThatThrownBy(() -> Collections.sort(mixed)).isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    void should_throw_NullPointerException_when_other_null() {
        Money money = Money.of("1", Currency.USD);

        assertThatNullPointerException().isThrownBy(() -> money.add(null)).withMessageContaining("other");
        assertThatNullPointerException().isThrownBy(() -> money.compareTo(null));
    }
}
