package com.divudi.bean.clinical;

import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Nurse Triage Station Controller
 */
@Named
@SessionScoped
public class NurseTriageController implements Serializable {

    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    
    @Inject
    private SessionController sessionController;

    private List<PatientEncounter> queuedPatients;
    private List<PatientEncounter> triagedPatients;
    private PatientEncounter selectedEncounter;
    private Date fromDate;
    private Date toDate;

    public NurseTriageController() {
    }

    public void loadQueuedPatients() {
        System.out.println("=== loadQueuedPatients START ===");
        
        if (fromDate == null) {
            fromDate = com.divudi.core.util.CommonFunctions.getStartOfDay(new Date());
        }
        if (toDate == null) {
            toDate = com.divudi.core.util.CommonFunctions.getEndOfDay(new Date());
        }
        
        System.out.println("Date Range: " + fromDate + " to " + toDate);

        // Load all OPD patients for the day - split them based on data completeness in UI
        String jpql = "SELECT e FROM PatientEncounter e "
                + "WHERE e.retired = false "
                + "AND e.createdAt BETWEEN :fromDate AND :toDate "
                + "AND e.encounterType = :encType "
                + "ORDER BY e.createdAt ASC";

        Map<String, Object> params = new HashMap<>();
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);
        params.put("encType", com.divudi.core.data.EncounterType.Opd);
        
        List<PatientEncounter> allPatients = patientEncounterFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        
        // Split into queued and triaged based on data completeness
        queuedPatients = new ArrayList<>();
        triagedPatients = new ArrayList<>();
        
        for (PatientEncounter e : allPatients) {
            // Consider triaged if triageCompleted flag is set
            if (e.isTriageCompleted()) {
                triagedPatients.add(e);
            } else {
                queuedPatients.add(e);
            }
        }
        
        System.out.println("Found " + queuedPatients.size() + " patients waiting for triage");
        System.out.println("Found " + triagedPatients.size() + " patients already triaged");

        
        System.out.println("=== loadQueuedPatients END ===");
    }

    public String navigateToTriage(PatientEncounter encounter) {
        System.out.println("=== navigateToTriage START ===");
        System.out.println("Encounter ID: " + (encounter != null ? encounter.getId() : "NULL"));
        
        if (encounter == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return "";
        }
        
        selectedEncounter = encounter;
        System.out.println("Selected encounter for patient: " + encounter.getPatient().getPerson().getName());
        return "/nurse/nurse_triage?faces-redirect=true";
    }

    public void saveTriageInformation() {
        System.out.println("=== saveTriageInformation START ===");
        
        if (selectedEncounter == null) {
            JsfUtil.addErrorMessage("No patient encounter selected");
            return;
        }

        try {
            patientEncounterFacade.edit(selectedEncounter);
            
            JsfUtil.addSuccessMessage("Triage information saved successfully");
            System.out.println("Triage saved for patient: " + selectedEncounter.getPatient().getPerson().getName());
            
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving triage: " + e.getMessage());
            System.out.println("Error saving triage: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== saveTriageInformation END ===");
    }

    public void completeTriageAndReturnToQueue() {
        System.out.println("=== completeTriageAndReturnToQueue START ===");
        
        if (selectedEncounter == null) {
            JsfUtil.addErrorMessage("No patient encounter selected");
            return;
        }
        
        try {
            // Mark triage as completed and record who did it
            selectedEncounter.setTriageCompleted(true);
            selectedEncounter.setTriageBy(sessionController.getLoggedUser());
            selectedEncounter.setTriageAt(new Date());
            patientEncounterFacade.edit(selectedEncounter);
            
            JsfUtil.addSuccessMessage("Triage completed! Patient sent to doctor queue.");
            System.out.println("Triage completed for patient: " + selectedEncounter.getPatient().getPerson().getName());
            System.out.println("Triaged by: " + sessionController.getLoggedUser().getWebUserPerson().getName());
            
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error completing triage: " + e.getMessage());
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
        
        selectedEncounter = null;
        loadQueuedPatients();
        
        System.out.println("=== completeTriageAndReturnToQueue END ===");
    }

    public String navigateToNurseStation() {
        loadQueuedPatients();
        return "/nurse/nurse_station?faces-redirect=true";
    }

    // Getters and Setters
    public List<PatientEncounter> getQueuedPatients() {
        if (queuedPatients == null) {
            queuedPatients = new ArrayList<>();
            loadQueuedPatients();
        }
        return queuedPatients;
    }

    public void setQueuedPatients(List<PatientEncounter> queuedPatients) {
        this.queuedPatients = queuedPatients;
    }

    public PatientEncounter getSelectedEncounter() {
        return selectedEncounter;
    }

    public void setSelectedEncounter(PatientEncounter selectedEncounter) {
        this.selectedEncounter = selectedEncounter;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = com.divudi.core.util.CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = com.divudi.core.util.CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<PatientEncounter> getTriagedPatients() {
        if (triagedPatients == null) {
            triagedPatients = new ArrayList<>();
        }
        return triagedPatients;
    }

    public void setTriagedPatients(List<PatientEncounter> triagedPatients) {
        this.triagedPatients = triagedPatients;
    }
}
