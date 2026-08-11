-- Migration Script: backfill BillItemFinanceDetails from the legacy double amounts
-- Issue #12437 Phase 3 (DB baseline, item level)
-- Author: Claude Opus 5 (1M context)
-- Date: 2026-08-11
--
-- WHAT THIS DOES
--   The item-level counterpart of
--   05_backfill_billfinancedetails_from_double.sql. Creates one
--   BillItemFinanceDetails row for every BillItem that does not have one yet,
--   mirroring the line-level legacy DOUBLE amounts into DECIMAL(18,4) using
--   exactly the mapping in
--   com.divudi.core.util.BillItemFinanceProjection#applyLineTotals, so a
--   backfilled row is indistinguishable from one written by the live OPD
--   settlement path (BillBeanController#calculateBillItemsForOpdBill).
--
--     BILLITEM.QTY        -> QUANTITY
--     BILLITEM.RATE       -> GROSSRATE      (the known-typo `Rate` field)
--     BILLITEM.NETRATE    -> NETRATE
--     BILLITEM.GROSSVALUE -> GROSSTOTAL
--     BILLITEM.NETVALUE   -> NETTOTAL
--     BILLITEM.DISCOUNT   -> TOTALDISCOUNT
--     BILLITEM.VAT        -> TOTALTAX
--
--   The line*/bill* discount and tax split is deliberately left NULL: the legacy
--   double model records only the combined per-line figure, never the
--   decomposition, so the combined value maps to the total* fields and the split
--   stays NULL, meaning "not migrated" as distinct from "migrated as zero".
--   The pharmacy costing fields (valueAtCostRate, valueAtRetailRate, ...) are
--   likewise left NULL - they are computed by the pharmacy paths, not mirrored,
--   and there is nothing in the legacy line amounts to derive them from.
--
-- UNITSPERPACK
--   Unlike every other DECIMAL column on this table, UNITSPERPACK is NOT NULL
--   with no database default (a deliberate exception - see
--   README_BigDecimal_Migration_Strategy.md: it is a business-critical
--   conversion factor that must never be null). Backfilled rows therefore get
--   the entity default of 1.0000, matching @Column(nullable = false) with
--   BigDecimal.ONE on BillItemFinanceDetails.
--
-- PRE-REQUISITES
--   1. Full database backup.
--   2. Application STOPPED. The ID allocation below assumes no concurrent
--      writes; running this against a live server can hand out IDs twice.
--   3. InnoDB (the script runs in a transaction).
--
--   Run 05_backfill_billfinancedetails_from_double.sql first if the bill-level
--   baseline has not been established. The two are independent - neither reads
--   the other's output - but bill-then-item is the natural order and makes the
--   reconciliation reports easier to read.
--
-- IDEMPOTENT
--   Re-running inserts nothing: every BillItem it would touch now has
--   BILLITEMFINANCEDETAILS_ID set, so the driving SELECT returns no rows. The
--   sequence is only advanced when rows were actually backfilled.
--
-- PRECISION NOTE
--   The DOUBLE -> DECIMAL(18,4) conversion rounds half away from zero, matching
--   RoundingMode.HALF_UP used by BigDecimalUtil.money() for both positive and
--   negative amounts. It is not bit-identical to BigDecimal.valueOf(double) in
--   every pathological case, so the verification asserts agreement within the
--   one-cent MoneyReconciliation.DEFAULT_TOLERANCE rather than exact equality.

START TRANSACTION;

-- ---------------------------------------------------------------------------
-- 1. Reserve an ID block above the sequence high-water mark.
--    All entities share the single EclipseLink TABLE generator SEQ_GEN, so
--    SEQ_COUNT is the authority; the MAX() guards are a cheap sanity check.
-- ---------------------------------------------------------------------------
SET @base := (SELECT SEQ_COUNT FROM SEQUENCE WHERE SEQ_NAME = 'SEQ_GEN');
SET @base := GREATEST(
        COALESCE(@base, 0),
        (SELECT COALESCE(MAX(ID), 0) FROM BILLITEM),
        (SELECT COALESCE(MAX(ID), 0) FROM BILLITEMFINANCEDETAILS));

-- ---------------------------------------------------------------------------
-- 2. Stage the bill items needing a backfill, with their reserved IDs.
--    Every legacy money column on BILLITEM is a nullable DOUBLE, hence COALESCE.
-- ---------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS TMP_BIFD_BACKFILL;
CREATE TEMPORARY TABLE TMP_BIFD_BACKFILL AS
SELECT bi.ID                                          AS BILLITEM_ID,
       @base + ROW_NUMBER() OVER (ORDER BY bi.ID)     AS FD_ID,
       CAST(COALESCE(bi.QTY,        0) AS DECIMAL(18,4)) AS QUANTITY,
       CAST(COALESCE(bi.RATE,       0) AS DECIMAL(18,4)) AS GROSS_RATE,
       CAST(COALESCE(bi.NETRATE,    0) AS DECIMAL(18,4)) AS NET_RATE,
       CAST(COALESCE(bi.GROSSVALUE, 0) AS DECIMAL(18,4)) AS GROSS_TOTAL,
       CAST(COALESCE(bi.NETVALUE,   0) AS DECIMAL(18,4)) AS NET_TOTAL,
       CAST(COALESCE(bi.DISCOUNT,   0) AS DECIMAL(18,4)) AS TOTAL_DISCOUNT,
       CAST(COALESCE(bi.VAT,        0) AS DECIMAL(18,4)) AS TOTAL_TAX
FROM BILLITEM bi
WHERE bi.BILLITEMFINANCEDETAILS_ID IS NULL;

SET @backfilled := (SELECT COUNT(*) FROM TMP_BIFD_BACKFILL);

-- ---------------------------------------------------------------------------
-- 3. Insert the companion rows. UNITSPERPACK is NOT NULL - see header.
-- ---------------------------------------------------------------------------
INSERT INTO BILLITEMFINANCEDETAILS
        (ID, QUANTITY, GROSSRATE, NETRATE, GROSSTOTAL, NETTOTAL,
         TOTALDISCOUNT, TOTALTAX, UNITSPERPACK, CREATEDAT)
SELECT t.FD_ID, t.QUANTITY, t.GROSS_RATE, t.NET_RATE, t.GROSS_TOTAL, t.NET_TOTAL,
       t.TOTAL_DISCOUNT, t.TOTAL_TAX, 1.0000, NOW()
FROM TMP_BIFD_BACKFILL t;

-- ---------------------------------------------------------------------------
-- 4. Point each BillItem at its new companion row (BillItem owns the FK).
--    NOTE: unlike BILL.BILLFINANCEDETAILS_ID, this column has no UNIQUE index,
--    so the database will not catch a double-link on its own. The reserved-ID
--    block above guarantees one fresh row per item.
-- ---------------------------------------------------------------------------
UPDATE BILLITEM bi
  JOIN TMP_BIFD_BACKFILL t ON t.BILLITEM_ID = bi.ID
   SET bi.BILLITEMFINANCEDETAILS_ID = t.FD_ID;

-- ---------------------------------------------------------------------------
-- 5. Advance the shared sequence past the block we just consumed.
--    The +50 matches the EclipseLink default allocationSize. Skipped entirely
--    when nothing was backfilled, so a no-op re-run does not drift the sequence.
-- ---------------------------------------------------------------------------
UPDATE SEQUENCE
   SET SEQ_COUNT = @base + @backfilled + 50
 WHERE SEQ_NAME = 'SEQ_GEN'
   AND @backfilled > 0
   AND SEQ_COUNT < @base + @backfilled + 50;

-- ---------------------------------------------------------------------------
-- 6. Verify before committing. Expected: ITEMS_WITHOUT_DETAILS = 0,
--    OUT_OF_TOLERANCE = 0 and DOUBLE_LINKED = 0.
--    If any is non-zero, ROLLBACK instead of COMMIT.
-- ---------------------------------------------------------------------------
SELECT @backfilled                                              AS ROWS_BACKFILLED,
       (SELECT COUNT(*) FROM BILLITEM
         WHERE BILLITEMFINANCEDETAILS_ID IS NULL)               AS ITEMS_WITHOUT_DETAILS,
       (SELECT COUNT(*)
          FROM BILLITEM bi JOIN BILLITEMFINANCEDETAILS f
            ON f.ID = bi.BILLITEMFINANCEDETAILS_ID
         WHERE ABS(COALESCE(bi.QTY,        0) - COALESCE(f.QUANTITY,      0)) >= 0.01
            OR ABS(COALESCE(bi.RATE,       0) - COALESCE(f.GROSSRATE,     0)) >= 0.01
            OR ABS(COALESCE(bi.NETRATE,    0) - COALESCE(f.NETRATE,       0)) >= 0.01
            OR ABS(COALESCE(bi.GROSSVALUE, 0) - COALESCE(f.GROSSTOTAL,    0)) >= 0.01
            OR ABS(COALESCE(bi.NETVALUE,   0) - COALESCE(f.NETTOTAL,      0)) >= 0.01
            OR ABS(COALESCE(bi.DISCOUNT,   0) - COALESCE(f.TOTALDISCOUNT, 0)) >= 0.01
            OR ABS(COALESCE(bi.VAT,        0) - COALESCE(f.TOTALTAX,      0)) >= 0.01)
                                                                AS OUT_OF_TOLERANCE,
       (SELECT COUNT(*) FROM (
           SELECT BILLITEMFINANCEDETAILS_ID
             FROM BILLITEM
            WHERE BILLITEMFINANCEDETAILS_ID IS NOT NULL
            GROUP BY BILLITEMFINANCEDETAILS_ID
           HAVING COUNT(*) > 1) d)                              AS DOUBLE_LINKED;

DROP TEMPORARY TABLE IF EXISTS TMP_BIFD_BACKFILL;

COMMIT;

-- ---------------------------------------------------------------------------
-- ROLLBACK PLAN
--   The backfilled rows are exactly those with ID > <the @base recorded above>
--   in BILLITEMFINANCEDETAILS. To undo:
--     UPDATE BILLITEM SET BILLITEMFINANCEDETAILS_ID = NULL
--      WHERE BILLITEMFINANCEDETAILS_ID > <base>;
--     DELETE FROM BILLITEMFINANCEDETAILS WHERE ID > <base>;
--   Record the @base value before committing.
-- ---------------------------------------------------------------------------
