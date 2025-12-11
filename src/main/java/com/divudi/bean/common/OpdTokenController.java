package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.FinancialTransactionController;
import com.divudi.bean.clinical.PatientEncounterController;
import com.divudi.bean.clinical.PastPatientEncounterController;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.pharmacy.PharmacyBillSearch;
import com.divudi.bean.pharmacy.PharmacyPreSettleController;
import com.divudi.bean.pharmacy.PharmacySaleController;
import com.divudi.core.data.EncounterType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.TokenCount;
import com.divudi.core.data.TokenType;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.Token;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.TokenFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author Damiya
 */
@Named
@SessionScoped
public class OpdTokenController implements Serializable, ControllerWithPatient {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    BillNumberGenerator billNumberGenerator;
    @EJB
    TokenFacade tokenFacade;
    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    com.divudi.core.facade.BillSessionFacade billSessionFacade;
    @EJB
    com.divudi.core.facade.ServiceSessionFacade serviceSessionFacade;
    @EJB
    com.divudi.core.facade.PatientEncounterFacade patientEncounterFacade;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    SessionController sessionController;
    @Inject
    PharmacySaleController pharmacySaleController;
    @Inject
    PharmacyPreSettleController pharmacyPreSettleController;
    @Inject
    PatientController patientController;
    @Inject
    private PharmacyBillSearch pharmacyBillSearch;
    @Inject
    OpdTabPreBillController opdTabPreBillController;
    @Inject
    OpdPreSettleController opdPreSettleController;
    @Inject
    FinancialTransactionController financialTransactionController;
    @Inject
    OpdPreBillController opdPreBillController;

    // </editor-fold>
    private Token currentToken;
    private Token onGoingToken;
    private Token removeingToken;
    private List<Token> currentTokens;
    private Patient patient;
    private PaymentMethod paymentMethod;

    private Department department;
    private Institution institution;
    private Department counter;
    private Department selectedCounter;
    private Doctor doctor;
    private Staff staff;
    private Bill bill;
    private boolean patientDetailsEditable;
    private List<TokenCount> tokenCounts;

    private boolean printPreview;

    public OpdTokenController() {
    }

    private void resetClassVariables() {
        currentToken = null;
        removeingToken = null;
        currentTokens = null;
        patient = null;
        printPreview = false;
    }

    public void saveToken(Token t) {
        if (t == null) {
            return;
        }
        if (t.getId() == null) {
            t.setCreatedAt(new Date());
            t.setCreatedBy(sessionController.getLoggedUser());
            tokenFacade.create(t);
        } else {
            tokenFacade.edit(t);
            onGoingToken = t;
        }
    }

    public String navigateToCreateNewOpdToken() {
        if (bill != null) {
            staff = bill.getFromStaff();
            patient = bill.getPatient();
        } else {
            resetClassVariables();
        }
        currentToken = new Token();
        currentToken.setTokenType(TokenType.OPD_TOKEN);
        currentToken.setDepartment(sessionController.getDepartment());
        currentToken.setFromDepartment(sessionController.getDepartment());
        currentToken.setPatient(getPatient());
        currentToken.setInstitution(sessionController.getInstitution());
        currentToken.setFromInstitution(sessionController.getInstitution());
        if (getCounter() == null) {
            if (sessionController.getLoggableSubDepartments() != null
                    && !sessionController.getLoggableSubDepartments().isEmpty()) {
                counter = sessionController.getLoggableSubDepartments().get(0);
            }
        }
        currentToken.setCounter(getCounter());
        if (counter != null) {
            currentToken.setToDepartment(counter.getSuperDepartment());
            if (counter.getSuperDepartment() != null) {
                currentToken.setToInstitution(counter.getSuperDepartment().getInstitution());
            }
        }
        return "/opd/token/opd_token?faces-redirect=true";
    }

    public String settleOpdToken() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token");
            return "";
        }
        if (currentToken.getTokenType() == null) {
            JsfUtil.addErrorMessage("Wrong Token");
            return "";
//        }
//        if (getPatient().getId() == null) {
//            JsfUtil.addErrorMessage("Please select a patient");
//            return "";
        } else if (getPatient().getPerson().getName() == null) {
            JsfUtil.addErrorMessage("Please select a patient");
            return "";
        } else if (getPatient().getPerson().getName().trim().equals("")) {
            JsfUtil.addErrorMessage("Please select a patient");
            return "";
        } else {
            patientController.save(patient);
            currentToken.setPatient(getPatient());
        }
        if (sessionController.getDepartmentPreference().isOpdSettleWithoutPatientArea()) {
            if (getPatient().getPerson().getArea() == null || getPatient().getPerson().getArea().getName().trim() == "") {
                JsfUtil.addErrorMessage("Please select a patient Area");
                return "";
            }
        }

        if (currentToken.getToDepartment() == null) {
            currentToken.setToDepartment(sessionController.getDepartment());
        }
        if (currentToken.getToInstitution() == null) {
            currentToken.setToInstitution(sessionController.getInstitution());
        }
        if (currentToken.getId() == null) {
            currentToken.setCreatedAt(new Date());
            currentToken.setCreatedBy(sessionController.getLoggedUser());
            tokenFacade.create(currentToken);
        } else {
            tokenFacade.edit(currentToken);
        }
        if (sessionController.getDepartmentPreference().isGenarateOpdTokenNumbersToCounterWise()) {
            currentToken.setTokenNumber(billNumberGenerator.generateDailyTokenNumberCounterWise(currentToken.getFromDepartment(), counter, null, null, TokenType.OPD_TOKEN));
        } else {
            currentToken.setTokenNumber(billNumberGenerator.generateDailyTokenNumber(currentToken.getFromDepartment(), null, null, TokenType.OPD_TOKEN));
        }
        currentToken.setCounter(counter);
        currentToken.setStaff(staff);
        currentToken.setTokenDate(new Date());
        currentToken.setTokenAt(new Date());
        tokenFacade.edit(currentToken);
        
        // Create BillSession and ServiceSession for clinical queue integration
        if (staff != null) {
            createBillSessionForToken(currentToken);
        }
        
        printPreview = true;
        return "/opd/token/opd_token_print?faces-redirect=true";
    }
    
    private void createBillSessionForToken(Token token) {
        try {
            Date sessionDate = token.getTokenDate() != null ? token.getTokenDate() : new Date();
            
            // Find or create ServiceSession for the staff and date
            Map<String, Object> params = new HashMap<>();
            params.put("staff", token.getStaff());
            params.put("date", sessionDate);
            String sql = "SELECT ss FROM ServiceSession ss WHERE ss.staff = :staff AND ss.sessionDate = :date";
            com.divudi.core.entity.ServiceSession serviceSession = serviceSessionFacade.findFirstByJpql(sql, params, TemporalType.DATE);
            
            if (serviceSession == null) {
                // Create new ServiceSession
                serviceSession = new com.divudi.core.entity.ServiceSession();
                serviceSession.setStaff(token.getStaff());
                serviceSession.setSessionDate(sessionDate);
                serviceSession.setOriginatingSession(serviceSession);
                serviceSession.setName(token.getStaff().getPerson().getName() + " - " + new java.text.SimpleDateFormat("yyyy-MM-dd").format(sessionDate));
                serviceSession.setCreatedAt(new Date());
                serviceSession.setCreater(sessionController.getLoggedUser());
                serviceSessionFacade.create(serviceSession);
            }
            
            // Get or create bill for this token
            Bill bill = token.getBill();
            if (bill == null && token.getPatient() != null) {
                // Create a reference bill for queue purposes
                bill = new Bill();
                bill.setPatient(token.getPatient());
                bill.setToStaff(token.getStaff());
                bill.setToDepartment(token.getToDepartment());
                bill.setToInstitution(token.getToInstitution());
                bill.setCreatedAt(new Date());
                bill.setCreater(sessionController.getLoggedUser());
                billFacade.create(bill);
            }
            
            // Create BillSession for this token
            com.divudi.core.entity.BillSession billSession = new com.divudi.core.entity.BillSession();
            billSession.setServiceSession(serviceSession);
            billSession.setSessionDate(sessionDate);
            billSession.setBill(bill);
            billSession.setStaff(token.getStaff());
            int serialNo = 0;
            try {
                if (token.getTokenNumber() != null && !token.getTokenNumber().isEmpty()) {
                    serialNo = Integer.parseInt(token.getTokenNumber());
                }
            } catch (NumberFormatException e) {
                // Use 0 if token number is not numeric
            }
            billSession.setSerialNo(serialNo);
            billSession.setDepartment(token.getToDepartment());
            billSession.setInstitution(token.getToInstitution());
            billSession.setCreatedAt(new Date());
            billSession.setCreater(sessionController.getLoggedUser());
            billSessionFacade.create(billSession);
            
            // Create PatientEncounter for nurse triage queue
            if (token.getPatient() != null) {
                com.divudi.core.entity.PatientEncounter encounter = new com.divudi.core.entity.PatientEncounter();
                encounter.setPatient(token.getPatient());
                encounter.setEncounterType(com.divudi.core.data.EncounterType.Opd);
                encounter.setPatientEncounterType(com.divudi.core.data.inward.PatientEncounterType.OpdVisit);
                encounter.setEncounterDate(sessionDate);
                encounter.setCreatedAt(new Date());
                encounter.setCreater(sessionController.getLoggedUser());
                encounter.setDepartment(token.getToDepartment());
                encounter.setInstitution(token.getToInstitution());
                encounter.setOpdDoctor(token.getStaff());
                encounter.setTriageCompleted(false);
                encounter.setBhtNo(token.getTokenNumber());
                patientEncounterFacade.create(encounter);
                
                // Link encounter to bill session
                billSession.setPatientEncounter(encounter);
                billSessionFacade.edit(billSession);
                
                System.out.println("✓ Created PatientEncounter for token: " + token.getTokenNumber());
            }
            
            // Link back to token if needed
            // token.setBillSession(billSession); // If Token has this field
            // tokenFacade.edit(token);
        } catch (Exception e) {
            System.err.println("Error creating BillSession for token: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void genarateTokenNumberCounterWise() {
        if (counter != null) {

        }
    }

    public void toggleCalledStatus() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token selected");
            return;
        }
        currentToken.setCalled(!currentToken.isCalled());
        currentToken.setCalledAt(currentToken.isCalled() ? new Date() : null);
        tokenFacade.edit(currentToken);
    }

    public void toggleCompletedStatus() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token selected");
            return;
        }
        currentToken.setCompleted(!currentToken.isCompleted());
        Date now = new Date();
        currentToken.setCompletedAt(currentToken.isCompleted() ? now : null);
        currentToken.setStartedAt(currentToken.isCompleted() ? (currentToken.getStartedAt() == null ? now : currentToken.getStartedAt()) : null);
        tokenFacade.edit(currentToken);
    }

    public void deleteToken() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token selected");
            return;
        }
        currentToken.setRetired(true);
        currentToken.setRetiredBy(sessionController.getLoggedUser());
        tokenFacade.edit(currentToken);
        fillOpdTokens();
        JsfUtil.addSuccessMessage("Token deleted successfully");
    }

    public String navigateToManageOpdTokens() {
        counter = null;
        fillOpdTokens();
        return "/opd/token/maage_opd_tokens?faces-redirect=true";
    }

    public String navigateToSettleOpdPreBill() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No Token");
            return "";
        }
        if (currentToken.getBill() == null) {
            JsfUtil.addErrorMessage("No Bill");
            return "";
        }
        if (currentToken.getBill().getBillType() == null) {
            JsfUtil.addErrorMessage("No Bill Type");
            return "";
        }

        findPreBill(currentToken.getBill());
        opdPreSettleController.setBillPreview(false);
        opdPreSettleController.setToken(currentToken);
        opdPreSettleController.toSettle(currentToken.getBill());
        return "/opd/opd_bill_pre_settle?faces-redirect=true";
    }

    public void findPreBill(Bill args) {
        Bill tmp;
        String sql = "Select b from BilledBill b"
                + " where b.referenceBill=:bil"
                + " and b.retired=false "
                + " and b.cancelled=false ";
        HashMap hm = new HashMap();
        hm.put("bil", args);
        tmp = billFacade.findFirstByJpql(sql, hm);

        if (tmp != null) {
            JsfUtil.addErrorMessage("Allready Paid");
            return;
        } else {
            opdPreSettleController.setPreBill(args);
        }
    }

    public String navigateToNewOpdBillForCashier() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No Token");
            return "";
        }

        opdPreBillController.makeNull();
        opdPreBillController.setPatient(currentToken.getPatient());
        opdPreBillController.setToken(currentToken);

        return "/opd/opd_pre_bill?faces-redirect=true";
    }

    public String navigateToNewOpdBillForCashierTabView() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No Token");
            return "";
        }
        //System.out.println("navigateToNewOpdBillForCashierTabView");
        opdTabPreBillController.makeNull();
        opdTabPreBillController.reloadCurrentlyWorkingStaff();
        opdTabPreBillController.setPatient(currentToken.getPatient());
        opdTabPreBillController.setToken(currentToken);
        opdTabPreBillController.setSelectedCurrentlyWorkingStaff(currentToken.getStaff());
        return "/opd/token/opd_prebill_for_tab?faces-redirect=true";
    }

    public void navigateToNewOpdBill() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No Token");
            return;
        }

        opdTabPreBillController.makeNull();
        opdTabPreBillController.setPatient(currentToken.getPatient());
        opdTabPreBillController.setToken(currentToken);
    }

    public void fillOpdTokens() {
        String j = "Select t "
                + " from Token t"
                + " where t.department=:dep"
                + " and t.tokenType=:ty"
                + " and t.completed=:com"
                + " and t.retired=:ret";
        Map m = new HashMap();
        m.put("dep", sessionController.getDepartment());
        m.put("ty", TokenType.OPD_TOKEN);
        m.put("com", false);
        m.put("ret", false);
        if (counter != null) {
            j += " and t.counter =:ct";
            m.put("ct", counter);
        }
        j += " order by t.id ASC";
        currentTokens = tokenFacade.findByJpql(j, m, TemporalType.DATE);

    }

    public String getTokenStatus(Token token) {
        if (token.isRetired()) {
            return "Retired";
        } else if (token.isCompleted()) {
            return "Completed";
        } else if (token.isInProgress()) {
            return "In Progress";
        } else if (token.isCalled()) {
            return "Called";
        } else {
            return "Pending";
        }
    }

    public String navigateToManageOpdTokensCalled() {
        fillOpdTokensCalled();
        return "/opd/token/opd_tokens_called?faces-redirect=true"; // Adjust the navigation string as per your page structure
    }

    public String navigateToManageOpdTokensWaiting() {
        //fillOpdTokensWaiting();
        fillOpdWaitingTokensCounts();
        return "/opd/token/opd_tokens_waiting?faces-redirect=true"; // Adjust the navigation string as per your page structure
    }

    public void fillOpdTokensCalled() {
        Map<String, Object> m = new HashMap<>();
        String j = "Select t "
                + " from Token t"
                + " where t.department=:dep"
                + " and t.tokenDate=:date "
                + " and t.called=:cal "
                + " and t.tokenType=:ty"
                + " and t.inProgress=:prog "
                + " and t.completed=:com"
                + " and t.retired=:ret";
        m.put("dep", sessionController.getDepartment());
        m.put("date", new Date());
        m.put("cal", true);
        m.put("prog", false);
        m.put("ty", TokenType.OPD_TOKEN);
        m.put("com", false);
        m.put("ret", false);
        j += " order by t.id";
        currentTokens = tokenFacade.findByJpql(j, m, TemporalType.DATE);
    }

    public void fillOpdWaitingTokensCounts() {
        Map<String, Object> m = new HashMap<>();
        String j = "Select new com.divudi.core.data.TokenCount(t.counter, t.staff, count(t)) "
                + " from Token t"
                + " where t.department=:dep"
                + " and t.tokenDate=:date "
                + " and t.called=:cal "
                + " and t.tokenType=:ty"
                + " and t.inProgress=:prog "
                + " and t.completed=:com"
                + " and t.retired=:ret";

        boolean testing = false;
        if(testing){
            Token t=new Token();
            t.getCounter();
            t.getStaff();

        }

        m.put("dep", sessionController.getDepartment());
        m.put("date", new Date());
        m.put("cal", false);
        m.put("prog", false);
        m.put("ty", TokenType.OPD_TOKEN);
        m.put("com", false);
        m.put("ret", false);
        j += " group by t.counter, t.staff";
        currentTokens = tokenFacade.findByJpql(j, m, TemporalType.DATE);
        tokenCounts = (List<TokenCount>) tokenFacade.findLightsByJpql(j, m);
    }

    public void fillOpdTokensWaiting() {
        Map<String, Object> m = new HashMap<>();
        String j = "Select t "
                + " from Token t"
                + " where t.department=:dep"
                + " and t.tokenDate=:date "
                + " and t.called=:cal "
                + " and t.tokenType=:ty"
                + " and t.inProgress=:prog "
                + " and t.completed=:com"
                + " and t.retired=:ret";
        m.put("dep", sessionController.getDepartment());
        m.put("date", new Date());
        m.put("cal", false);
        m.put("prog", false);
        m.put("ty", TokenType.OPD_TOKEN);
        m.put("com", false);
        m.put("ret", false);
        j += " order by t.id";
        currentTokens = tokenFacade.findByJpql(j, m, TemporalType.DATE);
    }

    public String navigateToTokenIndex() {
        Boolean opdBillingAfterShiftStart = sessionController.getApplicationPreference().isOpdBillingAftershiftStart();
        if (opdBillingAfterShiftStart) {
            financialTransactionController.findNonClosedShiftStartFundBillIsAvailable();
            if (financialTransactionController.getNonClosedShiftStartFundBill() != null) {
                resetClassVariables();
                return "/opd/token/opd_token?faces-redirect=true";
            } else {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        } else {
            resetClassVariables();
            return "/opd/token/opd_token?faces-redirect=true";
        }
    }

    public String navigateToOpdQueue() {
        fillOpdTokens();
        return "/opd/token/opd_queue?faces-redirect=true";
    }

    public String navigateToManageOpdTokensCompleted() {
        counter = null;
        fillOpdTokensCompleted();
        return "/opd/token/opd_tokens_completed?faces-redirect=true";
    }

    public void fillOpdTokensCompleted() {
        String j = "Select t "
                + " from Token t"
                + " where t.department=:dep"
                + " and t.tokenType=:ty"
                + " and t.tokenDate=:date "
                + " and t.completed=:com"
                + " and t.retired=:ret";
        Map m = new HashMap();

        m.put("dep", sessionController.getDepartment());
        m.put("date", new Date());
        m.put("ty", TokenType.OPD_TOKEN);
        m.put("com", true);
        m.put("ret", false);
        if (counter != null) {
            j += " and t.counter =:ct";
            m.put("ct", counter);
        }
        j += " order by t.id";
        currentTokens = tokenFacade.findByJpql(j, m, TemporalType.DATE);
    }

    public void callToken() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token selected");
            return;
        }
        currentToken.setCalled(true);
        currentToken.setCalledAt(new Date());
        tokenFacade.edit(currentToken);
    }

//    public void startTokenService() {
//
//    }
//
//    public void completeTokenService() {
//
//    }
//
//    public void reverseCallToken() {
//
//    }
//
    public void recallToken() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("Please select valid Token");
            return;
        }

        if (currentToken.isCalled()) {
            currentToken.setCalled(false);
        } else {
            currentToken.setCalled(true);
        }
        tokenFacade.edit(currentToken);
    }

//    public void restartTokenService() {
//
//    }
    public void reverseCompleteTokenService() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("Token Is Not Valid !");
            return;
        }
        currentToken.setRestartTokenServices(true);
        currentToken.setCompleted(false);
        tokenFacade.edit(currentToken);
    }

    public Token getCurrentToken() {
        if (currentToken == null) {
            currentToken = new Token();
        }
        return currentToken;
    }

    public void setCurrentToken(Token currentToken) {
        this.currentToken = currentToken;
    }

    public Token getRemoveingToken() {
        return removeingToken;
    }

    public void setRemoveingToken(Token removeingToken) {
        this.removeingToken = removeingToken;
    }

    public List<Token> getCurrentTokens() {
        return currentTokens;
    }

    public void setCurrentTokens(List<Token> currentTokens) {
        this.currentTokens = currentTokens;
    }

    public PharmacyBillSearch getPharmacyBillSearch() {
        return pharmacyBillSearch;
    }

    public void setPharmacyBillSearch(PharmacyBillSearch pharmacyBillSearch) {
        this.pharmacyBillSearch = pharmacyBillSearch;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getCounter() {
        return counter;
    }

    public void setCounter(Department counter) {
        this.counter = counter;
    }

    public Department getSelectedCounter() {
        return selectedCounter;
    }

    public void setSelectedCounter(Department selectedCounter) {
        this.selectedCounter = selectedCounter;
    }

    @Override
    public Patient getPatient() {
        if (patient == null) {
            patient = new Patient();
            Person p = new Person();
            patientDetailsEditable = true;

            patient.setPerson(p);
        }
        return patient;
    }

    @Override
    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    @Override
    public boolean isPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    @Override
    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public Doctor getDoctor() {
        return doctor;
    }



    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public Token getOnGoingToken() {
        return onGoingToken;
    }

    public void setOnGoingToken(Token onGoingToken) {
        this.onGoingToken = onGoingToken;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public List<TokenCount> getTokenCounts() {
        return tokenCounts;
    }

    public void setTokenCounts(List<TokenCount> tokenCounts) {
        this.tokenCounts = tokenCounts;
    }




    @Override
    public void listnerForPaymentMethodChange() {
        // ToDo: Add Logic
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    @Override
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    // Doctor Queue Methods
    private List<Token> toCompleteTokens;
    private List<Token> completedTokens;

    public void fillOpdTokensForDoctor() {
        if (sessionController.getLoggedUser() == null || 
            sessionController.getLoggedUser().getStaff() == null) {
            toCompleteTokens = new ArrayList<>();
            completedTokens = new ArrayList<>();
            return;
        }

        Staff doctor = sessionController.getLoggedUser().getStaff();
        
        if (sessionDate == null) {
            sessionDate = new Date();
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("tokenType", TokenType.OPD_TOKEN);
        parameters.put("date", sessionDate);
        parameters.put("staff", doctor);

        // Get tokens for this doctor with PatientEncounter join fetch
        String jpql = "SELECT t FROM Token t "
                + "LEFT JOIN FETCH t.patientEncounter pe "
                + "LEFT JOIN FETCH t.patient p "
                + "LEFT JOIN FETCH p.person "
                + "WHERE t.tokenType = :tokenType "
                + "AND t.tokenDate = :date "
                + "AND t.staff = :staff "
                + "AND t.retired = false "
                + "ORDER BY t.tokenNumber";

        List<Token> allTokens = tokenFacade.findByJpql(jpql, parameters, TemporalType.DATE);
        
        // For tokens without PatientEncounter, try to find it
        for (Token token : allTokens) {
            if (token.getPatientEncounter() == null && token.getPatient() != null) {
                // Find PatientEncounter for this token
                Map<String, Object> params = new HashMap<>();
                params.put("patient", token.getPatient());
                params.put("date", sessionDate);
                params.put("staff", doctor);
                String encounterJpql = "SELECT pe FROM PatientEncounter pe "
                        + "WHERE pe.patient = :patient "
                        + "AND pe.encounterDate = :date "
                        + "AND pe.opdDoctor = :staff "
                        + "ORDER BY pe.id DESC";
                PatientEncounter encounter = patientEncounterFacade.findFirstByJpql(encounterJpql, params, TemporalType.DATE);
                if (encounter != null) {
                    token.setPatientEncounter(encounter);
                }
            }
        }

        toCompleteTokens = new ArrayList<>();
        completedTokens = new ArrayList<>();

        for (Token token : allTokens) {
            if (token.isCompleted()) {
                completedTokens.add(token);
            } else {
                toCompleteTokens.add(token);
            }
        }
    }

    public String navigateToEmrVisit() {
        if (currentToken == null) {
            JsfUtil.addErrorMessage("No token selected");
            return "";
        }
        
        // Set up patient encounter for EMR
        if (currentToken.getPatientEncounter() == null) {
            // Create new encounter
            PatientEncounter encounter = new PatientEncounter();
            encounter.setPatient(currentToken.getPatient());
            encounter.setOpdDoctor(currentToken.getStaff());
            encounter.setEncounterType(EncounterType.Opd);
            encounter.setEncounterDate(new Date());
            encounter.setCreatedAt(new Date());
            encounter.setCreater(sessionController.getLoggedUser());
            currentToken.setPatientEncounter(encounter);
            tokenFacade.edit(currentToken);
        }
        
        patientEncounterController.setCurrent(currentToken.getPatientEncounter());
        patientEncounterController.setStartedEncounter(currentToken.getPatientEncounter());
        patientEncounterController.fillCurrentPatientLists(currentToken.getPatient());
        patientEncounterController.fillCurrentEncounterLists(currentToken.getPatientEncounter());
        
        return "/emr/opd_visit?faces-redirect=true";
    }

    public String navigateToViewEmrVisit() {
        if (currentToken == null || currentToken.getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("No visit to view");
            return "";
        }
        
        pastPatientEncounterController.setCurrent(currentToken.getPatientEncounter());
        return "/emr/opd_visit_view?faces-redirect=true";
    }

    public String navigateToPharmacy() {
        if (currentToken == null || currentToken.getPatientEncounter() == null) {
            JsfUtil.addErrorMessage("No visit found");
            return "";
        }
        
        pharmacySaleController.setPatient(currentToken.getPatient());
        pharmacySaleController.setPatientSearchTab(1);
        pharmacySaleController.setOpdEncounterComments(currentToken.getPatientEncounter().getComments());
        pharmacySaleController.setFromOpdEncounter(true);
        pharmacySaleController.setPatientTabId("tabSearchPt");
        
        return "/clinical/clinical_pharmacy_sale?faces-redirect=true";
    }

    public List<Token> getToCompleteTokens() {
        return toCompleteTokens;
    }

    public void setToCompleteTokens(List<Token> toCompleteTokens) {
        this.toCompleteTokens = toCompleteTokens;
    }

    public List<Token> getCompletedTokens() {
        return completedTokens;
    }

    public void setCompletedTokens(List<Token> completedTokens) {
        this.completedTokens = completedTokens;
    }

    private Date sessionDate;

    public Date getSessionDate() {
        if (sessionDate == null) {
            sessionDate = new Date();
        }
        return sessionDate;
    }

    public void setSessionDate(Date sessionDate) {
        this.sessionDate = sessionDate;
    }

    @Inject
    private PatientEncounterController patientEncounterController;

    @Inject
    private PastPatientEncounterController pastPatientEncounterController;

    public PatientEncounterController getPatientEncounterController() {
        return patientEncounterController;
    }

    public void setPatientEncounterController(PatientEncounterController patientEncounterController) {
        this.patientEncounterController = patientEncounterController;
    }

    public PastPatientEncounterController getPastPatientEncounterController() {
        return pastPatientEncounterController;
    }

    public void setPastPatientEncounterController(PastPatientEncounterController pastPatientEncounterController) {
        this.pastPatientEncounterController = pastPatientEncounterController;
    }

}
