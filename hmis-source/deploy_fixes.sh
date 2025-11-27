#!/bin/bash

# HMIS - Deploy bug fixes for nurse station and clinical queue
# Date: 2025-11-16

set -e

echo "================================"
echo "HMIS Bug Fixes Deployment"
echo "================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Database credentials
DB_NAME="hmis"
DB_USER="dan"
DB_PASS="Mun00nDa5#"

echo -e "${YELLOW}Step 1: Updating database schema...${NC}"
mysql -u $DB_USER -p"$DB_PASS" $DB_NAME < /home/elvis/hmis/fix_patientencounter_columns.sql
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Database schema updated successfully${NC}"
else
    echo -e "${RED}✗ Database schema update failed${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 2: Adding Nurse Triage privileges...${NC}"
mysql -u $DB_USER -p"$DB_PASS" $DB_NAME < /home/elvis/hmis/add_nurse_triage_privilege.sql
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Privileges added successfully${NC}"
else
    echo -e "${RED}✗ Privilege addition failed${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 3: Building application...${NC}"
cd /home/elvis/hmis
mvn clean install -DskipTests
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Build completed successfully${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 4: Deploying to Payara...${NC}"
/home/elvis/payara5/bin/asadmin undeploy rh 2>/dev/null || true
/home/elvis/payara5/bin/asadmin deploy --force=true --contextroot=rh /home/elvis/hmis/target/rh.war
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Deployment completed successfully${NC}"
else
    echo -e "${RED}✗ Deployment failed${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}Deployment Complete!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo "Please clear your browser cache and restart your session"
echo "Application URL: http://localhost:8080/rh"
