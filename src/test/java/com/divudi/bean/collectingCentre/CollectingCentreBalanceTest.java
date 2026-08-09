package com.divudi.bean.collectingCentre;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.divudi.bean.collectingCentre.CollectingCentreBillController.availableCollectingCentreBalance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the collecting-centre available-balance calculation.
 *
 * Regression guard for the fix that removed an erroneous outer Math.abs which
 * let an over-limit centre keep billing.
 */
@DisplayName("Collecting centre available balance")
public class CollectingCentreBalanceTest {

    @Test
    @DisplayName("an over-limit centre has a negative available balance and is blocked")
    void overLimitIsNegative() {
        // owes 10,000 with a 5,000 credit line -> 5,000 over the limit
        double available = availableCollectingCentreBalance(-10000.0, 5000.0);
        assertEquals(-5000.0, available, 0.0001);
        // any positive fee total therefore exceeds the available balance -> block
        assertTrue(available < 100.0, "over-limit centre must be blocked, not allowed to bill");
    }

    @Test
    @DisplayName("a centre within its credit line has headroom")
    void withinLimitHasHeadroom() {
        assertEquals(3000.0, availableCollectingCentreBalance(-2000.0, 5000.0), 0.0001);
    }

    @Test
    @DisplayName("a centre in credit adds its balance to the limit")
    void positiveBalanceAdds() {
        assertEquals(7000.0, availableCollectingCentreBalance(2000.0, 5000.0), 0.0001);
    }

    @Test
    @DisplayName("a negative stored credit limit is normalised to positive")
    void negativeLimitNormalised() {
        assertEquals(6000.0, availableCollectingCentreBalance(1000.0, -5000.0), 0.0001);
    }
}
