#!/bin/bash

# Build and Deploy HMIS Application
# This script builds the HMIS source and deploys to Payara

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SOURCE_DIR="$SCRIPT_DIR/hmis-source"
PAYARA_DIR="$SCRIPT_DIR/payara5"
AUTODEPLOY_DIR="$PAYARA_DIR/glassfish/domains/domain1/autodeploy"

echo "=========================================="
echo "HMIS Build and Deploy Script"
echo "=========================================="
echo ""

# Check if source directory exists
if [ ! -d "$SOURCE_DIR" ]; then
    echo "❌ Error: Source directory not found: $SOURCE_DIR"
    exit 1
fi

# Navigate to source directory
cd "$SOURCE_DIR"

echo "📁 Working directory: $(pwd)"
echo ""

# Build with Maven
echo "🔨 Building application with Maven..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo ""
echo "✅ Build successful!"
echo ""

# Check if WAR file was created
WAR_FILE="$SOURCE_DIR/target/rh-3.0.0.war"
if [ ! -f "$WAR_FILE" ]; then
    echo "❌ Error: WAR file not found: $WAR_FILE"
    exit 1
fi

# Get WAR file size
WAR_SIZE=$(du -h "$WAR_FILE" | cut -f1)
echo "📦 WAR file size: $WAR_SIZE"
echo ""

# Copy to autodeploy directory
echo "🚀 Deploying to Payara..."
cp "$WAR_FILE" "$AUTODEPLOY_DIR/"

if [ $? -ne 0 ]; then
    echo "❌ Deployment failed!"
    exit 1
fi

echo "✅ WAR file copied to autodeploy directory"
echo ""

# Wait for deployment
echo "⏳ Waiting for deployment to complete..."
sleep 5

# Check for deployment marker
for i in {1..30}; do
    if [ -f "$AUTODEPLOY_DIR/rh-3.0.0.war_deployed" ]; then
        echo "✅ Application deployed successfully!"
        echo ""
        echo "=========================================="
        echo "🎉 DEPLOYMENT COMPLETE"
        echo "=========================================="
        echo ""
        echo "Application URL: http://localhost:8080/rh"
        echo "Fast Retail Sale: http://localhost:8080/rh/faces/pharmacy/pharmacy_fast_retail_sale.xhtml"
        echo ""
        exit 0
    fi
    sleep 2
done

echo "⚠️  Deployment marker not found after 60 seconds"
echo "   Check logs: $PAYARA_DIR/glassfish/domains/domain1/logs/server.log"
echo ""

exit 0
