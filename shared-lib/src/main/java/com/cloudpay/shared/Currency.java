package com.cloudpay.shared;

import java.util.Arrays;
import java.util.Objects;

/**
 * Currencies and assets CloudPay can hold a {@link Money} amount in.
 *
 * <p>The set is closed on purpose: supporting a new currency is a business decision that also touches
 * corridors, providers and limits, so it goes through a shared-lib release (ADR 0001).
 *
 * <p>The scale of every amount is owned here, through {@link Kind}, so all services agree on it.
 */
public enum Currency {
    USD(Kind.FIAT),
    EUR(Kind.FIAT),
    GBP(Kind.FIAT),
    AED(Kind.FIAT),
    BTC(Kind.CRYPTO),
    ETH(Kind.CRYPTO),
    USDC(Kind.CRYPTO),
    SOL(Kind.CRYPTO),
    /** Gold, one troy ounce per unit (ISO 4217 meaning). Grams are a display concern. */
    XAU(Kind.COMMODITY);

    private final Kind kind;

    Currency(Kind kind) {
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    /** Number of decimal places every {@link Money} amount in this currency is stored at. */
    public int scale() {
        return kind.scale();
    }

    /**
     * Looks up a currency by its exact code (for example {@code "USD"}).
     *
     * @throws IllegalArgumentException if the code is not a supported currency
     */
    public static Currency fromCode(String code) {
        Objects.requireNonNull(code, "currency code must not be null");
        return Arrays.stream(values())
                .filter(currency -> currency.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported currency code: " + code));
    }

    /** What kind of asset a currency is. Decides the scale. */
    public enum Kind {
        FIAT(Kind.FIAT_SCALE),
        CRYPTO(Kind.CRYPTO_SCALE),
        COMMODITY(Kind.COMMODITY_SCALE);

        private static final int FIAT_SCALE = 4;
        private static final int CRYPTO_SCALE = 8;
        private static final int COMMODITY_SCALE = 8;

        private final int scale;

        Kind(int scale) {
            this.scale = scale;
        }

        public int scale() {
            return scale;
        }
    }
}
