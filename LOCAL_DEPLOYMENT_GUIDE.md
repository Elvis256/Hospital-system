# HMIS Local Deployment Guide

## Your Current Environment ✅
- **OS**: Linux Ubuntu
- **Java**: OpenJDK 11.0.28 ✅
- **Maven**: 3.8.7 ✅
- **MySQL**: Running ✅
- **Project Path**: /home/elvis/hmis

## Quick Start (5 Steps to Deploy)

### Step 1: Install Payara Server 5.2022.5

```bash
# Download Payara 5.2022.5
cd ~
wget https://nexus.payara.fish/repository/payara-community/fish/payara/distributions/payara/5.2022.5/payara-5.2022.5.zip

# Extract
unzip payara-5.2022.5.zip

# Configure Java path
echo 'AS_JAVA="/usr/lib/jvm/java-11-openjdk-amd64"' > ~/payara5/glassfish/config/asenv.conf

# Start Payara
~/payara5/bin/asadmin start-domain domain1
```

**Verify**: Open http://localhost:4848 in your browser (admin console)

### Step 2: Setup Database

```bash
# Login to MySQL as root
sudo mysql

# Then run these SQL commands:
CREATE DATABASE hmis CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE hmis_audit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'hmis'@'localhost' IDENTIFIED BY 'hmis123';
GRANT ALL PRIVILEGES ON hmis.* TO 'hmis'@'localhost';
GRANT ALL PRIVILEGES ON hmis_audit.* TO 'hmis'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

**Test Connection**:
```bash
mysql -u hmis -phmis123 hmis -e "SELECT 'Connection successful' AS status;"
```

### Step 3: Configure JDBC in Payara

```bash
# Download MySQL Connector
cd ~/payara5/glassfish/lib
wget https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar

# Restart Payara
~/payara5/bin/asadmin stop-domain domain1
~/payara5/bin/asadmin start-domain domain1
```

**Create JDBC Connection Pool** (via Admin Console http://localhost:4848):

1. Go to **Resources > JDBC > JDBC Connection Pools**
2. Click **New** with these settings:
   - **Pool Name**: `hmisPool`
   - **Resource Type**: `javax.sql.DataSource`
   - **Database Driver Vendor**: `MySQL`
   - Click **Next**
   
3. Scroll to **Additional Properties** and add:
   - `serverName`: `localhost`
   - `portNumber`: `3306`
   - `databaseName`: `hmis`
   - `user`: `hmis`
   - `password`: `hmis123`
   - `URL`: `jdbc:mysql://localhost:3306/hmis?zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true`
   
4. Click **Finish**, then **Ping** to test

**Create JDBC Resource**:
1. Go to **Resources > JDBC > JDBC Resources**
2. Click **New**:
   - **JNDI Name**: `jdbc/hmis`
   - **Pool Name**: `hmisPool`
   - Click **OK**

**Repeat for Audit Database**:
- Create pool `hmisAuditPool` pointing to `hmis_audit` database
- Create resource `jdbc/hmisAudit` using `hmisAuditPool`

**OR use command line**:

```bash
# Create main database connection pool
~/payara5/bin/asadmin create-jdbc-connection-pool \
  --datasourceclassname com.mysql.cj.jdbc.MysqlDataSource \
  --restype javax.sql.DataSource \
  --property "serverName=localhost:portNumber=3306:databaseName=hmis:user=hmis:password=hmis123:URL=jdbc\:mysql\://localhost\:3306/hmis?zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true" \
  hmisPool

# Create JNDI resource for main database
~/payara5/bin/asadmin create-jdbc-resource \
  --connectionpoolid hmisPool \
  jdbc/hmis

# Test the connection
~/payara5/bin/asadmin ping-connection-pool hmisPool

# Create audit database connection pool
~/payara5/bin/asadmin create-jdbc-connection-pool \
  --datasourceclassname com.mysql.cj.jdbc.MysqlDataSource \
  --restype javax.sql.DataSource \
  --property "serverName=localhost:portNumber=3306:databaseName=hmis_audit:user=hmis:password=hmis123:URL=jdbc\:mysql\://localhost\:3306/hmis_audit?zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true" \
  hmisAuditPool

# Create JNDI resource for audit database
~/payara5/bin/asadmin create-jdbc-resource \
  --connectionpoolid hmisAuditPool \
  jdbc/hmisAudit

# Test the audit connection
~/payara5/bin/asadmin ping-connection-pool hmisAuditPool
```

### Step 4: Update persistence.xml for Local Development

```bash
cd /home/elvis/hmis

# Backup original persistence.xml
cp src/main/resources/META-INF/persistence.xml src/main/resources/META-INF/persistence.xml.backup

# Create local version (temporary for build)
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
```

**IMPORTANT**: Before committing any changes, restore the original:
```bash
# When ready to commit/push, restore the original
cp src/main/resources/META-INF/persistence.xml.backup src/main/resources/META-INF/persistence.xml
```

### Step 5: Build and Deploy

```bash
cd /home/elvis/hmis

# Build the project (this takes 3-5 minutes)
mvn clean package -DskipTests

# Deploy to Payara
~/payara5/bin/asadmin deploy --force target/rh-3.0.0.war
```

## Access the Application

**Main URL**: http://localhost:8080/rh

1. On first run, you'll see the **Initial Setup** screen
2. Create your first institution and super admin user
3. Login with the credentials you just created

## Common Commands

### Managing Payara Server
```bash
# Start server
~/payara5/bin/asadmin start-domain domain1

# Stop server
~/payara5/bin/asadmin stop-domain domain1

# Restart server
~/payara5/bin/asadmin restart-domain domain1

# View server log
tail -f ~/payara5/glassfish/domains/domain1/logs/server.log
```

### Redeployment After Code Changes
```bash
cd /home/elvis/hmis

# Rebuild
mvn clean package -DskipTests

# Redeploy
~/payara5/bin/asadmin deploy --force target/rh-3.0.0.war
```

### Undeploy Application
```bash
~/payara5/bin/asadmin undeploy rh
```

### View Deployed Applications
```bash
~/payara5/bin/asadmin list-applications
```

## Troubleshooting

### Issue: Port 8080 Already in Use
```bash
# Find what's using port 8080
sudo lsof -i :8080

# Kill the process (replace PID)
sudo kill -9 [PID]

# Or change Payara's HTTP port
~/payara5/bin/asadmin set server-config.network-config.network-listeners.network-listener.http-listener-1.port=8081
```

### Issue: Database Connection Failed
```bash
# Test MySQL connection
mysql -u hmis -phmis123 hmis -e "SELECT 1;"

# Check JDBC pool in Payara
~/payara5/bin/asadmin ping-connection-pool hmisPool

# View Payara logs for errors
tail -100 ~/payara5/glassfish/domains/domain1/logs/server.log
```

### Issue: Application Not Starting (404)
```bash
# Check deployment status
~/payara5/bin/asadmin list-applications

# View detailed server logs
tail -200 ~/payara5/glassfish/domains/domain1/logs/server.log

# Undeploy and redeploy
~/payara5/bin/asadmin undeploy rh
~/payara5/bin/asadmin deploy target/rh-3.0.0.war
```

### Issue: Out of Memory
```bash
# Edit domain.xml to increase heap size
# File: ~/payara5/glassfish/domains/domain1/config/domain.xml
# Find: <jvm-options>-Xmx512m</jvm-options>
# Change to: <jvm-options>-Xmx2048m</jvm-options>

# Or use asadmin
~/payara5/bin/asadmin delete-jvm-options -Xmx512m
~/payara5/bin/asadmin create-jvm-options -Xmx2048m
~/payara5/bin/asadmin restart-domain domain1
```

## Development Workflow

### Making Code Changes

1. **Edit code** in your IDE
2. **Build**: `mvn clean package -DskipTests`
3. **Deploy**: `~/payara5/bin/asadmin deploy --force target/rh-3.0.0.war`
4. **Test**: Open http://localhost:8080/rh in browser
5. **View logs**: `tail -f ~/payara5/glassfish/domains/domain1/logs/server.log`

### Running Tests
```bash
# Run all tests
./detect-maven.sh test

# Run specific tests
mvn test -Dtest="*BigDecimal*Test"
```

### Database Migrations
The application uses JPA to auto-create tables on first run. For subsequent changes:
- Tables are created automatically by EclipseLink
- Check `~/payara5/glassfish/domains/domain1/logs/server.log` for DDL statements

## Quick Deployment Script

Create a helper script:

```bash
cat > ~/redeploy-hmis.sh << 'EOF'
#!/bin/bash
cd /home/elvis/hmis
echo "Building application..."
mvn clean package -DskipTests
if [ $? -eq 0 ]; then
    echo "Deploying to Payara..."
    ~/payara5/bin/asadmin deploy --force target/rh-3.0.0.war
    echo "Deployment complete! Access: http://localhost:8080/rh"
else
    echo "Build failed!"
    exit 1
fi
EOF

chmod +x ~/redeploy-hmis.sh
```

**Usage**: `~/redeploy-hmis.sh`

## Next Steps

1. ✅ **Setup Complete**: You can now run the application
2. 📚 **Read Documentation**: Check `/home/elvis/hmis/docs/` for user guides
3. 🔧 **Configure Features**: Use the admin interface to set up modules
4. 🧪 **Test Workflows**: Try pharmacy, lab, billing modules
5. 📖 **Read Wiki**: https://github.com/hmislk/hmis/wiki

## Important Notes

⚠️ **Before Committing Code**:
- Restore `persistence.xml` to use `${JDBC_DATASOURCE}` variables
- Run `./scripts/restore-local-jndi.sh` if it exists
- Never commit database credentials

⚠️ **Persistence Configuration**:
- Local development uses: `jdbc/hmis` and `jdbc/hmisAudit`
- Production/QA uses: `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}` placeholders
- GitHub Actions replaces these during deployment

## Support

- **Issues**: https://github.com/hmislk/hmis/issues
- **Wiki**: https://github.com/hmislk/hmis/wiki
- **Documentation**: `/home/elvis/hmis/developer_docs/`

---
*Generated for: elvis@HP-ProBook-440-14-inch-G10-Notebook-PC*
*Date: November 8, 2025*
