package com.cloudpay.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class CurrencyTest {

    private static final int FIAT_SCALE = 4;
    private static final int CRYPTO_SCALE = 8;

    @ParameterizedTest
    @EnumSource(
            value = Currency.class,
            names = {"USD", "EUR", "GBP", "AED"})
    void should_be_fiat_with_scale_4_when_fiat_currency(Currency currency) {
        assertThat(currency.kind()).isEqualTo(Currency.Kind.FIAT);
        assertThat(currency.scale()).isEqualTo(FIAT_SCALE);
    }

    @ParameterizedTest
    @EnumSource(
            value = Currency.class,
            names = {"BTC", "ETH", "USDC", "SOL"})
    void should_be_crypto_with_scale_8_when_crypto_currency(Currency currency) {
        assertThat(currency.kind()).isEqualTo(Currency.Kind.CRYPTO);
        assertThat(currency.scale()).isEqualTo(CRYPTO_SCALE);
    }

    @Test
    void should_be_commodity_with_scale_8_when_gold() {
        assertThat(Currency.XAU.kind()).isEqualTo(Currency.Kind.COMMODITY);
        assertThat(Currency.XAU.scale()).isEqualTo(CRYPTO_SCALE);
    }

    @ParameterizedTest
    @EnumSource(Currency.class)
    void should_return_currency_when_code_matches_exactly(Currency currency) {
        assertThat(Currency.fromCode(currency.name())).isSameAs(currency);
    }

    @ParameterizedTest
    @ValueSource(strings = {"usd", " USD", "USD ", "JPY", "XYZ", ""})
    void should_throw_IllegalArgumentException_when_code_unsupported(String code) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Currency.fromCode(code))
                .withMessageContaining("Unsupported currency code");
    }

    @Test
    void should_throw_NullPointerException_when_code_null() {
        assertThatNullPointerException().isThrownBy(() -> Currency.fromCode(null));
    }
}
