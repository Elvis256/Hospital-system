-- Optimistic locking (@Version) support for PatientDeposit and Stock.
--
-- EclipseLink uses this VERSION column to detect concurrent modification and
-- prevent lost updates on patient-deposit balances and pharmacy/store stock
-- (a second concurrent commit fails with an OptimisticLockException instead of
-- silently overwriting the first). Existing rows are initialized to 0.
--
-- IMPORTANT: run this migration BEFORE deploying the build that adds @Version
-- to com.divudi.core.entity.PatientDeposit and com.divudi.core.entity.pharmacy.Stock.
-- The app uses EclipseLink create-tables DDL generation, which does NOT add
-- columns to existing tables, so without this migration every read/write of
-- these entities would fail with "Unknown column 'VERSION'".

ALTER TABLE patientdeposit ADD COLUMN IF NOT EXISTS VERSION BIGINT NOT NULL DEFAULT 0;
UPDATE patientdeposit SET VERSION = 0 WHERE VERSION IS NULL;

ALTER TABLE stock ADD COLUMN IF NOT EXISTS VERSION BIGINT NOT NULL DEFAULT 0;
UPDATE stock SET VERSION = 0 WHERE VERSION IS NULL;

SELECT 'VERSION columns added to patientdeposit and stock' AS Status;
