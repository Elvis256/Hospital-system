# 🎉 HMIS Deployment Complete!

## ✅ Deployment Status

**SUCCESS!** The HMIS application has been successfully deployed to Payara Server 5.

### What Was Done:

1. ✅ **MySQL User Setup** - Configured database user 'dan' with correct credentials
2. ✅ **Payara 5 Installation** - Installed Payara Server 5.2022.3 (replaced Payara 6)
3. ✅ **MySQL Connector** - Downloaded and installed MySQL JDBC driver
4. ✅ **JDBC Pools Created** - Set up connection pools for both databases
5. ✅ **JDBC Resources Created** - Configured `jdbc/hmis` and `jdbc/hmisAudit`
6. ✅ **Application Built** - Successfully compiled WAR file
7. ✅ **Application Deployed** - Deployed `rh-3.0.0` to Payara

---

## 🌐 Access Your Application

**Application URL**: **http://localhost:8080/rh-3.0.0/**

### Alternative URLs to try:
- http://localhost:8080/rh-3.0.0/faces/index.xhtml
- http://localhost:8080/rh-3.0.0/faces/index1.xhtml

### Using Firefox:
```bash
firefox http://localhost:8080/rh-3.0.0/ &
```

### Using Chrome/Chromium:
```bash
google-chrome http://localhost:8080/rh-3.0.0/ &
# or
chromium-browser http://localhost:8080/rh-3.0.0/ &
```

---

## 📋 Important Information

### Payara Server Details
- **Version**: Payara Server 5.2022.3
- **Location**: `/opt/payara`
- **Domain**: domain1
- **HTTP Port**: 8080
- **Admin Port**: 4848
- **Admin Console**: http://localhost:4848

### Database Configuration
- **Main Database**: `hmis`
- **Audit Database**: `hmis_audit`
- **User**: `dan`
- **Password**: `Mun00nDa5#`
- **JNDI Resources**: `jdbc/hmis` and `jdbc/hmisAudit`

### Application Details
- **Deployed Name**: `rh-3.0.0`
- **Context Root**: `/rh-3.0.0`
- **WAR File**: `/home/elvis/hmis/target/rh-3.0.0.war`

---

## 🛠️ Common Commands

### Server Management
```bash
# Start Payara
sudo /opt/payara/bin/asadmin start-domain --domaindir /opt/payara/glassfish/domains domain1

# Stop Payara
sudo /opt/payara/bin/asadmin stop-domain domain1

# Restart Payara
sudo /opt/payara/bin/asadmin restart-domain domain1

# Check server status
sudo /opt/payara/bin/asadmin list-domains --domaindir /opt/payara/glassfish/domains
```

### Application Management
```bash
# List deployed applications
sudo /opt/payara/bin/asadmin list-applications

# Undeploy application
sudo /opt/payara/bin/asadmin undeploy rh-3.0.0

# Redeploy application
cd /home/elvis/hmis
sudo /opt/payara/bin/asadmin deploy --force target/rh-3.0.0.war
```

### View Logs
```bash
# View server logs in real-time
sudo tail -f /opt/payara/glassfish/domains/domain1/logs/server.log

# View recent errors
sudo tail -200 /opt/payara/glassfish/domains/domain1/logs/server.log | grep -i error

# View application deployment messages
sudo grep "rh-3.0.0" /opt/payara/glassfish/domains/domain1/logs/server.log | tail -50
```

### JDBC Management
```bash
# Test database connections
sudo /opt/payara/bin/asadmin ping-connection-pool hmisPool
sudo /opt/payara/bin/asadmin ping-connection-pool hmisAuditPool

# List JDBC resources
sudo /opt/payara/bin/asadmin list-jdbc-resources

# List JDBC pools
sudo /opt/payara/bin/asadmin list-jdbc-connection-pools
```

---

## 🔄 Development Workflow

### After Making Code Changes

Option 1: **Use the redeploy script** (Quick method)
```bash
cd /home/elvis/hmis
./redeploy.sh
```

Option 2: **Manual redeploy**
```bash
cd /home/elvis/hmis

# Build
mvn clean package -DskipTests

# Deploy
sudo /opt/payara/bin/asadmin deploy --force target/rh-3.0.0.war
```

### Running Tests
```bash
cd /home/elvis/hmis

# Run all tests
./detect-maven.sh test

# Run specific tests
mvn test -Dtest="*YourTest*"
```

---

## 🎯 First-Time Setup

When you first access the application:

1. **Open** http://localhost:8080/rh-3.0.0/ in your browser

2. **Initial Setup Wizard** will appear:
   - Fill in your institution details
   - Create a super admin user
   - **Important**: Remember these credentials!

3. **Login** with the admin credentials you just created

4. **Start using** the system!

---

## 🐛 Troubleshooting

### Application Not Loading?

1. **Check if Payara is running**:
   ```bash
   sudo /opt/payara/bin/asadmin list-domains --domaindir /opt/payara/glassfish/domains
   ```

2. **Check application deployment**:
   ```bash
   sudo /opt/payara/bin/asadmin list-applications
   ```

3. **Check server logs**:
   ```bash
   sudo tail -100 /opt/payara/glassfish/domains/domain1/logs/server.log
   ```

4. **Restart Payara**:
   ```bash
   sudo /opt/payara/bin/asadmin restart-domain domain1
   ```

### Database Connection Errors?

1. **Test MySQL connection**:
   ```bash
   mysql -u dan -p'Mun00nDa5#' hmis -e "SELECT 'Connected!' AS status;"
   ```

2. **Test JDBC pools**:
   ```bash
   sudo /opt/payara/bin/asadmin ping-connection-pool hmisPool
   sudo /opt/payara/bin/asadmin ping-connection-pool hmisAuditPool
   ```

### Port Already in Use?

```bash
# Check what's using port 8080
sudo lsof -i :8080

# Check what's using port 4848
sudo lsof -i :4848
```

### Application Errors?

```bash
# View recent errors
sudo tail -200 /opt/payara/glassfish/domains/domain1/logs/server.log | grep -iE "error|exception|severe"

# View application initialization
sudo grep "rh-3.0.0" /opt/payara/glassfish/domains/domain1/logs/server.log | tail -100
```

---

## 📚 Additional Resources

### Project Documentation
- **Quick Start**: `QUICK_START.md`
- **Full Guide**: `LOCAL_DEPLOYMENT_GUIDE.md`
- **Summary**: `DEPLOYMENT_SUMMARY.md`
- **Developer Docs**: `/home/elvis/hmis/developer_docs/`

### Online Resources
- **GitHub**: https://github.com/hmislk/hmis
- **Wiki**: https://github.com/hmislk/hmis/wiki
- **Issues**: https://github.com/hmislk/hmis/issues

---

## ⚠️ Important Notes

### Before Committing Code

Always restore `persistence.xml` to use environment variables:

```bash
# Check current state
grep '<jta-data-source>' /home/elvis/hmis/src/main/resources/META-INF/persistence.xml

# Should show placeholders (for commits):
# ${JDBC_DATASOURCE} and ${JDBC_AUDIT_DATASOURCE}

# If showing hardcoded values, restore backup:
cp /home/elvis/hmis/src/main/resources/META-INF/persistence.xml.backup \
   /home/elvis/hmis/src/main/resources/META-INF/persistence.xml
```

### Payara 6 Backup

The original Payara 6 installation has been backed up to:
```
/opt/payara6-backup
```

If you ever need to switch back, contact your system administrator.

---

## 🎓 Next Steps

1. ✅ **Access the application** - Open http://localhost:8080/rh-3.0.0/
2. 🏥 **Complete initial setup** - Create institution and admin user
3. 🔍 **Explore features** - Navigate through modules
4. 📖 **Read documentation** - Check the wiki and developer docs
5. 💻 **Start developing** - Make changes and use `./redeploy.sh`

---

## 🎉 Congratulations!

You have successfully deployed HMIS locally! The application is ready to use.

**Time to deployment**: ~30 minutes  
**Date completed**: November 8, 2025

---

*For questions or issues, check the documentation or create an issue on GitHub.*
