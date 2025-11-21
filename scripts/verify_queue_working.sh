#!/bin/bash
# Verify that clinical queue will work for a doctor

if [ -z "$1" ]; then
    echo "Usage: $0 <doctor_staff_id> [date]"
    echo "Example: $0 2001 2025-11-10"
    exit 1
fi

STAFF_ID=$1
DATE=${2:-$(date +%Y-%m-%d)}

mysql -u dan -p'Mun00nDa5#' hmis << SQL
SELECT '=== DOCTOR INFORMATION ===' as Section;
SELECT 
    s.ID as StaffID,
    p.NAME as DoctorName,
    p.TITLE
FROM STAFF s
JOIN PERSON p ON s.PERSON_ID = p.ID
WHERE s.ID = ${STAFF_ID};

SELECT '=== SERVICESESSION CHECK ===' as Section;
SELECT 
    i.ID as ServiceSessionID,
    i.STAFF_ID as Item_StaffID,
    ss.STAFF_ID as SS_StaffID,
    ss.SESSIONDATE,
    CASE 
        WHEN i.STAFF_ID = ss.STAFF_ID THEN 'OK' 
        ELSE 'BROKEN - STAFF_ID MISMATCH!' 
    END as Status
FROM ITEM i
JOIN SERVICESESSION ss ON i.ID = ss.ID
WHERE i.DTYPE = 'ServiceSession'
AND ss.STAFF_ID = ${STAFF_ID}
AND ss.SESSIONDATE = '${DATE}'
AND i.RETIRED = 0;

SELECT '=== QUEUE SUMMARY ===' as Section;
SELECT 
    COUNT(*) as TotalTokens,
    SUM(CASE WHEN bs.PATIENTENCOUNTER_ID IS NULL THEN 1 ELSE 0 END) as ToComplete,
    SUM(CASE WHEN bs.PATIENTENCOUNTER_ID IS NOT NULL THEN 1 ELSE 0 END) as Completed,
    SUM(CASE WHEN bs.SERVICESESSION_ID IS NULL THEN 1 ELSE 0 END) as NotLinkedToSession
FROM BILLSESSION bs
WHERE bs.STAFF_ID = ${STAFF_ID}
AND bs.SESSIONDATE = '${DATE}'
AND bs.RETIRED = 0;

SELECT '=== SAMPLE TOKENS ===' as Section;
SELECT 
    bs.SERIALNO as Token,
    CASE WHEN bs.PATIENTENCOUNTER_ID IS NULL THEN 'To Complete' ELSE 'Completed' END as Status,
    pat_p.NAME as Patient
FROM BILLSESSION bs
LEFT JOIN BILL b ON bs.BILL_ID = b.ID
LEFT JOIN PATIENT pat ON b.PATIENT_ID = pat.ID
LEFT JOIN PERSON pat_p ON pat.PERSON_ID = pat_p.ID
WHERE bs.STAFF_ID = ${STAFF_ID}
AND bs.SESSIONDATE = '${DATE}'
AND bs.RETIRED = 0
ORDER BY bs.SERIALNO
LIMIT 5;
SQL
