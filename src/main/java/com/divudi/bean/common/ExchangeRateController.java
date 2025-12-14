package com.divudi.bean.common;

import com.divudi.core.data.Currency;
import com.divudi.core.entity.ExchangeRate;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Controller for managing exchange rates across the system
 * @author HMIS Development Team
 */
@Named
@SessionScoped
public class ExchangeRateController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ExchangeRateFacade facade;

    @Inject
    private SessionController sessionController;

    private ExchangeRate current;
    private List<ExchangeRate> items;
    private List<ExchangeRate> activeRates;
    private Currency defaultCurrency = Currency.UGX;

    public ExchangeRateController() {
    }

    public void prepareAdd() {
        current = new ExchangeRate();
        current.setFromCurrency(Currency.UGX);
        current.setEffectiveFrom(new Date());
        current.setIsActive(true);
    }

    public void prepareEdit(ExchangeRate rate) {
        if (rate == null) {
            JsfUtil.addErrorMessage("Please select an exchange rate to edit");
            return;
        }
        current = rate;
    }

    public String save() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return null;
        }

        if (current.getFromCurrency() == null || current.getToCurrency() == null) {
            JsfUtil.addErrorMessage("Please select both currencies");
            return null;
        }

        if (current.getFromCurrency().equals(current.getToCurrency())) {
            JsfUtil.addErrorMessage("From and To currencies cannot be the same");
            return null;
        }

        if (current.getExchangeRate() == null || current.getExchangeRate() <= 0) {
            JsfUtil.addErrorMessage("Please enter a valid exchange rate");
            return null;
        }

        if (current.getEffectiveFrom() == null) {
            current.setEffectiveFrom(new Date());
        }

        try {
            if (current.getId() == null) {
                current.setCreatedBy(sessionController.getLoggedUser());
                current.setCreatedAt(new Date());
                
                // Deactivate existing rate for the same currency pair if setting as active
                if (current.getIsActive()) {
                    deactivateExistingRates(current.getFromCurrency(), current.getToCurrency());
                }
                
                facade.create(current);
                JsfUtil.addSuccessMessage("Exchange Rate Created Successfully");
            } else {
                current.setLastModifiedBy(sessionController.getLoggedUser());
                current.setLastModifiedAt(new Date());
                
                // Deactivate other rates if this one is being set as active
                if (current.getIsActive()) {
                    deactivateExistingRates(current.getFromCurrency(), current.getToCurrency());
                }
                
                facade.edit(current);
                JsfUtil.addSuccessMessage("Exchange Rate Updated Successfully");
            }
            items = null;
            activeRates = null;
            return "exchange_rate?faces-redirect=true";
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error saving exchange rate: " + e.getMessage());
            return null;
        }
    }

    private void deactivateExistingRates(Currency from, Currency to) {
        String jpql = "SELECT e FROM ExchangeRate e WHERE e.fromCurrency = :from "
                + "AND e.toCurrency = :to AND e.isActive = true AND e.retired = false "
                + "AND e.id != :currentId";
        HashMap params = new HashMap();
        params.put("from", from);
        params.put("to", to);
        params.put("currentId", current.getId() != null ? current.getId() : 0L);
        
        List<ExchangeRate> existingRates = facade.findByJpql(jpql, params);
        
        for (ExchangeRate rate : existingRates) {
            rate.setIsActive(false);
            rate.setEffectiveTo(new Date());
            rate.setLastModifiedBy(sessionController.getLoggedUser());
            rate.setLastModifiedAt(new Date());
            facade.edit(rate);
        }
    }

    public String delete() {
        if (current == null) {
            JsfUtil.addErrorMessage("Please select an exchange rate to delete");
            return null;
        }

        try {
            current.setRetired(true);
            current.setRetiredBy(sessionController.getLoggedUser());
            current.setRetiredAt(new Date());
            current.setIsActive(false);
            facade.edit(current);
            JsfUtil.addSuccessMessage("Exchange Rate Deleted Successfully");
            items = null;
            activeRates = null;
            return "exchange_rate?faces-redirect=true";
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error deleting exchange rate: " + e.getMessage());
            return null;
        }
    }

    public void deactivate(ExchangeRate rate) {
        if (rate == null) {
            JsfUtil.addErrorMessage("Please select an exchange rate");
            return;
        }

        try {
            rate.setIsActive(false);
            rate.setEffectiveTo(new Date());
            rate.setLastModifiedBy(sessionController.getLoggedUser());
            rate.setLastModifiedAt(new Date());
            facade.edit(rate);
            JsfUtil.addSuccessMessage("Exchange Rate Deactivated");
            items = null;
            activeRates = null;
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error deactivating: " + e.getMessage());
        }
    }

    public void activate(ExchangeRate rate) {
        if (rate == null) {
            JsfUtil.addErrorMessage("Please select an exchange rate");
            return;
        }

        try {
            // Deactivate other rates for same currency pair
            deactivateExistingRates(rate.getFromCurrency(), rate.getToCurrency());
            
            rate.setIsActive(true);
            rate.setEffectiveTo(null);
            rate.setEffectiveFrom(new Date());
            rate.setLastModifiedBy(sessionController.getLoggedUser());
            rate.setLastModifiedAt(new Date());
            facade.edit(rate);
            JsfUtil.addSuccessMessage("Exchange Rate Activated");
            items = null;
            activeRates = null;
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error activating: " + e.getMessage());
        }
    }

    /**
     * Get active exchange rate for conversion
     */
    public Double getActiveRate(Currency from, Currency to) {
        if (from == null || to == null) {
            return null;
        }
        
        if (from.equals(to)) {
            return 1.0;
        }

        String jpql = "SELECT e FROM ExchangeRate e WHERE e.fromCurrency = :from "
                + "AND e.toCurrency = :to AND e.isActive = true AND e.retired = false "
                + "ORDER BY e.effectiveFrom DESC";
        HashMap params = new HashMap();
        params.put("from", from);
        params.put("to", to);

        List<ExchangeRate> rates = facade.findByJpql(jpql, params, 1);
        
        if (rates != null && !rates.isEmpty()) {
            return rates.get(0).getExchangeRate();
        }
        
        return null;
    }

    /**
     * Convert amount from one currency to another using active rate
     */
    public Double convertAmount(Double amount, Currency from, Currency to) {
        if (amount == null || from == null || to == null) {
            return amount;
        }
        
        if (from.equals(to)) {
            return amount;
        }

        Double rate = getActiveRate(from, to);
        if (rate != null) {
            return amount * rate;
        }

        // Try reverse conversion
        Double reverseRate = getActiveRate(to, from);
        if (reverseRate != null && reverseRate != 0) {
            return amount / reverseRate;
        }

        JsfUtil.addInfoMessage("No exchange rate found for " + from + " to " + to);
        return amount;
    }

    public List<ExchangeRate> getItems() {
        if (items == null) {
            String jpql = "SELECT e FROM ExchangeRate e WHERE e.retired = false "
                    + "ORDER BY e.isActive DESC, e.effectiveFrom DESC";
            items = facade.findByJpql(jpql);
        }
        return items;
    }

    public List<ExchangeRate> getActiveRates() {
        if (activeRates == null) {
            String jpql = "SELECT e FROM ExchangeRate e WHERE e.isActive = true "
                    + "AND e.retired = false ORDER BY e.fromCurrency, e.toCurrency";
            activeRates = facade.findByJpql(jpql);
        }
        return activeRates;
    }

    public Currency[] getCurrencies() {
        return Currency.values();
    }

    public ExchangeRate getCurrent() {
        if (current == null) {
            current = new ExchangeRate();
        }
        return current;
    }

    public void setCurrent(ExchangeRate current) {
        this.current = current;
    }

    public void setItems(List<ExchangeRate> items) {
        this.items = items;
    }

    public Currency getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(Currency defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public ExchangeRateFacade getFacade() {
        return facade;
    }
}
