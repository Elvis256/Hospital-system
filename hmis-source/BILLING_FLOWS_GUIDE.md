# Complete Billing Flows in HMIS

## Overview

The HMIS system has **THREE MAIN BILLING FLOWS**:

1. **OPD Billing Flow** - Outpatient services, consultations, procedures
2. **Pharmacy Billing Flow** - Medication sales and dispensing
3. **Laboratory Billing Flow** - Lab tests and investigations

---

## 🏥 BILLING FLOW 1: OPD BILLING

### Purpose
Bill for outpatient services, consultations, procedures, and non-medication items.

### Entry Points
- **Direct OPD Billing**: `/opd/view/opd_bill.xhtml`
- **From EMR**: Click 🛒 Order button → `issueServices()`
- **Package Billing**: `/opd/opd_bill_package.xhtml`

### What Can Be Billed
- Doctor consultations
- Specialist consultations
- Medical procedures
- X-rays and imaging
- Lab tests
- Minor surgeries
- Dressings
- Physiotherapy sessions
- Any OPD service

### Workflow

**Step 1: Patient Search/Selection**
```
Search by:
  • Patient name
  • Phone number (0742020610)
  • MRN number
  • PHN number
```

**Step 2: Add Services**
```
Select services:
  • Search service name
  • Add to bill
  • Specify quantity
  • Set institution/department
```

**Step 3: Calculate Total**
```
System calculates:
  • Gross total
  • Discounts (membership/payment method)
  • Net total
```

**Step 4: Payment**
```
Select payment method:
  • Cash
  • Credit Card
  • Mobile Money
  • Insurance
  • Credit Account
```

**Step 5: Print Receipt**
```
Generate:
  • Official receipt
  • Service details
  • Payment proof
```

### Database Tables
- `BILL` - Main bill record
- `BILLITEM` - Individual service items
- `BILLFEE` - Fee components
- `BILLCOMPONENT` - Bill breakdown

### Test URL
```
http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
```

---

## 💊 BILLING FLOW 2: PHARMACY BILLING

### Purpose
Sell and dispense medications to patients.

### Entry Points
- **Pharmacy Retail Sale**: `/pharmacy/pharmacy_bill_retail_sale.xhtml`
- **From EMR**: Click 💊 Pharmacy button → `issueItems()`
- **From Clinical**: `/clinical/clinical_pharmacy_sale.xhtml`

### What Can Be Billed
- Prescribed medications
- Over-the-counter drugs
- Medical supplies
- Pharmaceutical items
- IV fluids
- Injections

### Workflow

**Step 1: Patient Selection**
```
Two modes:
  A) Walk-in customer (no patient record)
  B) Registered patient (with MRN)
```

**Step 2: Add Medications**
```
For each medication:
  • Search drug name
  • Select strength/form
  • Specify quantity
  • Check stock availability
  • Add to bill
```

**Step 3: Prescription Check**
```
System validates:
  • Prescription requirements
  • Drug interactions
  • Allergies (if patient registered)
  • Dosage appropriateness
```

**Step 4: Pricing**
```
System applies:
  • Standard pricing
  • Membership discounts
  • Payment method discounts
  • Wholesale/retail rates
```

**Step 5: Payment & Dispensing**
```
• Collect payment
• Print bill
• Dispense medications
• Update stock
```

### Stock Management Integration
```
After sale:
  ✅ Stock reduced automatically
  ✅ Batch tracking
  ✅ Expiry date monitoring
  ✅ Reorder alerts if low stock
```

### Database Tables
- `BILL` - Pharmacy bill
- `BILLITEM` - Individual medicines
- `STOCK` - Inventory tracking
- `PHARMACEUTICALITEM` - Medicine master data

### Test URL
```
http://localhost:8080/rh/faces/pharmacy/pharmacy_bill_retail_sale.xhtml
http://localhost:8080/rh/faces/clinical/clinical_pharmacy_sale.xhtml
```

---

## 🔬 BILLING FLOW 3: LABORATORY BILLING

### Purpose
Bill for laboratory tests and investigations.

### Entry Points
- **OPD Bill** (as part of OPD services)
- **Direct Lab Billing**: Through OPD billing selecting lab items
- **From EMR**: Click 🛒 Order button, select lab tests

### What Can Be Billed
- Blood tests (CBC, LFT, RFT, etc.)
- Urine analysis
- Stool examination
- Microbiology cultures
- Biochemistry tests
- Hematology tests
- Serology tests
- Molecular diagnostics

### Workflow

**Step 1: Order via OPD Bill**
```
Navigate: OPD Billing page
Search patient: 0742020610
Add investigation items:
  • Complete Blood Count
  • Liver Function Test
  • etc.
```

**Step 2: Payment**
```
Patient pays at billing counter
Receipt generated with:
  • Lab request form
  • Bill details
  • Tests ordered
```

**Step 3: Sample Collection**
```
Patient goes to lab with receipt
Lab staff:
  • Verifies payment
  • Collects samples
  • Labels properly
  • Logs in LIMS
```

**Step 4: Analysis**
```
Lab processes:
  • Run tests
  • Generate results
  • Quality control
  • Validate results
```

**Step 5: Results Entry**
```
Lab technician:
  • Enters results in system
  • Links to patient
  • Links to bill/order
  • Approves for release
```

**Step 6: Results Access**
```
Results available to:
  • Ordering doctor (EMR)
  • Patient (portal/print)
  • Lab staff (management)
```

### Integration Points
```
Lab Billing connects to:
  ✅ OPD Billing system
  ✅ LIMS (Lab Information System)
  ✅ EMR (doctor's records)
  ✅ Patient record
```

### Database Tables
- `BILL` - Lab billing record
- `BILLITEM` - Test items
- `PATIENTINVESTIGATION` - Test orders
- `PATIENTREPORT` - Results
- `INVESTIGATIONITEM` - Test master data

### Test URL
```
http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
(Select investigation items)
```

---

## 🔄 FLOW COMPARISON

| Aspect | OPD Billing | Pharmacy Billing | Lab Billing |
|--------|-------------|------------------|-------------|
| **Primary URL** | `/opd/view/opd_bill.xhtml` | `/pharmacy/pharmacy_bill_retail_sale.xhtml` | Via OPD Bill |
| **Items** | Services, procedures | Medications | Lab tests |
| **Stock Impact** | No | Yes ✅ | No |
| **Prescription** | Not required | Required for some | Doctor's order |
| **Physical Delivery** | Service performed | Medicines dispensed | Results delivered |
| **Integration** | EMR, Billing | EMR, Stock, Billing | EMR, LIMS, Billing |

---

## 🧪 TESTING GUIDE - Patient: 0742020610

### Prerequisites
1. ✅ Payara running with 3GB memory
2. ✅ Application deployed
3. ✅ Database accessible
4. ✅ User logged in with billing privileges

### TEST 1: OPD BILLING FLOW

**Objective**: Create OPD consultation bill

**Steps**:
```
1. Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml

2. Search Patient:
   Phone: 0742020610
   [Search]

3. If patient exists → Select
   If not → Create new patient first

4. Add Services:
   Service: "General Consultation"
   Quantity: 1
   Institution: Central Hospital
   Department: OPD
   [Add]

5. Add Investigation:
   Service: "Complete Blood Count"
   Quantity: 1
   [Add]

6. Review Total:
   Gross Total: KES 2,500
   Discount: KES 0
   Net Total: KES 2,500

7. Select Payment:
   Payment Method: Cash
   Amount Tendered: KES 2,500

8. Generate Bill:
   [Save & Print]

9. Verify:
   ✅ Bill number generated
   ✅ Receipt printed
   ✅ Patient copy issued
```

**Expected Result**:
```
✅ Bill created successfully
✅ Bill Number: OPD-2025-XXXXX
✅ Total: KES 2,500
✅ Payment: Cash
✅ Status: Paid
```

---

### TEST 2: PHARMACY BILLING FLOW

**Objective**: Dispense medications

**Steps**:
```
1. Navigate: http://localhost:8080/rh/faces/pharmacy/pharmacy_bill_retail_sale.xhtml

2. Search Patient:
   Phone: 0742020610
   [Search]
   Select patient

3. Add Medications:
   
   Medicine 1:
   Name: Paracetamol 500mg
   Quantity: 20 tablets
   [Add]
   
   Medicine 2:
   Name: Amoxicillin 250mg
   Quantity: 21 capsules
   [Add]

4. Review Total:
   Paracetamol: KES 200
   Amoxicillin: KES 630
   Total: KES 830

5. Select Payment:
   Payment Method: Cash
   Amount: KES 830

6. Dispense:
   [Save & Print]
   Print bill and labels

7. Stock Check:
   ✅ Verify stock reduced
   ✅ Check batch tracking

8. Handover:
   Give medications to patient
   Explain dosage
```

**Expected Result**:
```
✅ Pharmacy bill created
✅ Bill Number: PHARM-2025-XXXXX
✅ Total: KES 830
✅ Stock updated
✅ Medicines dispensed
```

---

### TEST 3: LABORATORY BILLING FLOW

**Objective**: Order and process lab tests

**Steps**:
```
1. Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml

2. Search Patient:
   Phone: 0742020610

3. Add Lab Tests:
   
   Test 1:
   Investigation: Complete Blood Count (CBC)
   [Add]
   
   Test 2:
   Investigation: Liver Function Test (LFT)
   [Add]

4. Review Total:
   CBC: KES 1,500
   LFT: KES 2,000
   Total: KES 3,500

5. Payment:
   Method: Cash
   Amount: KES 3,500
   [Save & Print]

6. Generate Lab Request:
   Print lab request form
   Give to patient

7. Sample Collection (Lab Side):
   Navigate: Lab sample collection
   Log sample
   Collect blood

8. Results Entry:
   Navigate: Lab results entry
   Enter test results
   Approve & release

9. Doctor Views Results:
   Navigate: EMR page
   Tab: Previous Investigations
   View results
```

**Expected Result**:
```
✅ Lab bill created: LAB-2025-XXXXX
✅ Total: KES 3,500
✅ Lab request generated
✅ Sample collected
✅ Results entered
✅ Available to doctor in EMR
```

---

## 🔍 DEBUGGING CHECKLIST

### Common Issues & Solutions

**Issue 1: Patient Not Found**
```
Problem: Search returns no results
Solution:
  1. Create patient first
  2. Use exact phone format (0742020610)
  3. Check database for patient record
  4. Verify phone number in correct field
```

**Issue 2: Services Not Loading**
```
Problem: Service dropdown empty
Solution:
  1. Check if services created in admin
  2. Verify institution/department setup
  3. Check service is not retired
  4. Verify user permissions
```

**Issue 3: Payment Not Processing**
```
Problem: Bill doesn't save
Solution:
  1. Check required fields filled
  2. Verify payment method configured
  3. Check user has billing privileges
  4. Review server logs for errors
```

**Issue 4: Stock Not Updating (Pharmacy)**
```
Problem: Stock doesn't reduce after sale
Solution:
  1. Verify stock exists for item
  2. Check batch availability
  3. Ensure stock tracking enabled
  4. Review stock transaction logs
```

**Issue 5: Lab Results Not Appearing**
```
Problem: Results not visible in EMR
Solution:
  1. Verify results entered and approved
  2. Check linking to correct patient
  3. Verify linking to encounter
  4. Check doctor permissions
```

---

## 📊 VALIDATION QUERIES

**Check OPD Bill Created**:
```sql
SELECT billNo, netTotal, paymentMethod, billTime 
FROM BILL 
WHERE patient IN (
  SELECT id FROM PERSON WHERE phone='0742020610'
) 
AND billType='OpdBill' 
ORDER BY billTime DESC LIMIT 5;
```

**Check Pharmacy Sale**:
```sql
SELECT billNo, netTotal, billTime 
FROM BILL 
WHERE patient IN (
  SELECT id FROM PERSON WHERE phone='0742020610'
) 
AND billType='PharmacySale' 
ORDER BY billTime DESC LIMIT 5;
```

**Check Lab Orders**:
```sql
SELECT * FROM BILLITEM 
WHERE bill_id IN (
  SELECT id FROM BILL WHERE patient IN (
    SELECT id FROM PERSON WHERE phone='0742020610'
  )
)
AND item_id IN (
  SELECT id FROM ITEM WHERE dtype='Investigation'
);
```

---

## ✅ SUCCESS CRITERIA

All three flows tested successfully when:

**OPD Billing**:
- ✅ Patient searched and found
- ✅ Services added to bill
- ✅ Payment processed
- ✅ Receipt generated
- ✅ Bill saved in database

**Pharmacy Billing**:
- ✅ Patient found
- ✅ Medicines added
- ✅ Stock available
- ✅ Payment processed
- ✅ Stock updated
- ✅ Medicines dispensed

**Laboratory Billing**:
- ✅ Lab tests ordered via OPD bill
- ✅ Payment processed
- ✅ Lab request generated
- ✅ Sample collected
- ✅ Results entered
- ✅ Results visible in EMR

---

## 📝 TEST RESULTS TEMPLATE

### Test Date: [Date]
### Tester: [Name]
### Patient Phone: 0742020610

#### Flow 1: OPD Billing
- [ ] Patient search: PASS/FAIL
- [ ] Service addition: PASS/FAIL
- [ ] Payment processing: PASS/FAIL
- [ ] Receipt generation: PASS/FAIL
- [ ] Bill Number: __________
- [ ] Total Amount: __________
- [ ] Notes: __________

#### Flow 2: Pharmacy Billing
- [ ] Patient search: PASS/FAIL
- [ ] Medicine addition: PASS/FAIL
- [ ] Stock check: PASS/FAIL
- [ ] Payment processing: PASS/FAIL
- [ ] Stock update: PASS/FAIL
- [ ] Bill Number: __________
- [ ] Total Amount: __________
- [ ] Notes: __________

#### Flow 3: Laboratory Billing
- [ ] Lab order via OPD: PASS/FAIL
- [ ] Payment processing: PASS/FAIL
- [ ] Lab request printed: PASS/FAIL
- [ ] Sample collection: PASS/FAIL
- [ ] Results entry: PASS/FAIL
- [ ] Results in EMR: PASS/FAIL
- [ ] Bill Number: __________
- [ ] Total Amount: __________
- [ ] Notes: __________

---

## 🎯 SUMMARY

**Total Billing Flows**: 3
- OPD Billing (Services/Procedures)
- Pharmacy Billing (Medications)
- Laboratory Billing (Tests/Investigations)

**All flows integrate with**:
- Patient Management System
- EMR (Electronic Medical Records)
- Financial Reporting
- Inventory Management (Pharmacy)
- LIMS (Laboratory only)

**Key URLs**:
1. OPD: `/opd/view/opd_bill.xhtml`
2. Pharmacy: `/pharmacy/pharmacy_bill_retail_sale.xhtml`
3. Lab: Via OPD billing with investigation items

