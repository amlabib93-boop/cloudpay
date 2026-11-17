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

## Money record — 2026-11-16
**Concepts:**
- A record's compact constructor runs before the fields are assigned, so it can validate and replace incoming values. It is the one place that covers every way a record is created, because the canonical constructor can't be hidden.
- `BigDecimal.equals` compares scale too; `compareTo` compares value only. `hashCode` must agree with `equals`, so use `stripTrailingZeros()`.
- HALF_EVEN rounds the .5 cases up half the time and down half the time, so totals don't drift in one direction.
- `new BigDecimal(double)` stores the binary error of the double; a String factory makes the exact path the easy one.

**Decision:** Normalise scale in the compact constructor and also override `equals`/`hashCode` to compare by value (`compareTo` plus a stripped hash), so value-based equality stays correct even if normalisation is ever removed. Rejected: relying on the record defaults (correct today only because of normalisation). Negative amounts are allowed; non-negative balances are the wallet's rule. Too many decimals are rounded, not rejected. JSON serialization is left to each service.

**Interview Q&A:**
- *Why override `equals` if the scale is normalised?* So the code states the rule (compare values) instead of depending on a side effect of construction. The cost is keeping `hashCode` consistent with `equals`.
- *What breaks if `hashCode` skipped `stripTrailingZeros`?* Nothing today. Without normalisation, equal values would hash differently and hash-based collections would hold duplicates.
- *Why HALF_EVEN over HALF_UP?* HALF_UP biases totals upward over many roundings; HALF_EVEN is unbiased, which is why accounting and IEEE 754 use it.

**Open questions / to revisit:**
- A strict factory that rejects too many decimals, if a service needs it.
- Capping amount size at the API edge (`1E+100000000` is a cheap denial-of-service input).

## Money arithmetic and comparison — 2026-11-17
**Concepts:**
- No implicit FX: `add`, `subtract` and `compareTo` throw `CurrencyMismatchException` across currencies.
- `Comparable` can throw on values it can't order, as the JDK does with `ClassCastException`; a fake order across currencies would hide bugs.
- `compareTo == 0` exactly when `equals` (same currency, same value), so `Money` is safe in both `HashSet` and `TreeSet`. Plain `BigDecimal` is not.
- `BigDecimal` can't overflow; the real limit is the database column (`numeric(19,4)`).

**Decision:** Instance methods on `Money`, with all currency checks in one private helper. `CurrencyMismatchException` extends `IllegalArgumentException` and carries `expected()` and `actual()`, so handlers can build structured errors. Rejected: a base `CloudPayException` (no second caller yet), ordering by currency code then amount (meaningless and hides bugs), and adding `multiply`/`divide`/`allocate` before fees or FX need them.

**Interview Q&A:**
- *Why does `compareTo` throw on mixed currencies?* Comparing BTC with USD needs an FX rate; that must be an explicit, dated step. To sort balances by value, convert to a display currency first.
- *Static `Money.add(a, b)` or instance methods?* Instance methods read like the domain (`balance.subtract(amount)`) and keep the currency check on the owning object; idiomatic for Java value types.
- *Does `Money` prevent overdraft?* No, it only computes. The wallet needs `@Version` or a conditional `UPDATE ... WHERE balance >= :amount` so concurrent debits can't both pass on stale data.

**Open questions / to revisit:**
- `multiply`/`divide`/`allocate` with explicit rounding when fees (P4) or FX (P9) arrive.
- A base `CloudPayException` once a second shared exception exists.
