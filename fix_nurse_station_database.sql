-- Fix all missing columns for nurse station functionality
-- Run this script to add all required columns to PatientEncounter table

USE hmis;

-- Add QUEUE_NUMBER column
ALTER TABLE patientencounter 
ADD COLUMN IF NOT EXISTS QUEUE_NUMBER INT DEFAULT NULL;

-- Add allergies column  
ALTER TABLE patientencounter 
ADD COLUMN IF NOT EXISTS allergies VARCHAR(1000) DEFAULT NULL;

-- Add currentMedications column
ALTER TABLE patientencounter 
ADD COLUMN IF NOT EXISTS currentMedications VARCHAR(1000) DEFAULT NULL;

-- Add indices for better performance
CREATE INDEX IF NOT EXISTS idx_queue_number ON patientencounter(QUEUE_NUMBER);
CREATE INDEX IF NOT EXISTS idx_triage_completed ON patientencounter(TRIAGECOMPLETED);
CREATE INDEX IF NOT EXISTS idx_encounter_type ON patientencounter(ENCOUNTERTYPE);

-- Verify columns were added
SELECT 'Database migration completed successfully' AS Status;

-- Show all triage-related columns
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'hmis' 
AND TABLE_NAME = 'patientencounter'
AND (COLUMN_NAME LIKE '%triage%' 
     OR COLUMN_NAME LIKE '%queue%' 
     OR COLUMN_NAME = 'allergies' 
     OR COLUMN_NAME = 'currentMedications')
ORDER BY COLUMN_NAME;
