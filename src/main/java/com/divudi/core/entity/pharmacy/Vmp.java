/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity.pharmacy;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 *
 * @author buddhika
 */
@Entity
public class Vmp extends PharmaceuticalItem implements Serializable {

    @ManyToOne
    private MeasurementUnit doseUnit;

    public MeasurementUnit getDoseUnit() {
        return doseUnit;
    }

    public void setDoseUnit(MeasurementUnit doseUnit) {
        this.doseUnit = doseUnit;
    }

}
