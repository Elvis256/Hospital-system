-- Add QUEUE_NUMBER column to PatientEncounter table
-- This fixes the error: Unknown column 'QUEUENUMBER' in 'field list'

ALTER TABLE patientencounter ADD COLUMN IF NOT EXISTS QUEUE_NUMBER INT DEFAULT NULL;

-- Add index for better performance
CREATE INDEX IF NOT EXISTS idx_queue_number ON patientencounter(QUEUE_NUMBER);

SELECT 'QUEUE_NUMBER column added successfully' AS Status;
