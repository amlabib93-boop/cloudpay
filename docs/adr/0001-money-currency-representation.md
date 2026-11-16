# 0001. Money currency representation

- Status: accepted
- Date: 2026-11-15
- Gate: G1 (roadmap #1, conflict #6)

## Context

`Money` needs a currency that covers fiat, crypto (BTC, ETH, USDC, SOL) and digital gold (XAU).
`java.util.Currency` only covers ISO 4217 and has no notion of crypto. Scale must be fixed per currency
(4 for fiat, 8 for crypto) and be the same in every service.

## Decision

A closed enum, `com.cloudpay.shared.Currency`. Each constant has a `Kind` (FIAT, CRYPTO, COMMODITY), and
the kind sets the scale. Initial set: USD, EUR, GBP, AED, BTC, ETH, USDC, SOL, XAU.

XAU is one troy ounce (its ISO 4217 meaning), at scale 8. Grams are a display conversion in
`crypto-service`.

## Options considered

1. **Closed enum (chosen).** Valid codes are checked at compile time, scale lives in one place, and
   Jackson and JPA (`EnumType.STRING`) work without custom code.
2. **Sealed interface** (`Fiat` wrapping `java.util.Currency`, plus enums for crypto and commodities).
   Covers every ISO currency, but needs polymorphic JSON and a JPA converter in every service.
   Flexibility we don't need.
3. **Record plus registry.** An open set, but invalid codes only fail at runtime, and a config-backed
   registry can drift between services.

## Consequences

- Adding a currency means a shared-lib release. That's deliberate: a new currency also needs its
  corridors, providers and limits reviewed.
- A service on an older shared-lib can't read `Money` in a currency it doesn't know. Roll out shared-lib
  before any producer uses a new constant.
- Fiat uses a flat scale of 4, JPY and KWD included. Rounding to settlement minor units is deferred.
- ETH amounts below 1e-8 are rounded HALF_EVEN. Accepted for now.
- A 1 g gold purchase is stored as 0.03215075 oz, so each trade can be off by up to 1e-8 oz.
