package com.cloudpay.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
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
}
