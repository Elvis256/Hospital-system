#!/bin/bash

# Full HMIS Redeploy Script
# This script performs a complete rebuild and redeployment

echo "================================"
echo "HMIS Full Redeployment Script"
echo "================================"
echo ""

# Set variables
PAYARA_HOME="/home/elvis/payara5"
APP_NAME="rh-3.0.0"
WAR_FILE="target/rh-3.0.0.war"

echo "[1/6] Stopping Payara domain..."
$PAYARA_HOME/bin/asadmin stop-domain domain1 2>/dev/null
sleep 3

echo "[2/6] Cleaning and building application..."
mvn clean package -DskipTests -q
if [ $? -ne 0 ]; then
    echo "ERROR: Maven build failed!"
    exit 1
fi
echo "Build successful!"

echo "[3/6] Starting Payara domain..."
$PAYARA_HOME/bin/asadmin start-domain domain1
sleep 10

echo "[4/6] Undeploying existing application..."
$PAYARA_HOME/bin/asadmin undeploy $APP_NAME 2>/dev/null
sleep 2

echo "[5/6] Deploying new application..."
$PAYARA_HOME/bin/asadmin deploy --force=true --contextroot=/rh --name=$APP_NAME $WAR_FILE

if [ $? -eq 0 ]; then
    echo "[6/6] Deployment successful!"
    echo ""
    echo "================================"
    echo "Application URL: http://localhost:8080/rh"
    echo "================================"
else
    echo "ERROR: Deployment failed!"
    exit 1
fi

echo ""
echo "Checking deployment status..."
$PAYARA_HOME/bin/asadmin list-applications | grep $APP_NAME

echo ""
echo "Done! Please wait 30 seconds for application to fully initialize."
