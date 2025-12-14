package com.divudi.core.entity;

import com.divudi.core.data.Currency;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/**
 * Entity to store exchange rates for multi-currency support
 * Maintains historical exchange rates for audit purposes
 * @author HMIS Development Team
 */
@Entity
@Table(name = "exchange_rate")
public class ExchangeRate implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_currency", nullable = false)
    private Currency fromCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_currency", nullable = false)
    private Currency toCurrency;

    @Column(name = "exchange_rate", nullable = false)
    private Double exchangeRate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "effective_from", nullable = false)
    private Date effectiveFrom;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "effective_to")
    private Date effectiveTo;

    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private WebUser createdBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt;

    @ManyToOne
    @JoinColumn(name = "last_modified_by")
    private WebUser lastModifiedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_modified_at")
    private Date lastModifiedAt;

    @Column(name = "notes")
    private String notes;

    @Column(name = "retired")
    private Boolean retired;

    @ManyToOne
    @JoinColumn(name = "retired_by")
    private WebUser retiredBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "retired_at")
    private Date retiredAt;

    @Column(name = "retired_reason")
    private String retiredReason;

    public ExchangeRate() {
        this.isActive = true;
        this.retired = false;
        this.createdAt = new Date();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Currency getFromCurrency() {
        return fromCurrency;
    }

    public void setFromCurrency(Currency fromCurrency) {
        this.fromCurrency = fromCurrency;
    }

    public Currency getToCurrency() {
        return toCurrency;
    }

    public void setToCurrency(Currency toCurrency) {
        this.toCurrency = toCurrency;
    }

    public Double getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(Double exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public Date getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Date effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Date getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Date effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public WebUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(WebUser createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public WebUser getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(WebUser lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Date getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(Date lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getRetired() {
        return retired;
    }

    public void setRetired(Boolean retired) {
        this.retired = retired;
    }

    public WebUser getRetiredBy() {
        return retiredBy;
    }

    public void setRetiredBy(WebUser retiredBy) {
        this.retiredBy = retiredBy;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public String getRetiredReason() {
        return retiredReason;
    }

    public void setRetiredReason(String retiredReason) {
        this.retiredReason = retiredReason;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ExchangeRate)) {
            return false;
        }
        ExchangeRate other = (ExchangeRate) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ExchangeRate{" + "id=" + id + ", fromCurrency=" + fromCurrency + 
               ", toCurrency=" + toCurrency + ", exchangeRate=" + exchangeRate + 
               ", effectiveFrom=" + effectiveFrom + '}';
    }
}
