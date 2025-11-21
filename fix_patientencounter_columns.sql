-- Fix PatientEncounter table - Add missing columns if they don't exist

-- Add QUEUE_NUMBER column if it doesn't exist
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'PATIENTENCOUNTER'
AND COLUMN_NAME = 'QUEUENUMBER';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE PATIENTENCOUNTER ADD COLUMN QUEUENUMBER INT NULL',
    'SELECT ''Column QUEUENUMBER already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Ensure temperature column exists (should already be there based on entity)
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'PATIENTENCOUNTER'
AND COLUMN_NAME = 'TEMPERATURE';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE PATIENTENCOUNTER ADD COLUMN TEMPERATURE DOUBLE NULL',
    'SELECT ''Column TEMPERATURE already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add allergies column for triage information
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'PATIENTENCOUNTER'
AND COLUMN_NAME = 'ALLERGIES';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE PATIENTENCOUNTER ADD COLUMN ALLERGIES TEXT NULL',
    'SELECT ''Column ALLERGIES already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add presenting complaint column
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'PATIENTENCOUNTER'
AND COLUMN_NAME = 'PRESENTINGCOMPLAINT';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE PATIENTENCOUNTER ADD COLUMN PRESENTINGCOMPLAINT TEXT NULL',
    'SELECT ''Column PRESENTINGCOMPLAINT already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add chief complaint column  
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'PATIENTENCOUNTER'
AND COLUMN_NAME = 'CHIEFCOMPLAINT';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE PATIENTENCOUNTER ADD COLUMN CHIEFCOMPLAINT TEXT NULL',
    'SELECT ''Column CHIEFCOMPLAINT already exists'' AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'PatientEncounter table columns updated successfully' AS result;
