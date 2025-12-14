package com.divudi.bean.common;

import com.divudi.core.entity.ExchangeRate;
import com.divudi.core.facade.AbstractFacade;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Facade for ExchangeRate entity
 * @author HMIS Development Team
 */
@Stateless
public class ExchangeRateFacade extends AbstractFacade<ExchangeRate> {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ExchangeRateFacade() {
        super(ExchangeRate.class);
    }
}
