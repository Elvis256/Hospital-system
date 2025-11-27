#!/bin/bash
# Quick redeploy script for HMIS
# Use this after making code changes

PAYARA_HOME="/opt/payara"
HMIS_DIR="/home/elvis/hmis"

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}Quick Redeployment Started...${NC}"

cd "$HMIS_DIR"

# Build
echo "Building..."
if mvn clean package -DskipTests -q; then
    echo -e "${GREEN}✓ Build successful${NC}"
else
    echo -e "${RED}✗ Build failed${NC}"
    exit 1
fi

# Deploy
echo "Deploying..."
if "$PAYARA_HOME/bin/asadmin" deploy --force target/rh-3.0.0.war 2>&1 | grep -q "Command deploy executed successfully"; then
    echo -e "${GREEN}✓ Deployment successful${NC}"
    echo ""
    echo "Application URL: http://localhost:8080/rh"
else
    echo -e "${RED}✗ Deployment failed${NC}"
    exit 1
fi
