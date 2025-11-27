# HMIS Bug Fixes - November 16, 2025

## Issues Fixed

### 1. Clinical Queue - Doctor Filtering Issue
**Problem:** Dr. Moses was seeing Dr. Coleta's patients in the queue.

**Root Cause:** The `getDoctor()` method was caching the doctor without validating if the cached doctor matches the currently logged-in user.

**Solution:** Modified `PracticeBookingController.getDoctor()` to:
- Always validate that the cached doctor matches the logged-in user's staff record
- Refresh the doctor automatically if there's a mismatch
- This ensures each doctor only sees their own queue

**Files Changed:**
- `src/main/java/com/divudi/bean/clinical/PracticeBookingController.java`

---

### 2. Nurse Station - Database Column Errors
**Problem:** Multiple errors about missing columns: `QUEUENUMBER`, `TEMPERATURE`, `ALLERGIES`, etc.

**Root Cause:** The PatientEncounter entity has these fields defined, but they may not exist in the database.

**Solution:** Created SQL migration script (`fix_patientencounter_columns.sql`) that:
- Adds `QUEUENUMBER` column for queue management
- Ensures `TEMPERATURE` column exists for vital signs
- Adds `ALLERGIES` column for patient allergies
- Adds `PRESENTINGCOMPLAINT` and `CHIEFCOMPLAINT` columns for triage notes

**Files Created:**
- `fix_patientencounter_columns.sql`

---

### 3. Nurse Station - Query Type Casting Error
**Problem:** Error: `class java.lang.String cannot be cast to class java.lang.Enum`

**Root Cause:** The JPQL query was comparing `encounterType` with a string `'Opd'` when it should use the enum value.

**Solution:** Fixed the query in `NurseTriageController.loadQueuedPatients()` to:
- Remove the string literal `'Opd'` 
- Use the proper enum: `com.divudi.core.data.EncounterType.Opd`
- Removed the `OR e.encounterType IS NULL` condition to avoid ambiguity

**Files Changed:**
- `src/main/java/com/divudi/bean/clinical/NurseTriageController.java`

---

### 4. Nurse Station - Navigation Menu
**Problem:** User couldn't find the nurse station icon or it didn't have a dropdown menu.

**Current Status:** The navigation menu is already properly implemented in `home.xhtml`:
- Located between OPD and Member icons
- Has dropdown menu with:
  - "Nursing Work Bench" (`/nurse/index.xhtml`)
  - "Nurse Triage Station" (`/nurse/nurse_station.xhtml`)
- Visibility controlled by privileges: `NursingWorkBench` or `NurseTriageStation`

**Solution:** Created privilege setup script to ensure privileges exist in database.

**Files Created:**
- `add_nurse_triage_privilege.sql`

---

### 5. Nurse Station - Authorization
**Problem:** "You are not Authorized to Access this Page" error when accessing nurse station.

**Root Cause:** Missing `NurseTriageStation` privilege in the database for the user.

**Solution:** Created SQL script to:
- Add `NurseTriageStation` privilege if it doesn't exist
- Grant it to Super User role automatically
- Grant it to users who already have `NursingWorkBench` privilege

**Files Created:**
- `add_nurse_triage_privilege.sql`

---

## Deployment Instructions

### Automated Deployment (Recommended)

Run the deployment script:

```bash
cd /home/elvis/hmis
./deploy_fixes.sh
```

This script will:
1. Update database schema (add missing columns)
2. Add nurse triage privileges
3. Build the application with Maven
4. Deploy to Payara server

### Manual Deployment

If you prefer manual steps:

```bash
# 1. Update database schema
sudo mysql hmis < /home/elvis/hmis/fix_patientencounter_columns.sql

# 2. Add privileges
sudo mysql hmis < /home/elvis/hmis/add_nurse_triage_privilege.sql

# 3. Build application
cd /home/elvis/hmis
mvn clean install -DskipTests

# 4. Deploy to Payara
/home/elvis/payara5/bin/asadmin undeploy rh
/home/elvis/payara5/bin/asadmin deploy --force=true --contextroot=rh /home/elvis/hmis/target/rh.war
```

### Post-Deployment Steps

1. **Clear Browser Cache:**
   - Press `Ctrl + Shift + Delete`
   - Clear cookies and cached files
   - Or use Incognito/Private browsing mode

2. **Logout and Login Again:**
   - Logout from the current session
   - Login as admin (elvis) or the nurse user
   - The nurse icon should appear on the home dashboard

3. **Grant Nurse Privileges to Users:**
   - Go to: http://localhost:8080/rh/faces/admin/users/user_privileges.xhtml
   - Select the nurse user
   - Check "Nurse Triage Station" privilege
   - Save

---

## Testing the Fixes

### Test 1: Clinical Queue Filter (Doctors)
1. Login as Dr. Moses
2. Navigate to: http://localhost:8080/rh/faces/clinical/clinical_queue.xhtml
3. Verify: Only patients queued for Dr. Moses appear
4. Logout and login as Dr. Coleta
5. Verify: Only patients queued for Dr. Coleta appear

### Test 2: Nurse Station Access
1. Login as admin (elvis)
2. Home page should show nurse icon between OPD and Member icons
3. Click the nurse icon → dropdown menu should appear with 2 options
4. Click "Nurse Triage Station"
5. Verify: Queue of patients appears

### Test 3: Triage Workflow
1. At Nurse Station, click "Start Triage" on a patient
2. Enter vital signs:
   - Weight, Height (BMI auto-calculates)
   - Blood Pressure (Systolic/Diastolic)
   - Temperature, Pulse Rate
   - Respiratory Rate, O2 Saturation
3. Enter clinical notes:
   - Chief Complaint
   - Known Allergies
   - Current Medications
4. Click "Save & Return to Queue"
5. Patient should be marked as triaged

### Test 4: Doctor Sees Triage Information
1. Login as the doctor assigned to the patient
2. Go to clinical queue and click "Visit" on a triaged patient
3. Navigate to: http://localhost:8080/rh/faces/emr/opd_visit.xhtml
4. Verify: Triage information (vitals, allergies) is visible
5. Field should indicate which nurse performed the triage

---

## Remaining Issues to Address

### 1. ICD-10 Diagnosis Integration
**Status:** Not yet implemented

The user wants diagnosis at `http://localhost:8080/rh/faces/emr/opd_visit.xhtml` to use current WHO ICD-10 codes.

**Next Steps:**
- Need to integrate ICD-10 code database
- Update diagnosis lookup to search ICD-10 codes
- Provide autocomplete for ICD-10 codes with descriptions

### 2. Receipt Printing After Payment
**Status:** Needs investigation

The user reports that after paying at `http://localhost:8080/rh/faces/opd/opd_bill_pre_settle.xhtml`, clicking print doesn't show items.

**Next Steps:**
- Investigate the print receipt functionality
- Check if bill items are being passed to the print template
- Test the print workflow after payment

### 3. View Expired Error (ViewExpiredException)
**Status:** Configuration issue

Multiple `ViewExpiredException` errors for `/admin/users/user.xhtml`

**Cause:** JSF view state expires (session timeout or navigation issues)

**Possible Solutions:**
- Increase JSF state timeout in web.xml
- Add view state saving configuration
- Implement proper navigation without breaking view state

---

## Files Created/Modified

### Created Files:
1. `/home/elvis/hmis/fix_patientencounter_columns.sql` - Database schema migration
2. `/home/elvis/hmis/add_nurse_triage_privilege.sql` - Privilege setup
3. `/home/elvis/hmis/deploy_fixes.sh` - Automated deployment script

### Modified Files:
1. `src/main/java/com/divudi/bean/clinical/PracticeBookingController.java` - Fixed doctor caching
2. `src/main/java/com/divudi/bean/clinical/NurseTriageController.java` - Fixed query type casting

---

## Notes

- The nurse station menu and pages are already well-implemented
- The main issues were related to privileges and database schema
- Doctor queue filtering now properly validates the logged-in user
- All patient queue filtering is based on the logged-in doctor's ServiceSession

---

## Support

If you encounter any issues after deployment:
1. Check server logs: `/home/elvis/payara5/glassfish/domains/domain1/logs/server.log`
2. Verify database changes: `sudo mysql hmis` then run `DESCRIBE patientencounter;`
3. Check privileges: `SELECT * FROM privilege WHERE name LIKE '%Nurse%';`
