package com.ates.billing.scheduler;

import com.ates.billing.account.BillingLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class should emulate scheduler service. We need ability to trigger it at any time. <br>
 * In real system the same command should be triggered by any kind of cron job.
 */
@RestController
public class FakeSchedulerService {
    @Autowired
    BillingLogic billingLogic;

    @PostMapping("/close-billing-cycle")
    public void performPayout() {
        billingLogic.closeBillingCycle();
    }

}
