# HMIS Hospital System

## Quick Start

### Build and Deploy
```bash
./build-and-deploy.sh
```

### Start Server
```bash
./start-domain.sh
```

### Stop Server
```bash
./stop.sh
```

### View Logs
```bash
./logs.sh
```

## Project Structure

```
HMISD/
├── hmis-source/        # Source code (GitHub: ITSolutionsUganda/Hospital-system)
├── payara5/            # Payara Application Server
├── build-and-deploy.sh # Build and deploy script
├── start-domain.sh     # Start server
├── stop.sh             # Stop server
└── logs.sh             # View logs
```

## Application URLs

- **Base URL**: http://localhost:8080/rh
- **Fast Retail Sale**: http://localhost:8080/rh/faces/pharmacy/pharmacy_fast_retail_sale.xhtml

## Recent Fixes

✅ **Prescription Duplicate Key Error** - Fixed on 2025-11-26
- Issue: Transaction aborted with duplicate key on PRESCRIPTION table
- Solution: Create new Prescription objects instead of reusing managed entities
- File: `src/main/java/com/divudi/bean/pharmacy/PharmacyFastRetailSaleController.java`
- Method: `saveSaleBillItems()`

## Documentation

See `/home/elvis/HMIS_PROJECT_FINAL_STRUCTURE.md` for complete documentation.
