#!/bin/bash
# Comprehensive fix for all reported issues
# Date: November 16, 2025

echo "=========================================="
echo "Starting Comprehensive System Fixes"
echo "=========================================="

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

HMIS_DIR="/home/elvis/hmis"
PAYARA_DIR="/home/elvis/payara5"

cd "$HMIS_DIR" || exit 1

echo -e "${YELLOW}Step 1: Adding missing database columns...${NC}"
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

SELECT 'Database columns added successfully' AS Status;
EOSQL

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Database columns added successfully${NC}"
else
    echo -e "${RED}✗ Failed to add database columns${NC}"
    exit 1
fi

echo -e "${YELLOW}Step 2: Building the application...${NC}"
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Build successful${NC}"

echo -e "${YELLOW}Step 3: Deploying to Payara...${NC}"
# Stop Payara
$PAYARA_DIR/bin/asadmin stop-domain domain1
sleep 5

# Deploy the WAR file
WAR_FILE="$HMIS_DIR/target/rh-3.0.0.war"
if [ -f "$WAR_FILE" ]; then
    rm -rf $PAYARA_DIR/glassfish/domains/domain1/applications/rh-3.0.0
    rm -rf $PAYARA_DIR/glassfish/domains/domain1/generated/jsp/rh-3.0.0
    cp "$WAR_FILE" $PAYARA_DIR/glassfish/domains/domain1/autodeploy/
    echo -e "${GREEN}✓ WAR file copied to autodeploy${NC}"
else
    echo -e "${RED}✗ WAR file not found${NC}"
    exit 1
fi

# Start Payara
$PAYARA_DIR/bin/asadmin start-domain domain1
sleep 10

echo -e "${YELLOW}Step 4: Verifying deployment...${NC}"
# Wait for deployment to complete
sleep 30

# Check if application is running
if curl -s http://localhost:8080/rh/faces/home.xhtml | grep -q "html"; then
    echo -e "${GREEN}✓ Application is running${NC}"
else
    echo -e "${YELLOW}⚠ Application may still be starting up${NC}"
fi

echo ""
echo "=========================================="
echo "Deployment Complete!"
echo "=========================================="
echo ""
echo "Fixed Issues:"
echo "1. ✓ Added QUEUE_NUMBER column to database"
echo "2. ✓ Added allergies column for triage"
echo "3. ✓ Added currentMedications column for triage"
echo "4. ✓ Application rebuilt and deployed"
echo ""
echo "Please test the following:"
echo "- Nurse Station: http://localhost:8080/rh/faces/nurse/nurse_station.xhtml"
echo "- Clinical Queue: http://localhost:8080/rh/faces/clinical/clinical_queue.xhtml"
echo "- OPD Visit: http://localhost:8080/rh/faces/emr/opd_visit.xhtml"
echo ""
echo "Note: You may need to clear browser cache and restart browser"
echo "=========================================="
