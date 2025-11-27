#!/bin/bash
# HMIS Local Deployment Script
# This script automates the local deployment process

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
PAYARA_HOME="/home/elvis/payara5"
HMIS_DIR="/home/elvis/hmis"
DB_NAME="hmis"
DB_AUDIT_NAME="hmis_audit"
DB_USER="dan"
DB_PASS="Mun00nDa5#"
JNDI_NAME="jdbc/hmis"
JNDI_AUDIT_NAME="jdbc/hmisAudit"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}HMIS Local Deployment Script${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Function to print status
print_status() {
    echo -e "${GREEN}[✓]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

# Check if Payara is installed
check_payara() {
    if [ ! -d "$PAYARA_HOME" ]; then
        print_error "Payara not found at $PAYARA_HOME"
        echo "Please install Payara Server 5.2022.5 first"
        echo "See LOCAL_DEPLOYMENT_GUIDE.md for instructions"
        exit 1
    fi
    print_status "Payara found at $PAYARA_HOME"
}

# Check if MySQL is running
check_mysql() {
    if ! systemctl is-active --quiet mysql; then
        print_error "MySQL is not running"
        echo "Start MySQL with: sudo systemctl start mysql"
        exit 1
    fi
    print_status "MySQL is running"
}

# Check if databases exist
check_databases() {
    if ! mysql -u "$DB_USER" -p"$DB_PASS" -e "USE $DB_NAME" 2>/dev/null; then
        print_warning "Database '$DB_NAME' not found"
        read -p "Create database? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            sudo mysql -e "CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
            sudo mysql -e "CREATE DATABASE IF NOT EXISTS $DB_AUDIT_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
            sudo mysql -e "CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASS';"
            sudo mysql -e "GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'localhost';"
            sudo mysql -e "GRANT ALL PRIVILEGES ON $DB_AUDIT_NAME.* TO '$DB_USER'@'localhost';"
            sudo mysql -e "FLUSH PRIVILEGES;"
            print_status "Databases created successfully"
        else
            print_error "Database required. Exiting."
            exit 1
        fi
    else
        print_status "Database '$DB_NAME' exists"
    fi
}

# Start Payara if not running
start_payara() {
    if ! "$PAYARA_HOME/bin/asadmin" list-domains | grep -q "domain1 running"; then
        print_status "Starting Payara..."
        "$PAYARA_HOME/bin/asadmin" start-domain domain1
        sleep 5
    else
        print_status "Payara is already running"
    fi
}

# Update persistence.xml for local development
update_persistence() {
    print_status "Updating persistence.xml for local deployment..."
    
    cd "$HMIS_DIR"
    
    # Backup if not already backed up
    if [ ! -f "src/main/resources/META-INF/persistence.xml.backup" ]; then
        cp src/main/resources/META-INF/persistence.xml src/main/resources/META-INF/persistence.xml.backup
    fi
    
    # Replace placeholders with actual JNDI names
    cat > src/main/resources/META-INF/persistence.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<persistence version="2.2" xmlns="http://java.sun.com/xml/ns/persistence" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence http://xmlns.jcp.org/xml/ns/persistence/persistence_2_2.xsd">
    <persistence-unit name="hmisPU" transaction-type="JTA">
        <provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>
        <jta-data-source>jdbc/hmis</jta-data-source>
        <exclude-unlisted-classes>false</exclude-unlisted-classes>
        <properties>
            <property name="eclipselink.logging.level.sql" value="SEVERE"/>
            <property name="eclipselink.logging.level" value="SEVERE"/>
        </properties>
    </persistence-unit>
    <persistence-unit name="hmisAuditPU" transaction-type="JTA">
        <jta-data-source>jdbc/hmisAudit</jta-data-source>
        <exclude-unlisted-classes>false</exclude-unlisted-classes>
        <properties>
            <property name="eclipselink.logging.level.sql" value="SEVERE"/>
            <property name="eclipselink.logging.level" value="SEVERE"/>
        </properties>
    </persistence-unit>
</persistence>
EOF
    
    print_status "persistence.xml updated"
}

# Build the project
build_project() {
    print_status "Building HMIS application..."
    cd "$HMIS_DIR"
    
    if mvn clean package -DskipTests -q; then
        print_status "Build completed successfully"
    else
        print_error "Build failed"
        exit 1
    fi
}

# Deploy to Payara
deploy_application() {
    print_status "Deploying to Payara..."
    
    # Check if already deployed
    if "$PAYARA_HOME/bin/asadmin" list-applications | grep -q "rh"; then
        print_status "Undeploying existing application..."
        "$PAYARA_HOME/bin/asadmin" undeploy rh || true
    fi
    
    # Deploy new WAR
    if "$PAYARA_HOME/bin/asadmin" deploy "$HMIS_DIR/target/rh-3.0.0.war"; then
        print_status "Application deployed successfully"
    else
        print_error "Deployment failed"
        exit 1
    fi
}

# Restore original persistence.xml
restore_persistence() {
    if [ -f "$HMIS_DIR/src/main/resources/META-INF/persistence.xml.backup" ]; then
        print_warning "Restoring original persistence.xml..."
        cp "$HMIS_DIR/src/main/resources/META-INF/persistence.xml.backup" "$HMIS_DIR/src/main/resources/META-INF/persistence.xml"
        print_status "persistence.xml restored"
    fi
}

# Main deployment flow
main() {
    echo ""
    check_payara
    check_mysql
    check_databases
    start_payara
    
    echo ""
    echo -e "${GREEN}Starting deployment process...${NC}"
    echo ""
    
    update_persistence
    build_project
    deploy_application
    
    # Optional: restore persistence.xml after deployment
    read -p "Restore original persistence.xml? (recommended for git commits) (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        restore_persistence
    fi
    
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Deployment Complete!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "Access the application at: ${YELLOW}http://localhost:8080/rh${NC}"
    echo ""
    echo "Useful commands:"
    echo "  - View logs: tail -f $PAYARA_HOME/glassfish/domains/domain1/logs/server.log"
    echo "  - Admin console: http://localhost:4848"
    echo "  - Stop server: $PAYARA_HOME/bin/asadmin stop-domain domain1"
    echo ""
}

# Run main function
main
