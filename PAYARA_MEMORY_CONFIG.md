# Payara Memory Configuration

## Current Configuration

**Heap Memory:** 3GB (3072MB)  
**Configuration File:** `/home/elvis/payara5/glassfish/domains/domain1/config/domain.xml`  
**Changed:** 2025-11-09  

## Settings

The following JVM options are configured:

```xml
<jvm-options>-Xmx3072m</jvm-options>
```

Located at two places in `domain.xml`:
- Line 236 (approximate)
- Line 463 (approximate)

## Previous Configuration

**Old Setting:** 512MB (caused deployment timeouts)  
**New Setting:** 3072MB (3GB)

## Benefits

✅ **Faster Deployment** - Application deploys in ~2-3 minutes instead of timing out  
✅ **Better Performance** - More memory for application operations  
✅ **No Timeouts** - Eliminates "Java heap space" errors during deployment  
✅ **Stable Operation** - Smoother running with large applications  

## How to Change Memory in Future

### Option 1: Manual Edit (Recommended)

1. Stop Payara:
   ```bash
   ~/payara5/bin/asadmin stop-domain domain1
   # OR force stop:
   pkill -9 -f payara
   ```

2. Edit configuration:
   ```bash
   nano /home/elvis/payara5/glassfish/domains/domain1/config/domain.xml
   ```

3. Find and change both occurrences:
   ```xml
   <jvm-options>-Xmx3072m</jvm-options>
   ```
   Change `3072m` to desired value (e.g., `4096m` for 4GB)

4. Start Payara:
   ```bash
   ~/payara5/bin/asadmin start-domain domain1
   ```

### Option 2: Using sed command

```bash
# Stop Payara
pkill -9 -f payara

# Change memory (example: 4GB)
sed -i 's/-Xmx3072m/-Xmx4096m/g' /home/elvis/payara5/glassfish/domains/domain1/config/domain.xml

# Start Payara
~/payara5/bin/asadmin start-domain domain1
```

### Option 3: Using asadmin command

```bash
~/payara5/bin/asadmin delete-jvm-options -Xmx3072m
~/payara5/bin/asadmin create-jvm-options -Xmx4096m
~/payara5/bin/asadmin restart-domain domain1
```

## Verification

Check if memory setting is applied:

```bash
ps aux | grep payara | grep Xmx | grep -v grep
```

Should show: `-Xmx3072m`

## Memory Recommendations by System RAM

| System RAM | Recommended Xmx | Safe for Other Apps |
|------------|-----------------|---------------------|
| 4GB        | 1024m - 2048m   | Yes                 |
| 8GB        | 2048m - 4096m   | Yes                 |
| 16GB       | 4096m - 8192m   | Yes                 |
| 32GB+      | 8192m - 16384m  | Yes                 |

**Current System:** 16GB RAM  
**Payara Setting:** 3GB (18.75% of RAM)  
**Status:** ✅ Optimal  

## Deployment Time Comparison

| Memory | Deployment Time | Status |
|--------|----------------|---------|
| 512MB  | Timeout (10+ min) | ❌ Failed |
| 3GB    | 2-3 minutes    | ✅ Success |

## Notes

- Changes are PERMANENT until manually changed again
- Payara must be restarted for changes to take effect
- Monitor system resources to ensure enough RAM for OS and other applications
- If memory errors persist, increase to 4GB

## Last Updated

2025-11-09 - Increased from 512MB to 3GB
