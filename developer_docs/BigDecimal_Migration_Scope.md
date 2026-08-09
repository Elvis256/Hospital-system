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
  and the DB columns `DOUBLE`→`DECIMAL(18,4)`, fix every call site. Cleanest end
  state, but a giant atomic change with a hard DB migration — high risk for a
  live system.

**Recommendation: stay on A** (it's already underway and de-risks a live system),
but add the discipline that's currently missing: a single write-path per amount
and automated divergence checks.

## Phased plan (continuing issue #12437)

1. **Finish the foundation.** `BigDecimalUtil` complete and unit-tested
   (`valueOrZero`, `valueOrNull`, `isNullOrZero`, `money`, `divide` —
   `DECIMAL(18,4)`, `HALF_UP`) plus the reconciliation harness. **Note: the
   `*FinanceDetails` getters intentionally stay nullable** — `getX()` returns the
   raw value so that "not set" (null) stays distinct from "set to zero", a
   contract locked in by `BigDecimalIntegrationTest` /
   `BigDecimalRegressionTest`. Null-safety is a *call-site* concern applied with
   `BigDecimalUtil.valueOrZero(...)`, **not** by wrapping the getters. (This
   supersedes the "add null-safe getters" wording in the older
   `BigDecimal_Refactoring_Implementation_Guide.md`.) *(Done.)*
2. **One write-path.** Make bill/bill-item settlement compute money in
   `BigDecimal` and populate `*FinanceDetails`; have the legacy `double` fields
   *derived* from it (mirror), so there is exactly one authoritative source.
3. **DB baseline.** *(Bill level done; see "Phase 3 findings" below.)* The
   `DECIMAL(18,4)` columns already exist — EclipseLink's
   `ddl-generation=create-tables` created them, so the "add columns" half of this
   step was already satisfied and the real work is the **backfill**:
   `db/migration/05_backfill_billfinancedetails_from_double.sql` gives every
   existing `Bill` a `BillFinanceDetails` mirroring its legacy `double` totals.
   Still open: the item-level backfill, and adopting an ordered migration runner
   (Flyway/Liquibase — see the separate migration finding).
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
| Scale/rounding inconsistency | One policy everywhere: `DECIMAL(18,4)`, `HALF_UP`; enforce via `BigDecimalUtil`, ban ad-hoc `new BigDecimal(double)` |
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
  lab) asserting exact expected `DECIMAL(18,4)` totals.
- Report parity: each migrated report tested legacy-vs-new before cutover.

## Rough effort

Foundation finish ~1 sprint; write-path + DB baseline ~1–2 sprints; then
~1 sprint per module (6 modules) + reporting; retire phase ~1 sprint. Order of
**2–3 months** of focused work with continuous reconciliation — appropriately
sized as a tracked epic (#12437), not a single change.

## Phase 3 findings (DB baseline)

Measured against the running system, not assumed:

| Question | Finding |
|---|---|
| Do the `DECIMAL` columns exist? | **Yes.** `BILLFINANCEDETAILS` has 42 `decimal(18,4)` columns, `BILLITEMFINANCEDETAILS` 61. All nullable except `UNITSPERPACK` — exactly as `db/migration/README_BigDecimal_Migration_Strategy.md` intends. |
| Who created them? | EclipseLink `ddl-generation=create-tables` (`persistence.xml`), **not** a migration script. The numbered `db/migration/*.sql` files are hand-applied and not tracked by any runner. |
| Precision | **`DECIMAL(18,4)`**, not the `(19,4)` this document previously claimed. 18,4 is what the 100+ entity annotations and the live schema actually use, so the document was corrected — not the schema. Scale 4 + `HALF_UP` matches `BigDecimalUtil.MONEY_SCALE`. |
| How is the companion row linked? | `Bill` owns the FK (`BILL.BILLFINANCEDETAILS_ID`, UNIQUE); `BillFinanceDetails` is the inverse side and has **no** `BILL_ID` column. A backfill must insert the row *and* update the bill. |
| ID allocation | One shared EclipseLink TABLE generator (`SEQUENCE.SEQ_GEN`, default `allocationSize` 50). A backfill must reserve IDs above the high-water mark and advance `SEQ_COUNT`, or the application will later re-issue them. |

**Delivered:** `db/migration/05_backfill_billfinancedetails_from_double.sql` —
idempotent, transactional, sequence-safe, using exactly the
`BillFinanceProjection.fromBillTotals` mapping so backfilled rows are
indistinguishable from rows the live settlement path writes. Verified locally on
synthetic pre-migration bills covering ordinary, repeating-decimal, negative
(credit-note), zero and exact-half amounts; the SQL `DOUBLE`→`DECIMAL(18,4)`
conversion was confirmed to agree with Java `HALF_UP` on every case, including
`±10.00005 → ±10.0001`.

## Immediate next step (smallest safe increment)

Steps 1 and 2 are done (helpers + harness; bill-level write-path wired into
cashier income-bill settlement). Step 3 is done at bill level.

The next increment is the **item-level baseline**: `BILLITEMFINANCEDETAILS` has
no write path and no backfill. Unlike the bill level it is not a straight mirror
— pharmacy costing, free quantities and per-line tax mean the item amounts must
be *computed*, not copied — so the write path has to come first, then a backfill
consistent with it. Until then, the bill-level `BigDecimal` totals are the only
migrated amounts and reporting cutover (step 4) should start there.
