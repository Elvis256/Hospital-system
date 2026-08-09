# `double` → `BigDecimal` Monetary Migration — Scope

> Scoping analysis for continuing the monetary-precision migration (GitHub issue
> #12437). Complements `BigDecimal_Refactoring_Implementation_Guide.md`.

## Why

Monetary amounts across the billing domain are stored as primitive `double`.
Binary floating point cannot represent decimal currency exactly, so fee splits,
tax, discounts and running balances accumulate rounding error over many
operations — a correctness risk in patient invoices and reconciliation.

## Current state (measured)

The team has already chosen and started a **strangler** approach: rather than
convert the `double` fields in place, new companion entities hold the money as
`BigDecimal` and coexist with the legacy fields.

| Signal | Count |
|---|---|
| Companion entities live | `BillFinanceDetails` (36 BigDecimal fields), `BillItemFinanceDetails` (~62) + DTOs |
| Helper | `com.divudi.core.util.BigDecimalUtil` (null-safe ops) |
| Entities importing `BigDecimal` | 3 of 292 |
| Files referencing `BillFinanceDetails` | 46 |
| Files referencing `BillItemFinanceDetails` | 40 |
| Files referencing `BigDecimalUtil` | 10 |

So the foundation exists and has early adoption, but the legacy `double` model is
still the source of truth almost everywhere.

## Blast radius (measured)

| Dimension | Size |
|---|---|
| `double`/`Double` fields in entities | **748** total (~**534** money-named) |
| `double` money columns (DB) | BILL **37**, BILLITEM **19**, BILLFEE **9**, FEE **5**, PAYMENT **2**, STOCK **2**, PATIENTDEPOSIT **1**, BILLFEEPAYMENT **1** |
| `.getNetTotal(` call sites | **156 files** |
| `.setNetTotal(` call sites | **147 files** |
| `.getTotal(` / `.getDiscount(` | **126** / **91 files** |
| `.getPaidValue(` / `.getFeeValue(` / `.getPaidAmount(` | **63** / **58** / **37 files** |

This is a **hundreds-of-files**, multi-month effort touching billing, pharmacy,
inward, lab, channelling, cashier and every report that sums money.

## The central strategic decision

Two `BigDecimal` fields can't just replace two `double` fields, because during
the transition the same amount lives in **two places** (legacy `double` +
companion `BigDecimal`). Pick one model per slice and cut over; never let both be
independently written, or they silently diverge. Options:

- **A. Companion-entity coexistence (team's current path).** Write BigDecimal to
  `*FinanceDetails`, keep `double` as a mirror during transition, migrate readers
  slice by slice, then retire the `double` fields. Lowest risk per step; slowest;
  requires a bulletproof sync so the two never diverge.
- **B. In-place field conversion.** Change the entity fields `double`→`BigDecimal`
  and the DB columns `DOUBLE`→`DECIMAL(19,4)`, fix every call site. Cleanest end
  state, but a giant atomic change with a hard DB migration — high risk for a
  live system.

**Recommendation: stay on A** (it's already underway and de-risks a live system),
but add the discipline that's currently missing: a single write-path per amount
and automated divergence checks.

## Phased plan (continuing issue #12437)

1. **Finish the foundation.** Nullability + null-safe getters on both
   `*FinanceDetails`; `BigDecimalUtil` fully unit-tested (`valueOrZero`,
   `valueOrNull`, `isNullOrZero`, scale/rounding — always `RoundingMode.HALF_UP`,
   `DECIMAL(19,4)`). *(Partly done.)*
2. **One write-path.** Make bill/bill-item settlement compute money in
   `BigDecimal` and populate `*FinanceDetails`; have the legacy `double` fields
   *derived* from it (mirror), so there is exactly one authoritative source.
3. **DB baseline.** Add `DECIMAL(19,4)` columns for the companion tables via a
   real, ordered migration (Flyway/Liquibase — see the separate migration
   finding); backfill from existing `double` values once.
4. **Reporting cutover, by report.** Point each money report/DTO at the
   `BigDecimal` fields; keep a reconciliation test asserting legacy-vs-BigDecimal
   totals match within tolerance until the report is fully switched.
5. **Module cutover, by module.** OPD → pharmacy → inward → lab → channelling →
   cashier. Each module: settlement writes BigDecimal, screens/read paths switch,
   double becomes read-only mirror.
6. **Retire the `double` fields.** Once no reader uses them, drop the mirror and
   the `DOUBLE` columns. This is the only step that removes the divergence risk
   for good.

## Risks & mitigations

| Risk | Mitigation |
|---|---|
| **Two sources of truth diverge** | Single write-path (step 2) + a scheduled job/test asserting `abs(double − bigDecimal) < 0.01` per bill |
| Scale/rounding inconsistency | One policy everywhere: `DECIMAL(19,4)`, `HALF_UP`; enforce via `BigDecimalUtil`, ban ad-hoc `new BigDecimal(double)` |
| `equals`/`compareTo` on BigDecimal | Always compare with `compareTo`, never `equals` (scale-sensitive) — lint/review rule |
| Huge call-site churn | Migrate by module behind the mirror; never a big-bang |
| DB migration on live data | Backfill + reconcile in staging first; the migration only *adds* columns until the final retire step |
| Autoboxing NPEs (nullable Double) | `BigDecimalUtil.valueOrZero()` at every read |

## Testing strategy

- Unit: `BigDecimalUtil` (all ops, rounding, nulls) — extend the existing
  `BigDecimal*Test` suite.
- Reconciliation: per-bill legacy-`double` vs `BigDecimal` total equality within
  tolerance, run continuously during coexistence.
- Regression: golden-value tests for representative bills (OPD, pharmacy, inward,
  lab) asserting exact expected `DECIMAL(19,4)` totals.
- Report parity: each migrated report tested legacy-vs-new before cutover.

## Rough effort

Foundation finish ~1 sprint; write-path + DB baseline ~1–2 sprints; then
~1 sprint per module (6 modules) + reporting; retire phase ~1 sprint. Order of
**2–3 months** of focused work with continuous reconciliation — appropriately
sized as a tracked epic (#12437), not a single change.

## Immediate next step (smallest safe increment)

Complete **step 1** for one entity end-to-end: finish `BillFinanceDetails`
nullability + null-safe getters, land full `BigDecimalUtil` unit tests, and add
the reconciliation test skeleton. That closes Phase 1 of the existing guide and
establishes the reconciliation harness every later phase depends on.
