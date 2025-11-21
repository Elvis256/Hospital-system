# HMIS Deployment Summary

## ✅ What I've Created for You

I've set up everything you need to deploy and run the HMIS project locally. Here's what's ready:

### 📄 Documentation
1. **LOCAL_DEPLOYMENT_GUIDE.md** - Complete step-by-step deployment guide
2. **This file** - Quick reference summary

### 🔧 Deployment Scripts
1. **deploy-local.sh** - Full automated deployment script
2. **redeploy.sh** - Quick redeploy after code changes

### ✅ Your System Status
- ✅ Java 11 (OpenJDK 11.0.28)
- ✅ Maven 3.8.7
- ✅ MySQL Server (running)
- ✅ All prerequisites met!

---

## 🚀 Quick Start (Choose Your Path)

### Option A: Automated Deployment (Recommended)

```bash
# 1. Install Payara Server (one-time setup)
cd ~
wget https://nexus.payara.fish/repository/payara-community/fish/payara/distributions/payara/5.2022.5/payara-5.2022.5.zip
unzip payara-5.2022.5.zip
echo 'AS_JAVA="/usr/lib/jvm/java-11-openjdk-amd64"' > ~/payara5/glassfish/config/asenv.conf

# 2. Download MySQL connector (one-time setup)
wget -P ~/payara5/glassfish/lib https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar

# 3. Setup JDBC pools (one-time setup)
~/payara5/bin/asadmin start-domain domain1

# Main database pool
~/payara5/bin/asadmin create-jdbc-connection-pool \
  --datasourceclassname com.mysql.cj.jdbc.MysqlDataSource \
  --restype javax.sql.DataSource \
  --property "serverName=localhost:portNumber=3306:databaseName=hmis:user=hmis:password=hmis123:URL=jdbc\:mysql\://localhost\:3306/hmis?zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true" \
  hmisPool

~/payara5/bin/asadmin create-jdbc-resource --connectionpoolid hmisPool jdbc/hmis

# Audit database pool
~/payara5/bin/asadmin create-jdbc-connection-pool \
  --datasourceclassname com.mysql.cj.jdbc.MysqlDataSource \
  --restype javax.sql.DataSource \
  --property "serverName=localhost:portNumber=3306:databaseName=hmis_audit:user=hmis:password=hmis123:URL=jdbc\:mysql\://localhost\:3306/hmis_audit?zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true" \
  hmisAuditPool

~/payara5/bin/asadmin create-jdbc-resource --connectionpoolid hmisAuditPool jdbc/hmisAudit

# 4. Run the automated deployment
cd /home/elvis/hmis
./deploy-local.sh
```

### Option B: Manual Deployment

Follow the detailed steps in `LOCAL_DEPLOYMENT_GUIDE.md`

---

## 📋 What Happens During Deployment

1. **Checks Prerequisites** - Verifies Payara, MySQL are installed and running
2. **Creates Databases** - Sets up `hmis` and `hmis_audit` databases
3. **Updates Configuration** - Modifies persistence.xml for local JNDI names
4. **Builds Application** - Compiles using Maven (takes 3-5 minutes)
5. **Deploys to Payara** - Installs the WAR file
6. **Restores Config** - Returns persistence.xml to original state

---

## 🎯 After Successful Deployment

### Access the Application
Open your browser: **http://localhost:8080/rh**

### First-Time Setup
1. You'll see an **Initial Setup** screen
2. Create your institution details
3. Create a super admin user
4. Login and start using HMIS!

---

## 🔄 Daily Development Workflow

### After Making Code Changes

```bash
# Quick redeploy (uses current persistence.xml)
cd /home/elvis/hmis
./redeploy.sh
```

### Manual Redeploy
```bash
cd /home/elvis/hmis

# Build
mvn clean package -DskipTests

# Deploy
~/payara5/bin/asadmin deploy --force target/rh-3.0.0.war
```

---

## 🛠️ Useful Commands

### Payara Server Management
```bash
# Start server
~/payara5/bin/asadmin start-domain domain1

# Stop server
~/payara5/bin/asadmin stop-domain domain1

# Restart server
~/payara5/bin/asadmin restart-domain domain1

# View server logs (in real-time)
tail -f ~/payara5/glassfish/domains/domain1/logs/server.log

# Admin console
firefox http://localhost:4848 &
```

### Application Management
```bash
# List deployed applications
~/payara5/bin/asadmin list-applications

# Undeploy
~/payara5/bin/asadmin undeploy rh

# Deploy
~/payara5/bin/asadmin deploy target/rh-3.0.0.war

# Force redeploy
~/payara5/bin/asadmin deploy --force target/rh-3.0.0.war
```

### Database Management
```bash
# Login to MySQL
mysql -u hmis -phmis123 hmis

# Test connection
mysql -u hmis -phmis123 -e "SELECT 'Connected!' AS status;"

# Backup database
mysqldump -u hmis -phmis123 hmis > backup_$(date +%Y%m%d).sql

# Restore database
mysql -u hmis -phmis123 hmis < backup_20251108.sql
```

### JDBC Pool Testing
```bash
# Test main database connection
~/payara5/bin/asadmin ping-connection-pool hmisPool

# Test audit database connection
~/payara5/bin/asadmin ping-connection-pool hmisAuditPool

# List all JDBC resources
~/payara5/bin/asadmin list-jdbc-resources
```

---

## 🐛 Troubleshooting

### Application Returns 404
```bash
# Check if deployed
~/payara5/bin/asadmin list-applications

# Check server logs
tail -200 ~/payara5/glassfish/domains/domain1/logs/server.log

# Redeploy
~/payara5/bin/asadmin undeploy rh
~/payara5/bin/asadmin deploy target/rh-3.0.0.war
```

### Database Connection Errors
```bash
# Test MySQL
mysql -u hmis -phmis123 hmis -e "SELECT 1;"

# Test JDBC pools
~/payara5/bin/asadmin ping-connection-pool hmisPool
~/payara5/bin/asadmin ping-connection-pool hmisAuditPool

# Check logs for SQL errors
tail -100 ~/payara5/glassfish/domains/domain1/logs/server.log | grep -i sql
```

### Port 8080 Already in Use
```bash
# Find process using port 8080
sudo lsof -i :8080

# Kill the process (if needed)
sudo kill -9 [PID]

# Or change Payara port
~/payara5/bin/asadmin set server-config.network-config.network-listeners.network-listener.http-listener-1.port=8081
```

### Build Failures
```bash
# Clean Maven cache
mvn clean

# Rebuild with debug output
mvn clean package -DskipTests -X

# Check Java version
java -version  # Should be 11.x
```

---

## ⚠️ Important Notes

### Before Committing Code
Always restore `persistence.xml` to use environment variables:

```bash
# Check current persistence.xml
grep '<jta-data-source>' src/main/resources/META-INF/persistence.xml

# Should show: ${JDBC_DATASOURCE} and ${JDBC_AUDIT_DATASOURCE}
# NOT: jdbc/hmis or jdbc/hmisAudit

# If showing hardcoded values, restore:
cp src/main/resources/META-INF/persistence.xml.backup src/main/resources/META-INF/persistence.xml
```

### Local vs Production Configuration
- **Local**: Uses `jdbc/hmis` and `jdbc/hmisAudit`
- **Production/QA**: Uses `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}` placeholders
- CI/CD replaces placeholders during deployment

### Never Commit:
- Database credentials
- Hardcoded JNDI datasource names
- Local file paths in persistence.xml

---

## 📚 Additional Resources

### Project Documentation
- `/home/elvis/hmis/README.md` - Project overview
- `/home/elvis/hmis/developer_docs/` - Developer documentation
- `/home/elvis/hmis/docs/wiki/` - User guides

### Online Resources
- **GitHub**: https://github.com/hmislk/hmis
- **Issues**: https://github.com/hmislk/hmis/issues
- **Wiki**: https://github.com/hmislk/hmis/wiki

### Developer Docs
- **Installation**: `developer_docs/INSTALL_GUIDE.md`
- **QA Deployment**: `developer_docs/deployment/qa-deployment-guide.md`
- **UI Guidelines**: `developer_docs/ui/comprehensive-ui-guidelines.md`
- **Privilege System**: `developer_docs/security/privilege-system.md`

---

## 🎓 Learning Path

1. ✅ **Deploy locally** (you're here!)
2. 🔧 **Explore features** - Login and navigate the system
3. 📖 **Read documentation** - Understand the architecture
4. 💻 **Make changes** - Start with small UI improvements
5. 🧪 **Test changes** - Use `./redeploy.sh`
6. 📝 **Follow guidelines** - Check `AGENTS.md` and `developer_docs/`
7. 🚀 **Contribute** - Submit pull requests

---

## ✅ Deployment Checklist

- [ ] Install Payara 5.2022.5
- [ ] Configure Java path in asenv.conf
- [ ] Start Payara server
- [ ] Download MySQL connector JAR
- [ ] Create databases (hmis, hmis_audit)
- [ ] Create JDBC connection pools
- [ ] Create JDBC resources
- [ ] Test JDBC connections
- [ ] Update persistence.xml (for build)
- [ ] Build project with Maven
- [ ] Deploy to Payara
- [ ] Restore persistence.xml (for git)
- [ ] Access http://localhost:8080/rh
- [ ] Complete initial setup
- [ ] Login and verify

---

## 🆘 Getting Help

If you encounter issues:

1. **Check logs**: `tail -f ~/payara5/glassfish/domains/domain1/logs/server.log`
2. **Search documentation**: Look in `/home/elvis/hmis/developer_docs/`
3. **Check GitHub issues**: https://github.com/hmislk/hmis/issues
4. **Review troubleshooting section** in LOCAL_DEPLOYMENT_GUIDE.md

---

**Ready to deploy? Run:**
```bash
cd /home/elvis/hmis
./deploy-local.sh
```

Good luck! 🚀
