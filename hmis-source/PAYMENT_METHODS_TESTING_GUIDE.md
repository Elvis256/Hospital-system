# Complete Payment Methods Testing Guide

## Patient Details for Testing
**Phone Number**: 0742020610  
**Test Date**: 2025-11-09  
**All tests should use the SAME patient**

---

## 📊 Total Payment Methods: 18

The HMIS system supports the following payment methods:

| # | Payment Method | Type | Description |
|---|---------------|------|-------------|
| 1 | **Cash** | Non-Credit | Physical cash payment |
| 2 | **Credit Card** | Non-Credit | Card payment (Visa/Mastercard) |
| 3 | **Multiple Payment Methods** | Non-Credit | Split payment across methods |
| 4 | **Staff Credit** | Credit | Staff member credit account |
| 5 | **Credit** | Credit | General credit account |
| 6 | **Staff Welfare** | Non-Credit | Staff welfare fund |
| 7 | **Voucher** | Non-Credit | Payment voucher |
| 8 | **IOU** | Non-Credit | I Owe You payment |
| 9 | **Agent Payment** | Non-Credit | Payment via agent |
| 10 | **Cheque** | Non-Credit | Bank cheque |
| 11 | **Slip Payment** | Non-Credit | Payment slip/bank slip |
| 12 | **e-Wallet Payment** | Non-Credit | Mobile money/digital wallet |
| 13 | **Patient Deposit** | Non-Credit | Pre-paid deposit |
| 14 | **Patient Points** | Non-Credit | Loyalty points |
| 15 | **Online Settlement** | Non-Credit | Online payment gateway |
| 16 | **None** | None | No payment method |
| 17 | **OnlineBooking Agent** | None | Online booking system |
| 18 | **On Call** | None | Telephone booking |

---

## 🧪 TESTING MATRIX

### Patient: 0742020610
### Service: General Consultation (KES 1,000)

Test each payment method with the same patient to verify all options work correctly.

---

## TEST 1: Cash Payment ✅

### Test Details
- **Payment Method**: Cash
- **Amount**: KES 1,000
- **Type**: Non-Credit (Immediate payment)

### Steps
```
1. Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
2. Search Patient: 0742020610
3. Add Service: General Consultation (KES 1,000)
4. Select Payment Method: Cash
5. Amount Tendered: KES 1,000
6. Click: Save & Print
```

### Expected Result
```
✅ Bill created: OPD-2025-001
✅ Payment Method: Cash
✅ Amount: KES 1,000
✅ Status: Paid
✅ Cash in hand entry created
✅ Receipt printed
```

### Verification Query
```sql
SELECT billNo, netTotal, paymentMethod, paid 
FROM BILL 
WHERE id = [BILL_ID] AND paymentMethod = 'Cash';
```

---

## TEST 2: Credit Card Payment 💳

### Test Details
- **Payment Method**: Credit Card
- **Amount**: KES 1,500
- **Type**: Non-Credit

### Steps
```
1. Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
2. Search Patient: 0742020610
3. Add Service: Specialist Consultation (KES 1,500)
4. Select Payment Method: Credit Card
5. Card Details:
   - Card Type: Visa/Mastercard
   - Last 4 digits: 1234
   - Transaction ID: TXN123456
6. Amount: KES 1,500
7. Click: Save & Print
```

### Expected Result
```
✅ Bill created: OPD-2025-002
✅ Payment Method: Card
✅ Amount: KES 1,500
✅ Card details recorded
✅ Transaction ID saved
✅ Receipt printed
```

### Additional Fields
- Card authorization code
- Transaction reference
- Bank name

---

## TEST 3: Multiple Payment Methods 💰+💳

### Test Details
- **Payment Methods**: Cash + Credit Card
- **Total Amount**: KES 2,000
- **Split**: Cash KES 1,000 + Card KES 1,000

### Steps
```
1. Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
2. Search Patient: 0742020610
3. Add Service: ECG Test (KES 2,000)
4. Select Payment Method: Multiple Payment Methods
5. Split Payment:
   Method 1: Cash - KES 1,000
   Method 2: Credit Card - KES 1,000
6. Click: Save & Print
```

### Expected Result
```
✅ Bill created: OPD-2025-003
✅ Payment Method: MultiplePaymentMethods
✅ Total: KES 2,000
✅ Split recorded:
   - Cash: 1,000
   - Card: 1,000
✅ Both payments tracked separately
```

---

## TEST 4: Staff Credit 👨‍⚕️

### Test Details
- **Payment Method**: Staff Credit
- **Amount**: KES 800
- **Type**: Credit (Deferred payment)

### Prerequisites
- Patient must be linked as staff member
- Staff must have credit limit

### Steps
```
1. Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
2. Search Patient: 0742020610 (must be staff)
3. Add Service: Medical Checkup (KES 800)
4. Select Payment Method: Staff Credit
5. Verify credit limit available
6. Click: Save & Print
```

### Expected Result
```
✅ Bill created: OPD-2025-004
✅ Payment Method: Staff
✅ Amount: KES 800
✅ Status: Credit (Unpaid)
✅ Staff account debited
✅ Credit limit reduced
```

### Credit Account Check
```
Staff Credit Balance Before: KES 5,000
After Transaction: KES 4,200 (5,000 - 800)
```

---

## TEST 5: General Credit Account 🏦

### Test Details
- **Payment Method**: Credit
- **Amount**: KES 1,200
- **Type**: Credit

### Prerequisites
- Patient must have credit account
- Credit limit must be set

### Steps
```
1. Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
2. Search Patient: 0742020610
3. Verify patient has credit account enabled
4. Add Service: Lab Test - CBC (KES 1,200)
5. Select Payment Method: Credit
6. Verify credit available
7. Click: Save & Print
```

### Expected Result
```
✅ Bill created: OPD-2025-005
✅ Payment Method: Credit
✅ Amount: KES 1,200
✅ Status: Credit (To be settled)
✅ Patient account debited
✅ Credit bill generated for later settlement
```

---

## TEST 6: Staff Welfare 🏥

### Test Details
- **Payment Method**: Staff Welfare
- **Amount**: KES 600
- **Type**: Non-Credit

### Steps
```
1. Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
2. Search Patient: 0742020610 (staff member)
3. Add Service: Prescription (KES 600)
4. Select Payment Method: Staff Welfare
5. Enter welfare approval code (if required)
6. Click: Save & Print
```

### Expected Result
```
✅ Bill created: OPD-2025-006
✅ Payment Method: Staff_Welfare
✅ Amount: KES 600
✅ Welfare fund debited
✅ Approval recorded
```

---

## TEST 7: Voucher Payment 🎟️

### Test Details
- **Payment Method**: Voucher
- **Amount**: KES 1,000
- **Type**: Non-Credit

### Steps
```
1. Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
2. Search Patient: 0742020610
3. Add Service: Consultation (KES 1,000)
4. Select Payment Method: Voucher
5. Enter Voucher Details:
   - Voucher Number: VOUCH-001
   - Voucher Value: KES 1,000
   - Issued By: HR Department
6. Click: Save & Print
```

### Expected Result
```
✅ Bill created: OPD-2025-007
✅ Payment Method: Voucher
✅ Voucher number recorded
✅ Amount: KES 1,000
✅ Voucher marked as used
```

---

## TEST 8: IOU Payment 📝

### Test Details
- **Payment Method**: IOU (I Owe You)
- **Amount**: KES 500
- **Type**: Non-Credit

### Steps
```
1. Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
2. Search Patient: 0742020610
3. Add Service: Dressing (KES 500)
4. Select Payment Method: IOU
5. Enter IOU Details:
   - Reference Number
   - Promised Payment Date
   - Guarantor (if required)
6. Click: Save & Print
```

### Expected Result
```
✅ Bill created: OPD-2025-008
✅ Payment Method: IOU
✅ Amount: KES 500
✅ IOU recorded
✅ Payment promise logged
```

---

## TEST 9: Agent Payment 🤝

### Test Details
- **Payment Method**: Agent Payment
- **Amount**: KES 2,500
- **Type**: Non-Credit

### Prerequisites
- Agent must be registered in system
- Agent commission structure defined

### Steps
```
1. Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
2. Search Patient: 0742020610
3. Add Service: Specialist Consultation (KES 2,500)
4. Select Payment Method: Agent Payment
5. Select Agent from list
6. Agent Commission: Auto-calculated (e.g., 10% = 250)
7. Click: Save & Print
```

### Expected Result
```
✅ Bill created: OPD-2025-009
✅ Payment Method: Agent
✅ Amount: KES 2,500
✅ Agent recorded
✅ Commission calculated: KES 250
✅ Net amount: KES 2,250
```

---

## TEST 10: Cheque Payment 💵

### Test Details
- **Payment Method**: Cheque
- **Amount**: KES 3,000
- **Type**: Non-Credit

### Steps
```
1. Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
2. Search Patient: 0742020610
3. Add Service: Medical Procedure (KES 3,000)
4. Select Payment Method: Cheque
5. Enter Cheque Details:
   - Cheque Number: 123456
   - Bank Name: ABC Bank
   - Branch: Main Branch
   - Date: 2025-11-09
6. Amount: KES 3,000
7. Click: Save & Print
```

### Expected Result
```
✅ Bill created: OPD-2025-010
✅ Payment Method: Cheque
✅ Cheque details recorded
✅ Amount: KES 3,000
✅ Pending clearance status
✅ Will update to "Cleared" after bank verification
```

---

## TEST 11: Slip Payment 🧾

### Test Details
- **Payment Method**: Slip Payment (Bank Deposit Slip)
- **Amount**: KES 1,800
- **Type**: Non-Credit

### Steps
```
1. Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
2. Search Patient: 0742020610
3. Add Service: X-Ray (KES 1,800)
4. Select Payment Method: Slip Payment
5. Enter Deposit Slip Details:
   - Slip Number: DEP-789
   - Bank: XYZ Bank
   - Deposit Date: 2025-11-09
   - Branch: City Branch
6. Click: Save & Print
```

### Expected Result
```
✅ Bill created: OPD-2025-011
✅ Payment Method: Slip
✅ Slip details recorded
✅ Amount: KES 1,800
✅ Pending verification
```

---

## TEST 12: e-Wallet Payment 📱

### Test Details
- **Payment Method**: e-Wallet (M-Pesa, Airtel Money, etc.)
- **Amount**: KES 900
- **Type**: Non-Credit

### Steps
```
1. Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
2. Search Patient: 0742020610
3. Add Service: Consultation Follow-up (KES 900)
4. Select Payment Method: e-Wallet Payment
5. Enter Details:
   - Wallet Type: M-Pesa
   - Transaction Code: MPE123456789
   - Phone Number: 0742020610
   - Transaction Date/Time: 2025-11-09 14:30
6. Click: Save & Print
```

### Expected Result
```
✅ Bill created: OPD-2025-012
✅ Payment Method: ewallet
✅ Transaction code recorded
✅ Amount: KES 900
✅ Mobile money entry created
✅ Receipt with transaction details
```

---

## TEST 13: Patient Deposit 💰

### Test Details
- **Payment Method**: Patient Deposit
- **Amount**: KES 1,500
- **Type**: Non-Credit (Using pre-paid balance)

### Prerequisites
- Patient must have deposit balance
- Deposit must be sufficient

### Steps
```
1. First: Patient makes deposit
   Navigate: Patient Deposit page
   Patient: 0742020610
   Deposit Amount: KES 5,000
   Save deposit

2. Then: Use deposit for billing
   Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
   Search Patient: 0742020610
   Add Service: Minor Surgery (KES 1,500)
   Select Payment Method: Patient Deposit
   Available Balance: KES 5,000
   Amount to deduct: KES 1,500
   Click: Save & Print
```

### Expected Result
```
✅ Bill created: OPD-2025-013
✅ Payment Method: PatientDeposit
✅ Amount: KES 1,500
✅ Deposit balance before: KES 5,000
✅ Deposit balance after: KES 3,500
✅ Transaction logged
```

---

## TEST 14: Patient Points 🎁

### Test Details
- **Payment Method**: Patient Points (Loyalty Program)
- **Amount**: KES 500
- **Type**: Non-Credit

### Prerequisites
- Patient enrolled in loyalty program
- Sufficient points available
- Points conversion rate defined (e.g., 1 point = KES 1)

### Steps
```
1. Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
2. Search Patient: 0742020610
3. Check available points: 1,000 points
4. Add Service: Lab Test (KES 500)
5. Select Payment Method: Patient Points
6. Points to redeem: 500 points
7. Remaining points: 500
8. Click: Save & Print
```

### Expected Result
```
✅ Bill created: OPD-2025-014
✅ Payment Method: PatientPoints
✅ Amount: KES 500
✅ Points redeemed: 500
✅ Points balance updated: 500 remaining
✅ Receipt shows points transaction
```

---

## TEST 15: Online Settlement 🌐

### Test Details
- **Payment Method**: Online Settlement
- **Amount**: KES 2,200
- **Type**: Non-Credit

### Steps
```
1. Navigate: http://localhost:8080/rh/faces/opd/view/opd_bill.xhtml
2. Search Patient: 0742020610
3. Add Service: Comprehensive Checkup (KES 2,200)
4. Select Payment Method: Online Settlement
5. Enter Payment Gateway Details:
   - Gateway: PayPal/Stripe/Local Gateway
   - Transaction ID: TXN987654321
   - Authorization Code: AUTH123
   - Payment Time: 2025-11-09 15:00
6. Click: Save & Print
```

### Expected Result
```
✅ Bill created: OPD-2025-015
✅ Payment Method: OnlineSettlement
✅ Amount: KES 2,200
✅ Gateway transaction recorded
✅ Payment verified online
✅ Immediate confirmation
```

---

## TEST 16: On Call Payment ☎️

### Test Details
- **Payment Method**: On Call
- **Amount**: KES 800
- **Type**: None (Telephone booking)

### Steps
```
1. Navigate: Channelling/Booking page
2. Phone: 0742020610
3. Book appointment via phone call
4. Select Payment Method: On Call
5. Appointment details recorded
6. Payment to be made on arrival
```

### Expected Result
```
✅ Appointment booked
✅ Payment Method: OnCall
✅ Amount: KES 800
✅ Payment deferred to arrival
✅ Booking confirmed
```

---

## 📊 COMPREHENSIVE TEST MATRIX

### Test Summary Template

| Test # | Payment Method | Amount | Status | Bill Number | Notes |
|--------|---------------|--------|--------|-------------|-------|
| 1 | Cash | 1,000 | ☐ PASS/FAIL | | |
| 2 | Credit Card | 1,500 | ☐ PASS/FAIL | | |
| 3 | Multiple Methods | 2,000 | ☐ PASS/FAIL | | |
| 4 | Staff Credit | 800 | ☐ PASS/FAIL | | |
| 5 | Credit | 1,200 | ☐ PASS/FAIL | | |
| 6 | Staff Welfare | 600 | ☐ PASS/FAIL | | |
| 7 | Voucher | 1,000 | ☐ PASS/FAIL | | |
| 8 | IOU | 500 | ☐ PASS/FAIL | | |
| 9 | Agent | 2,500 | ☐ PASS/FAIL | | |
| 10 | Cheque | 3,000 | ☐ PASS/FAIL | | |
| 11 | Slip | 1,800 | ☐ PASS/FAIL | | |
| 12 | e-Wallet | 900 | ☐ PASS/FAIL | | |
| 13 | Patient Deposit | 1,500 | ☐ PASS/FAIL | | |
| 14 | Patient Points | 500 | ☐ PASS/FAIL | | |
| 15 | Online Settlement | 2,200 | ☐ PASS/FAIL | | |

**Total Amount Tested**: KES 18,900

---

## 🔍 VALIDATION QUERIES

### Check All Bills for Patient
```sql
SELECT 
    billNo,
    billTime,
    paymentMethod,
    netTotal,
    paid,
    deptId
FROM BILL
WHERE patient IN (
    SELECT id FROM PERSON WHERE phone='0742020610'
)
ORDER BY billTime DESC;
```

### Check Payment Method Distribution
```sql
SELECT 
    paymentMethod,
    COUNT(*) as count,
    SUM(netTotal) as total_amount
FROM BILL
WHERE patient IN (
    SELECT id FROM PERSON WHERE phone='0742020610'
)
GROUP BY paymentMethod;
```

### Check Credit Payments
```sql
SELECT 
    billNo,
    netTotal,
    paymentMethod,
    paid
FROM BILL
WHERE patient IN (
    SELECT id FROM PERSON WHERE phone='0742020610'
)
AND paymentMethod IN ('Staff', 'Credit')
AND paid = false;
```

---

## ✅ SUCCESS CRITERIA

All payment methods tested successfully when:

1. ✅ Each payment method creates bill successfully
2. ✅ Correct payment method recorded in database
3. ✅ Amount correctly processed
4. ✅ Receipt generated for each
5. ✅ Payment-specific details captured (e.g., card number, cheque number)
6. ✅ Credit limits updated (for credit methods)
7. ✅ Deposits/points reduced (for pre-paid methods)
8. ✅ All bills linked to same patient (0742020610)

---

## 🎯 SUMMARY

**Total Payment Methods**: 18
**Active Methods**: 16 (excluding deprecated)
**Test Patient**: 0742020610
**Total Test Amount**: KES 18,900

**Categories**:
- **Non-Credit** (Immediate payment): 13 methods
- **Credit** (Deferred payment): 2 methods
- **Special** (Booking/None): 3 methods

**All payment methods should work independently and be properly recorded in the billing system!**

