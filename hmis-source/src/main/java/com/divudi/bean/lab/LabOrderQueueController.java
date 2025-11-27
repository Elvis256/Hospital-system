/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.clinical.ClinicalFindingValueType;
import com.divudi.core.entity.clinical.ClinicalFindingValue;
import com.divudi.core.facade.ClinicalFindingValueFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
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
 * Controller for Lab Order Queue
 * Shows tests ordered by doctors with payment status
 * 
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class LabOrderQueueController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ClinicalFindingValueFacade clinicalFindingValueFacade;

    @Inject
    private SessionController sessionController;

    // Properties
    private Date fromDate;
    private Date toDate;
    private String statusFilter = "ALL";
    private List<ClinicalFindingValue> orders;

    /**
     * Constructor - Initialize dates
     */
    public LabOrderQueueController() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        fromDate = cal.getTime();
        
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        toDate = cal.getTime();
    }

    /**
     * Load orders based on filters
     */
    public void loadOrders() {
        orders = new ArrayList<>();
        
        if (fromDate == null || toDate == null) {
            return;
        }

        String jpql = "SELECT cfv FROM ClinicalFindingValue cfv "
                + "WHERE cfv.retired = false "
                + "AND cfv.clinicalFindingValueType = :valueType "
                + "AND cfv.createdAt BETWEEN :fromDate AND :toDate "
                + "AND cfv.itemValue IS NOT NULL ";

        Map<String, Object> params = new HashMap<>();
        params.put("valueType", ClinicalFindingValueType.VisitInvestigation);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        // Apply status filters
        switch (statusFilter) {
            case "NOT_PAID":
                jpql += "AND cfv.patientInvestigation IS NULL ";
                break;
            case "PAID_NOT_SENT":
                jpql += "AND cfv.patientInvestigation IS NOT NULL "
                        + "AND cfv.patientInvestigation.collected = false ";
                break;
            case "SENT_TO_LAB":
                jpql += "AND cfv.patientInvestigation IS NOT NULL "
                        + "AND cfv.patientInvestigation.collected = true ";
                break;
            default:
                // ALL - no additional filter
                break;
        }

        jpql += "ORDER BY cfv.createdAt DESC";

        orders = clinicalFindingValueFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    /**
     * Get count of orders by status
     */
    public Long getCountByStatus(String status) {
        if (fromDate == null || toDate == null) {
            return 0L;
        }

        String jpql = "SELECT COUNT(cfv) FROM ClinicalFindingValue cfv "
                + "WHERE cfv.retired = false "
                + "AND cfv.clinicalFindingValueType = :valueType "
                + "AND cfv.createdAt BETWEEN :fromDate AND :toDate "
                + "AND cfv.itemValue IS NOT NULL ";

        Map<String, Object> params = new HashMap<>();
        params.put("valueType", ClinicalFindingValueType.VisitInvestigation);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        switch (status) {
            case "NOT_PAID":
                jpql += "AND cfv.patientInvestigation IS NULL";
                break;
            case "PAID":
                jpql += "AND cfv.patientInvestigation IS NOT NULL "
                        + "AND cfv.patientInvestigation.collected = false";
                break;
            case "SENT_TO_LAB":
                jpql += "AND cfv.patientInvestigation IS NOT NULL "
                        + "AND cfv.patientInvestigation.collected = true";
                break;
        }

        return clinicalFindingValueFacade.countByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    // Getters and Setters
    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
    }

    public List<ClinicalFindingValue> getOrders() {
        if (orders == null) {
            loadOrders();
        }
        return orders;
    }

    public void setOrders(List<ClinicalFindingValue> orders) {
        this.orders = orders;
    }
}
