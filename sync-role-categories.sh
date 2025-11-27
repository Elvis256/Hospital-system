#!/bin/bash

# HMIS Role to Staff Category Sync Script
# This script automatically assigns staff categories to users based on their roles
# Run this periodically or after creating new users

DB_USER="hmisd"
DB_PASS="hmisd123"
DB_NAME="hmisd"

echo "=========================================="
echo "HMIS Role to Staff Category Sync"
echo "=========================================="
echo ""

mysql -u $DB_USER -p$DB_PASS $DB_NAME << 'EOF'

-- Create staff categories if they don't exist
INSERT IGNORE INTO CATEGORY (ID, DTYPE, NAME, RETIRED, CREATEDAT)
SELECT COALESCE(MAX(ID), 0) + 1, 'Category', 'Doctor', 0, NOW() FROM CATEGORY
WHERE NOT EXISTS (SELECT 1 FROM CATEGORY WHERE NAME = 'Doctor');

INSERT IGNORE INTO CATEGORY (ID, DTYPE, NAME, RETIRED, CREATEDAT)
SELECT COALESCE(MAX(ID), 0) + 1, 'Category', 'Nurse', 0, NOW() FROM CATEGORY
WHERE NOT EXISTS (SELECT 1 FROM CATEGORY WHERE NAME = 'Nurse');

INSERT IGNORE INTO CATEGORY (ID, DTYPE, NAME, RETIRED, CREATEDAT)
SELECT COALESCE(MAX(ID), 0) + 1, 'Category', 'Lab Technician', 0, NOW() FROM CATEGORY
WHERE NOT EXISTS (SELECT 1 FROM CATEGORY WHERE NAME = 'Lab Technician');

INSERT IGNORE INTO CATEGORY (ID, DTYPE, NAME, RETIRED, CREATEDAT)
SELECT COALESCE(MAX(ID), 0) + 1, 'Category', 'Receptionist', 0, NOW() FROM CATEGORY
WHERE NOT EXISTS (SELECT 1 FROM CATEGORY WHERE NAME = 'Receptionist');

-- Fix Doctor role users
UPDATE STAFF s
JOIN WEBUSER wu ON s.ID = wu.STAFF_ID
JOIN WEBUSERROLEUSER wru ON wu.ID = wru.WEBUSER_ID
JOIN WEBUSERROLE r ON wru.WEBUSERROLE_ID = r.ID
SET s.STAFFCATEGORY_ID = (SELECT ID FROM CATEGORY WHERE NAME = 'Doctor' LIMIT 1)
WHERE r.NAME = 'Doctor' 
AND s.STAFFCATEGORY_ID IS NULL;

-- Fix Nurse role users
UPDATE STAFF s
JOIN WEBUSER wu ON s.ID = wu.STAFF_ID
JOIN WEBUSERROLEUSER wru ON wu.ID = wru.WEBUSER_ID
JOIN WEBUSERROLE r ON wru.WEBUSERROLE_ID = r.ID
SET s.STAFFCATEGORY_ID = (SELECT ID FROM CATEGORY WHERE NAME = 'Nurse' LIMIT 1)
WHERE r.NAME = 'Nurse' 
AND s.STAFFCATEGORY_ID IS NULL;

-- Fix Pharmacist role users
UPDATE STAFF s
JOIN WEBUSER wu ON s.ID = wu.STAFF_ID
JOIN WEBUSERROLEUSER wru ON wu.ID = wru.WEBUSER_ID
JOIN WEBUSERROLE r ON wru.WEBUSERROLE_ID = r.ID
SET s.STAFFCATEGORY_ID = (SELECT ID FROM CATEGORY WHERE NAME = 'PHARMACIST' LIMIT 1)
WHERE r.NAME = 'Pharmacist' 
AND s.STAFFCATEGORY_ID IS NULL;

-- Fix Lab Technician role users
UPDATE STAFF s
JOIN WEBUSER wu ON s.ID = wu.STAFF_ID
JOIN WEBUSERROLEUSER wru ON wu.ID = wru.WEBUSER_ID
JOIN WEBUSERROLE r ON wru.WEBUSERROLE_ID = r.ID
SET s.STAFFCATEGORY_ID = (SELECT ID FROM CATEGORY WHERE NAME = 'LAB TECHNICIAN' LIMIT 1)
WHERE r.NAME LIKE '%Lab%Technician%'
AND s.STAFFCATEGORY_ID IS NULL;

-- Fix Receptionist role users
UPDATE STAFF s
JOIN WEBUSER wu ON s.ID = wu.STAFF_ID
JOIN WEBUSERROLEUSER wru ON wu.ID = wru.WEBUSER_ID
JOIN WEBUSERROLE r ON wru.WEBUSERROLE_ID = r.ID
SET s.STAFFCATEGORY_ID = (SELECT ID FROM CATEGORY WHERE NAME = 'Receptionist' LIMIT 1)
WHERE r.NAME = 'Receptionist' 
AND s.STAFFCATEGORY_ID IS NULL;

-- Show summary
SELECT '========================================' as '';
SELECT 'SYNC COMPLETE - SUMMARY BY ROLE' as '';
SELECT '========================================' as '';

SELECT 
    r.NAME as Role,
    COUNT(DISTINCT wu.ID) as Total_Users,
    COUNT(DISTINCT CASE WHEN s.STAFFCATEGORY_ID IS NOT NULL THEN wu.ID END) as With_Category,
    COUNT(DISTINCT CASE WHEN s.STAFFCATEGORY_ID IS NULL THEN wu.ID END) as Missing_Category
FROM WEBUSERROLE r
LEFT JOIN WEBUSERROLEUSER wru ON r.ID = wru.WEBUSERROLE_ID
LEFT JOIN WEBUSER wu ON wru.WEBUSER_ID = wu.ID
LEFT JOIN STAFF s ON wu.STAFF_ID = s.ID
WHERE wu.STAFF_ID IS NOT NULL
GROUP BY r.NAME
ORDER BY r.NAME;

SELECT '' as '';
SELECT 'Users with missing categories (if any):' as '';

SELECT 
    wu.NAME as Username,
    r.NAME as Role,
    s.CODE as Staff_Code,
    'MISSING CATEGORY' as Status
FROM WEBUSER wu
JOIN WEBUSERROLEUSER wru ON wu.ID = wru.WEBUSER_ID
JOIN WEBUSERROLE r ON wru.WEBUSERROLE_ID = r.ID
LEFT JOIN STAFF s ON wu.STAFF_ID = s.ID
WHERE wu.STAFF_ID IS NOT NULL
AND s.STAFFCATEGORY_ID IS NULL
LIMIT 10;

EOF

echo ""
echo "=========================================="
echo "Sync completed!"
echo "=========================================="
echo ""
echo "Note: Users need to log out and log back in"
echo "      for changes to take effect."
echo ""
