# HMIS Quick Deployment Guide (For Your Setup)

## Your Environment
- ✅ Database: `hmis` and `hmis_audit` (already created)
- ✅ User: `dan`
- ✅ Password: `Mun00nDa5#`
- ✅ MySQL: Running
- ✅ Java 11: Installed
- ✅ Maven: Installed

---

## 🚀 Deploy in 4 Steps

### Step 0: Setup MySQL User (One-time, ~1 minute)

First, ensure the database user has the correct password and permissions:

```bash
cd /home/elvis/hmis
./setup-mysql-user.sh
```

This will:
- Reset the password for user `dan` to `Mun00nDa5#`
- Grant permissions on `hmis` and `hmis_audit` databases
- Test the connection

**Verify**: You should see "Connection successful!" message.

### Step 1: Install Payara Server (One-time, ~5 minutes)

```bash
# Download and install Payara 5.2022.5
cd ~
wget https://nexus.payara.fish/repository/payara-community/fish/payara/distributions/payara/5.2022.5/payara-5.2022.5.zip
unzip payara-5.2022.5.zip

# Configure Java path
echo 'AS_JAVA="/usr/lib/jvm/java-11-openjdk-amd64"' > ~/payara5/glassfish/config/asenv.conf

# Download MySQL connector
wget -P ~/payara5/glassfish/lib https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar

# Start Payara
~/payara5/bin/asadmin start-domain domain1
```

**Verify**: Open http://localhost:4848 (Payara admin console should load)

### Step 2: Setup JDBC Connection Pools (One-time, ~2 minutes)

```bash
cd /home/elvis/hmis
./setup-payara-jdbc.sh
```

This will:
- Create JDBC connection pools for both databases
- Test the connections
- Display success confirmation

### Step 3: Build and Deploy HMIS (~5 minutes)

```bash
cd /home/elvis/hmis
./deploy-local.sh
```

**Done!** Access your application at: **http://localhost:8080/rh**

---

## 🎯 First-Time Application Setup

1. Open http://localhost:8080/rh in your browser
2. You'll see an **Initial Setup** wizard
3. Fill in your institution details
4. Create a super admin user (remember these credentials!)
5. Click **Submit**
6. Login and start using HMIS!

---

## 🔄 After Making Code Changes

Use the quick redeploy script:

```bash
cd /home/elvis/hmis
./redeploy.sh
```

This rebuilds and redeploys in ~3 minutes.

---

## 🛠️ Useful Commands

### Payara Management
```bash
# Start server
~/payara5/bin/asadmin start-domain domain1

# Stop server
~/payara5/bin/asadmin stop-domain domain1

# View logs
tail -f ~/payara5/glassfish/domains/domain1/logs/server.log

# Admin console
firefox http://localhost:4848 &
```

### Application Management
```bash
# List deployed apps
~/payara5/bin/asadmin list-applications

# Force redeploy
~/payara5/bin/asadmin deploy --force target/rh-3.0.0.war

# Undeploy
~/payara5/bin/asadmin undeploy rh
```

### Database Access
```bash
# Login to MySQL
mysql -u dan -p'Mun00nDa5#' hmis

# Quick test
mysql -u dan -p'Mun00nDa5#' hmis -e "SHOW TABLES;"
```

---

## 🐛 Troubleshooting

### Can't Connect to Database?
```bash
# Test MySQL connection
mysql -u dan -p'Mun00nDa5#' hmis -e "SELECT 'Success!' AS status;"

# Test JDBC pools
~/payara5/bin/asadmin ping-connection-pool hmisPool
~/payara5/bin/asadmin ping-connection-pool hmisAuditPool
```

### Application Not Loading (404)?
```bash
# Check deployment status
~/payara5/bin/asadmin list-applications

# View recent logs
tail -100 ~/payara5/glassfish/domains/domain1/logs/server.log

# Redeploy
cd /home/elvis/hmis
./redeploy.sh
```

### Payara Won't Start?
```bash
# Check if port 8080 is in use
sudo lsof -i :8080

# Check Payara status
~/payara5/bin/asadmin list-domains

# View startup logs
cat ~/payara5/glassfish/domains/domain1/logs/server.log
```

---

## 📋 Complete Checklist

- [ ] Run `./setup-mysql-user.sh` to configure database user
- [ ] Download and extract Payara 5.2022.5
- [ ] Configure Java path in `asenv.conf`
- [ ] Download MySQL connector JAR
- [ ] Start Payara server
- [ ] Run `./setup-payara-jdbc.sh`
- [ ] Verify JDBC connections work
- [ ] Run `./deploy-local.sh`
- [ ] Access http://localhost:8080/rh
- [ ] Complete initial setup wizard
- [ ] Login with your admin credentials

---

## 🎓 What's Next?

1. **Explore the Application** - Navigate through different modules
2. **Read Documentation** - Check `developer_docs/` folder
3. **Make Changes** - Edit code and use `./redeploy.sh`
4. **Learn the Architecture** - Review `AGENTS.md` for guidelines
5. **Check Wiki** - Visit https://github.com/hmislk/hmis/wiki

---

## 📞 Need Help?

- **Full Documentation**: See `LOCAL_DEPLOYMENT_GUIDE.md`
- **Command Reference**: See `DEPLOYMENT_SUMMARY.md`
- **GitHub Issues**: https://github.com/hmislk/hmis/issues
- **Developer Docs**: `/home/elvis/hmis/developer_docs/`

---

**Ready? Start with Step 1!** ⬆️

Each step should take less than 10 minutes. Total setup time: **~15 minutes** 🚀
