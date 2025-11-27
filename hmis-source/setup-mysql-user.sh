#!/bin/bash
# Setup/Reset MySQL User for HMIS
# This ensures the 'dan' user has the correct password and permissions

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

DB_USER="dan"
DB_PASS="Mun00nDa5#"
DB_NAME="hmis"
DB_AUDIT_NAME="hmis_audit"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}MySQL User Setup for HMIS${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

echo -e "${YELLOW}This will reset the password for user 'dan' and grant necessary permissions.${NC}"
echo -e "${YELLOW}You'll need to enter your sudo password.${NC}"
echo ""

# Reset password and grant permissions
echo "Setting up user '$DB_USER' with password..."
sudo mysql << EOF
-- Reset password for dan user
ALTER USER 'dan'@'localhost' IDENTIFIED BY '$DB_PASS';

-- Grant all privileges on hmis database
GRANT ALL PRIVILEGES ON $DB_NAME.* TO 'dan'@'localhost';

-- Grant all privileges on hmis_audit database
GRANT ALL PRIVILEGES ON $DB_AUDIT_NAME.* TO 'dan'@'localhost';

-- Flush privileges
FLUSH PRIVILEGES;
EOF

echo ""
echo "Testing connection..."
if mysql -u "$DB_USER" -p"$DB_PASS" -e "SELECT 'Connection successful!' AS status;" 2>/dev/null; then
    echo -e "${GREEN}✓ Connection test successful!${NC}"
    echo ""
    echo "Checking database access..."
    mysql -u "$DB_USER" -p"$DB_PASS" << EOF
SELECT CONCAT('✓ Database: ', SCHEMA_NAME) AS Info FROM information_schema.SCHEMATA WHERE SCHEMA_NAME IN ('$DB_NAME', '$DB_AUDIT_NAME');
EOF
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Setup Complete!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "User: $DB_USER"
    echo "Password: $DB_PASS"
    echo "Databases: $DB_NAME, $DB_AUDIT_NAME"
    echo ""
    echo -e "${GREEN}You can now proceed to setup Payara JDBC!${NC}"
    echo ""
else
    echo -e "${RED}✗ Connection test failed!${NC}"
    echo "Please check the error above and try again."
    exit 1
fi
