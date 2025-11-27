# 🎯 START HERE - HMIS Deployment

**Your Setup:**
- ✅ Database `hmis` and `hmis_audit` already exist
- ✅ User `dan` exists (password needs verification)
- ✅ MySQL is running
- ✅ Java 11 and Maven are installed

---

## 🚀 Quick Deployment (4 Easy Steps)

### Before You Start
Make sure you're in the project directory:
```bash
cd /home/elvis/hmis
```

---

### ✅ Step 1: Setup Database User (1 minute)

```bash
./setup-mysql-user.sh
```

**What this does:**
- Resets password for user `dan` to `Mun00nDa5#`
- Grants permissions on both databases
- Tests the connection

**Success indicator:** You'll see "✓ Connection successful!"

---

### ✅ Step 2: Install Payara Server (5 minutes)

```bash
cd ~
wget https://nexus.payara.fish/repository/payara-community/fish/payara/distributions/payara/5.2022.5/payara-5.2022.5.zip
unzip payara-5.2022.5.zip
echo 'AS_JAVA="/usr/lib/jvm/java-11-openjdk-amd64"' > ~/payara5/glassfish/config/asenv.conf
wget -P ~/payara5/glassfish/lib https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar
~/payara5/bin/asadmin start-domain domain1
```

**Success indicator:** Admin console opens at http://localhost:4848

---

### ✅ Step 3: Configure JDBC Connections (2 minutes)

```bash
cd /home/elvis/hmis
./setup-payara-jdbc.sh
```

**What this does:**
- Creates JDBC connection pools for both databases
- Creates JDBC resources (`jdbc/hmis` and `jdbc/hmisAudit`)
- Tests both connections

**Success indicator:** You'll see "✓ Main database connection successful!" and "✓ Audit database connection successful!"

---

### ✅ Step 4: Build and Deploy HMIS (5 minutes)

```bash
cd /home/elvis/hmis
./deploy-local.sh
```

**What this does:**
- Updates persistence.xml for local deployment
- Builds the application with Maven
- Deploys to Payara server
- Optionally restores original persistence.xml

**Success indicator:** "Deployment Complete!" message and URL shown

---

## 🎉 Done! Access Your Application

Open in your browser: **http://localhost:8080/rh**

### First-Time Setup Wizard
1. Fill in your institution details
2. Create a super admin user (save these credentials!)
3. Submit and login
4. Start using HMIS!

---

## 📁 Helpful Scripts Created

| Script | Purpose | When to Use |
|--------|---------|-------------|
| `setup-mysql-user.sh` | Setup database user and permissions | One-time (done in Step 1) |
| `setup-payara-jdbc.sh` | Configure JDBC pools in Payara | One-time (done in Step 3) |
| `deploy-local.sh` | Full build and deployment | First deployment or major changes |
| `redeploy.sh` | Quick redeploy after code changes | After editing code |

---

## 🔄 Daily Development Workflow

After you make code changes:

```bash
cd /home/elvis/hmis
./redeploy.sh
```

This takes ~3 minutes and redeploys your changes.

---

## 🛠️ Essential Commands

### View Server Logs (watch for errors)
```bash
tail -f ~/payara5/glassfish/domains/domain1/logs/server.log
```

### Stop Payara Server
```bash
~/payara5/bin/asadmin stop-domain domain1
```

### Start Payara Server
```bash
~/payara5/bin/asadmin start-domain domain1
```

### Check Deployed Applications
```bash
~/payara5/bin/asadmin list-applications
```

### Access Admin Console
```bash
firefox http://localhost:4848 &
```

---

## 🐛 Troubleshooting

### Problem: MySQL connection fails in Step 1
**Solution:**
```bash
# Check if MySQL is running
systemctl status mysql

# Try connecting manually
mysql -u dan -p'Mun00nDa5#' hmis -e "SELECT 1;"
```

### Problem: Payara won't start
**Solution:**
```bash
# Check if port 8080 is in use
sudo lsof -i :8080

# Check Payara logs
cat ~/payara5/glassfish/domains/domain1/logs/server.log
```

### Problem: JDBC connection test fails in Step 3
**Solution:**
```bash
# Verify MySQL user can connect
mysql -u dan -p'Mun00nDa5#' hmis -e "SHOW TABLES;"

# Check if MySQL connector JAR exists
ls -la ~/payara5/glassfish/lib/mysql-connector-j-8.0.33.jar

# Restart Payara and try again
~/payara5/bin/asadmin restart-domain domain1
cd /home/elvis/hmis
./setup-payara-jdbc.sh
```

### Problem: Application shows 404 after deployment
**Solution:**
```bash
# Check deployment status
~/payara5/bin/asadmin list-applications

# View recent logs
tail -100 ~/payara5/glassfish/domains/domain1/logs/server.log

# Try redeploying
cd /home/elvis/hmis
./redeploy.sh
```

---

## 📚 More Documentation

| File | Content |
|------|---------|
| `QUICK_START.md` | Detailed quick start with troubleshooting |
| `LOCAL_DEPLOYMENT_GUIDE.md` | Complete deployment guide |
| `DEPLOYMENT_SUMMARY.md` | Command reference and cheat sheet |
| `developer_docs/` | Development guidelines and architecture |

---

## ⚠️ Important Reminders

1. **Before committing code**, always restore `persistence.xml`:
   ```bash
   cp src/main/resources/META-INF/persistence.xml.backup src/main/resources/META-INF/persistence.xml
   ```

2. **Never commit** database credentials or hardcoded JNDI names

3. **Use `./redeploy.sh`** for quick changes during development

4. **Check logs** when something doesn't work:
   ```bash
   tail -f ~/payara5/glassfish/domains/domain1/logs/server.log
   ```

---

## 🎓 Next Steps After Deployment

1. ✅ Deploy the application (follow steps above)
2. 🏥 Complete initial institution setup
3. 👤 Create user accounts
4. 📖 Read the wiki: https://github.com/hmislk/hmis/wiki
5. 🔧 Explore features and modules
6. 💻 Start developing!

---

**Ready to start? Begin with Step 1! ⬆️**

Total time needed: **~15 minutes** ⏱️

---

*For detailed information, see `QUICK_START.md`*  
*For complete documentation, see `LOCAL_DEPLOYMENT_GUIDE.md`*
