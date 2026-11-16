# shared-lib — learning notes

## Currency enum (G1, ADR 0001) — 2026-11-15
**Concepts:**
- A closed enum makes "which currencies we support" a reviewed, compile-time decision.
- Storage scale is a property of the currency kind (fiat 4, crypto 8, gold 8), not of the incoming amount.
- `java.util.Currency` fraction digits are ISO minor units (JPY 0, XAU -1), not storage scale.
- Adding an enum constant is source compatible but not wire compatible: consumers must upgrade before producers use it.

**Decision:** A closed `Currency` enum whose `Kind` sets the scale. Rejected: a sealed interface wrapping `java.util.Currency` (polymorphic JSON and a JPA converter in every service, for flexibility we don't need), and a record plus registry (invalid codes fail only at runtime, and the registry can drift between services). XAU is one troy ounce (ISO meaning) at scale 8; grams are a display conversion.

**Interview Q&A:**
- *Why not `java.util.Currency`?* It's ISO 4217 only (no BTC, ETH, SOL), and its fraction digits are minor units, not a storage scale. We need one scale rule every service agrees on.
- *Isn't a release per new currency rigid?* On purpose: a new currency is a business change (corridors, providers, limits). Roll it out like a schema change, consumers first. At 150 currencies with frequent additions, move to a versioned reference-data registry and accept runtime validation.
- *Why tie scale to the type?* So `Money` normalises on construction, and same-currency amounts always share a scale whatever the input format.

**Open questions / to revisit:**
- Per-currency scale override (for example USDC at 6) if a provider needs it.
- Settlement rounding to ISO minor units (JPY 0, KWD 3) when a provider needs it.
