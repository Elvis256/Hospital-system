-- Migration Script: backfill BillFinanceDetails from the legacy double totals
-- Issue #12437 Phase 3 (DB baseline)
-- Author: Claude Opus 5 (1M context)
-- Date: 2026-08-09
--
-- WHAT THIS DOES
--   Creates one BillFinanceDetails row for every Bill that does not have one yet,
--   mirroring the bill-level legacy DOUBLE totals into DECIMAL(18,4) using exactly
--   the mapping in com.divudi.core.util.BillFinanceProjection#fromBillTotals, so a
--   backfilled row is indistinguishable from one written by the live settlement
--   path (FinancialTransactionController#settleIncomeBill).
--
--     BILL.NETTOTAL -> NETTOTAL, BILLNETTOTAL
--     BILL.TOTAL    -> GROSSTOTAL, BILLGROSSTOTAL
--     BILL.DISCOUNT -> BILLDISCOUNT, TOTALDISCOUNT
--     BILL.VAT      -> BILLTAXVALUE
--
--   Every other BillFinanceDetails column is deliberately left NULL: the write
--   path does not populate them either, and NULL correctly means "not migrated"
--   as distinct from "migrated as zero" (see BigDecimal_Migration_Scope.md step 1).
--
-- WHAT THIS DOES NOT DO
--   BILLITEMFINANCEDETAILS is not backfilled. Item-level amounts are not a simple
--   bill-level mirror (pharmacy costing, free quantities, per-line tax) and there
--   is no item-level write path yet, so there is nothing to be consistent with.
--   That is a later increment of Phase 3.
--
-- PRE-REQUISITES
--   1. Full database backup.
--   2. Application STOPPED. The ID allocation below assumes no concurrent writes;
--      running this against a live server can hand out IDs twice.
--   3. Run inside a transaction (see COMMIT at the end) on InnoDB.
--
-- IDEMPOTENT
--   Re-running inserts nothing: every Bill it would touch now has
--   BILLFINANCEDETAILS_ID set, so the driving SELECT returns no rows.
--
-- PRECISION NOTE
--   The DOUBLE -> DECIMAL(18,4) conversion below rounds half away from zero,
--   which matches RoundingMode.HALF_UP used by BigDecimalUtil.money() for both
--   positive and negative amounts. It is not bit-identical to
--   BigDecimal.valueOf(double) in every pathological case, which is why the
--   verification step at the end asserts agreement within the one-cent
--   MoneyReconciliation.DEFAULT_TOLERANCE rather than exact equality.

START TRANSACTION;

-- ---------------------------------------------------------------------------
-- 1. Reserve an ID block above the sequence high-water mark.
--    All entities share the single EclipseLink TABLE generator SEQ_GEN, so
--    SEQ_COUNT is the authority; the MAX() guards are a cheap sanity check.
-- ---------------------------------------------------------------------------
SET @base := (SELECT SEQ_COUNT FROM SEQUENCE WHERE SEQ_NAME = 'SEQ_GEN');
SET @base := GREATEST(
        COALESCE(@base, 0),
        (SELECT COALESCE(MAX(ID), 0) FROM BILL),
        (SELECT COALESCE(MAX(ID), 0) FROM BILLFINANCEDETAILS));

-- ---------------------------------------------------------------------------
-- 2. Stage the bills needing a backfill, with their reserved IDs.
-- ---------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS TMP_BFD_BACKFILL;
CREATE TEMPORARY TABLE TMP_BFD_BACKFILL AS
SELECT b.ID                                        AS BILL_ID,
       @base + ROW_NUMBER() OVER (ORDER BY b.ID)   AS FD_ID,
       CAST(COALESCE(b.NETTOTAL, 0) AS DECIMAL(18,4)) AS NET_TOTAL,
       CAST(COALESCE(b.TOTAL,    0) AS DECIMAL(18,4)) AS GROSS_TOTAL,
       CAST(COALESCE(b.DISCOUNT, 0) AS DECIMAL(18,4)) AS DISCOUNT,
       CAST(COALESCE(b.VAT,      0) AS DECIMAL(18,4)) AS TAX
FROM BILL b
WHERE b.BILLFINANCEDETAILS_ID IS NULL;

SET @backfilled := (SELECT COUNT(*) FROM TMP_BFD_BACKFILL);

-- ---------------------------------------------------------------------------
-- 3. Insert the companion rows.
-- ---------------------------------------------------------------------------
INSERT INTO BILLFINANCEDETAILS
        (ID, NETTOTAL, BILLNETTOTAL, GROSSTOTAL, BILLGROSSTOTAL,
         BILLDISCOUNT, TOTALDISCOUNT, BILLTAXVALUE, CREATEDAT)
SELECT t.FD_ID, t.NET_TOTAL, t.NET_TOTAL, t.GROSS_TOTAL, t.GROSS_TOTAL,
       t.DISCOUNT, t.DISCOUNT, t.TAX, NOW()
FROM TMP_BFD_BACKFILL t;

-- ---------------------------------------------------------------------------
-- 4. Point each Bill at its new companion row (Bill owns the FK).
-- ---------------------------------------------------------------------------
UPDATE BILL b
  JOIN TMP_BFD_BACKFILL t ON t.BILL_ID = b.ID
   SET b.BILLFINANCEDETAILS_ID = t.FD_ID;

-- ---------------------------------------------------------------------------
-- 5. Advance the shared sequence past the block we just consumed.
--    The +50 matches the EclipseLink default allocationSize so the next
--    application-side allocation cannot overlap the backfilled IDs.
-- ---------------------------------------------------------------------------
--    Skipped entirely when nothing was backfilled, so a no-op re-run does not
--    keep advancing the sequence.
UPDATE SEQUENCE
   SET SEQ_COUNT = @base + @backfilled + 50
 WHERE SEQ_NAME = 'SEQ_GEN'
   AND @backfilled > 0
   AND SEQ_COUNT < @base + @backfilled + 50;

-- ---------------------------------------------------------------------------
-- 6. Verify before committing. Expected: BILLS_WITHOUT_DETAILS = 0 and
--    OUT_OF_TOLERANCE = 0. If either is non-zero, ROLLBACK instead of COMMIT.
-- ---------------------------------------------------------------------------
SELECT @backfilled                                              AS ROWS_BACKFILLED,
       (SELECT COUNT(*) FROM BILL
         WHERE BILLFINANCEDETAILS_ID IS NULL)                   AS BILLS_WITHOUT_DETAILS,
       (SELECT COUNT(*)
          FROM BILL b JOIN BILLFINANCEDETAILS f
            ON f.ID = b.BILLFINANCEDETAILS_ID
         WHERE ABS(COALESCE(b.NETTOTAL, 0) - COALESCE(f.NETTOTAL,  0)) >= 0.01
            OR ABS(COALESCE(b.TOTAL,    0) - COALESCE(f.GROSSTOTAL, 0)) >= 0.01
            OR ABS(COALESCE(b.DISCOUNT, 0) - COALESCE(f.BILLDISCOUNT, 0)) >= 0.01
            OR ABS(COALESCE(b.VAT,      0) - COALESCE(f.BILLTAXVALUE, 0)) >= 0.01)
                                                                AS OUT_OF_TOLERANCE;

DROP TEMPORARY TABLE IF EXISTS TMP_BFD_BACKFILL;

COMMIT;

-- ---------------------------------------------------------------------------
-- ROLLBACK PLAN
--   The backfilled rows are exactly those with ID > <the @base recorded above>
--   in BILLFINANCEDETAILS. To undo:
--     UPDATE BILL SET BILLFINANCEDETAILS_ID = NULL WHERE BILLFINANCEDETAILS_ID > <base>;
--     DELETE FROM BILLFINANCEDETAILS WHERE ID > <base>;
--   Record the @base value printed by step 6's run before committing.
-- ---------------------------------------------------------------------------
