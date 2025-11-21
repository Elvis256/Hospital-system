#!/bin/bash
# Setup Payara JDBC Connection Pools for HMIS
# Uses existing database with user 'dan' and password 'Mun00nDa5#'

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

PAYARA_HOME="/home/elvis/payara5"
DB_USER="dan"
DB_PASS="Mun00nDa5#"
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="hmis"
DB_AUDIT_NAME="hmis_audit"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}HMIS JDBC Setup for Payara${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Check if Payara is running
if ! "$PAYARA_HOME/bin/asadmin" list-domains --domaindir "$PAYARA_HOME/glassfish/domains" 2>/dev/null | grep -q "domain1 running"; then
    echo -e "${YELLOW}Starting Payara...${NC}"
    "$PAYARA_HOME/bin/asadmin" start-domain --domaindir "$PAYARA_HOME/glassfish/domains" domain1
    sleep 5
fi

echo -e "${GREEN}Creating JDBC Connection Pools...${NC}"
echo ""

# Delete existing pools if they exist (ignore errors)
"$PAYARA_HOME/bin/asadmin" delete-jdbc-resource jdbc/hmis 2>/dev/null || true
"$PAYARA_HOME/bin/asadmin" delete-jdbc-resource jdbc/hmisAudit 2>/dev/null || true
"$PAYARA_HOME/bin/asadmin" delete-jdbc-connection-pool hmisPool 2>/dev/null || true
"$PAYARA_HOME/bin/asadmin" delete-jdbc-connection-pool hmisAuditPool 2>/dev/null || true

echo "Creating main database connection pool (hmisPool)..."
"$PAYARA_HOME/bin/asadmin" create-jdbc-connection-pool \
  --datasourceclassname com.mysql.cj.jdbc.MysqlDataSource \
  --restype javax.sql.DataSource \
  --property "serverName=${DB_HOST}:portNumber=${DB_PORT}:databaseName=${DB_NAME}:user=${DB_USER}:password=${DB_PASS}" \
  hmisPool

echo "Creating JDBC resource (jdbc/hmis)..."
"$PAYARA_HOME/bin/asadmin" create-jdbc-resource \
  --connectionpoolid hmisPool \
  jdbc/hmis

echo "Testing main database connection..."
if "$PAYARA_HOME/bin/asadmin" ping-connection-pool hmisPool; then
    echo -e "${GREEN}✓ Main database connection successful!${NC}"
else
    echo -e "${RED}✗ Main database connection failed!${NC}"
    exit 1
fi

echo ""
echo "Creating audit database connection pool (hmisAuditPool)..."
"$PAYARA_HOME/bin/asadmin" create-jdbc-connection-pool \
  --datasourceclassname com.mysql.cj.jdbc.MysqlDataSource \
  --restype javax.sql.DataSource \
  --property "serverName=${DB_HOST}:portNumber=${DB_PORT}:databaseName=${DB_AUDIT_NAME}:user=${DB_USER}:password=${DB_PASS}" \
  hmisAuditPool

echo "Creating JDBC resource (jdbc/hmisAudit)..."
"$PAYARA_HOME/bin/asadmin" create-jdbc-resource \
  --connectionpoolid hmisAuditPool \
  jdbc/hmisAudit

echo "Testing audit database connection..."
if "$PAYARA_HOME/bin/asadmin" ping-connection-pool hmisAuditPool; then
    echo -e "${GREEN}✓ Audit database connection successful!${NC}"
else
    echo -e "${RED}✗ Audit database connection failed!${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}JDBC Setup Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "JDBC Resources created:"
echo "  - jdbc/hmis (pointing to database: $DB_NAME)"
echo "  - jdbc/hmisAudit (pointing to database: $DB_AUDIT_NAME)"
echo ""
echo "You can now deploy the HMIS application!"
echo ""
