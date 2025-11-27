#!/bin/bash
# Quick database migration script
# Run this FIRST before deploying

echo "=========================================="
echo "Running Database Migration"
echo "=========================================="

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Adding missing columns to patientencounter table...${NC}"

sudo mysql hmis <<'EOSQL'
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

-- Verify columns
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE 
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'hmis' 
AND TABLE_NAME = 'patientencounter'
AND COLUMN_NAME IN ('QUEUE_NUMBER', 'allergies', 'currentMedications')
ORDER BY COLUMN_NAME;

SELECT 'Migration completed successfully!' AS Status;
EOSQL

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Database migration completed successfully${NC}"
    echo ""
    echo "Next step: Run ./deploy_comprehensive_fix.sh to deploy the application"
else
    echo -e "${RED}✗ Migration failed${NC}"
    exit 1
fi
