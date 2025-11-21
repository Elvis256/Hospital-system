#!/bin/bash
# Comprehensive fix for tokens - fixes both new and old broken tokens
# Includes critical ITEM.STAFF_ID fix for JPA inheritance

echo "Starting comprehensive token fix..."

mysql -u dan -p'Mun00nDa5#' hmis << 'SQL'
-- Step 1: Fix BillSession STAFF_ID from Bill table (last 30 days)
UPDATE BILLSESSION bs
JOIN BILL b ON bs.BILL_ID = b.ID
SET bs.STAFF_ID = COALESCE(b.FROMSTAFF_ID, b.REFERREDBY_ID, b.TOSTAFF_ID)
WHERE bs.STAFF_ID IS NULL
AND bs.RETIRED = 0
AND bs.SESSIONDATE >= CURDATE() - INTERVAL 30 DAY
AND (b.FROMSTAFF_ID IS NOT NULL OR b.REFERREDBY_ID IS NOT NULL OR b.TOSTAFF_ID IS NOT NULL);

SELECT CONCAT('Step 1: Fixed ', ROW_COUNT(), ' BillSession STAFF_IDs') as Result;

-- Step 2: Fix ITEM.STAFF_ID for ALL existing ServiceSessions (critical for JPA)
UPDATE ITEM i
JOIN SERVICESESSION ss ON i.ID = ss.ID
SET i.STAFF_ID = ss.STAFF_ID
WHERE i.DTYPE = 'ServiceSession'
AND (i.STAFF_ID IS NULL OR i.STAFF_ID != ss.STAFF_ID)
AND i.RETIRED = 0;

SELECT CONCAT('Step 2: Fixed ', ROW_COUNT(), ' ITEM.STAFF_ID mismatches (CRITICAL!)') as Result;

-- Step 3: Link BillSessions to ServiceSessions (last 30 days)
UPDATE BILLSESSION bs
JOIN SERVICESESSION ss ON bs.STAFF_ID = ss.STAFF_ID AND bs.SESSIONDATE = ss.SESSIONDATE
JOIN ITEM i ON ss.ID = i.ID AND i.STAFF_ID = ss.STAFF_ID
SET bs.SERVICESESSION_ID = ss.ID
WHERE bs.SERVICESESSION_ID IS NULL
AND bs.STAFF_ID IS NOT NULL
AND bs.RETIRED = 0
AND bs.SESSIONDATE >= CURDATE() - INTERVAL 30 DAY;

SELECT CONCAT('Step 3: Linked ', ROW_COUNT(), ' BillSessions to ServiceSessions') as Result;

-- Report results
SELECT 
    '=== FINAL STATUS ===' as Section,
    COUNT(*) as TotalBillSessions,
    SUM(CASE WHEN STAFF_ID IS NOT NULL THEN 1 ELSE 0 END) as WithDoctor,
    SUM(CASE WHEN SERVICESESSION_ID IS NOT NULL THEN 1 ELSE 0 END) as LinkedToSession,
    SUM(CASE WHEN STAFF_ID IS NULL THEN 1 ELSE 0 END) as StillNeedsFix
FROM BILLSESSION
WHERE SESSIONDATE >= CURDATE() - INTERVAL 30 DAY
AND RETIRED = 0;

-- Verify ServiceSessions are findable
SELECT 
    '=== SERVICESESSIONS ===' as Section,
    COUNT(*) as TotalServiceSessions,
    SUM(CASE WHEN i.STAFF_ID = ss.STAFF_ID THEN 1 ELSE 0 END) as Synchronized,
    SUM(CASE WHEN i.STAFF_ID IS NULL THEN 1 ELSE 0 END) as Broken
FROM SERVICESESSION ss
JOIN ITEM i ON ss.ID = i.ID
WHERE ss.SESSIONDATE >= CURDATE() - INTERVAL 30 DAY
AND ss.RETIRED = 0;
SQL

echo ""
echo "Comprehensive fix completed at $(date)"
