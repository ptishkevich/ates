package com.ates.billing.account;

import com.ates.billing.FakeEmailService;
import com.ates.billing.profile.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.stream.StreamSupport;

@Component
public class BillingLogic {

    @Autowired
    AccountRepository accountRepository;
    @Autowired
    FakeEmailService emailService;
    @Autowired
    ProfileRepository profileRepository;
    @Autowired
    BillingCycleRepository billingCycleRepository;

    public void closeBillingCycle() {
        // determine beginning and the end of billing cycle
        //LocalDate today = LocalDate.now();
        //today.atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli();
        long cycleEnd = System.currentTimeMillis();

        //today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        BillingCycle currentBillingCycle = StreamSupport
                .stream(billingCycleRepository.findAll().spliterator(), false)
                .filter(billingCycle -> BillingCycle.Status.OPEN == billingCycle.getStatus())
                .findFirst()
                .orElseGet(() -> new BillingCycle(0, cycleEnd, BillingCycle.Status.OPEN));

        long cycleBeginning = currentBillingCycle.getBeginningTimestamp();

        // perform payout for each account
        for (Account account : accountRepository.findAll()) {
            performPayout(account, cycleBeginning, cycleEnd);
        }

        // mark current billing cycle as closed and open the next one
        currentBillingCycle.setEndTimestamp(cycleEnd);
        currentBillingCycle.setStatus(BillingCycle.Status.CLOSED);
        billingCycleRepository.save(currentBillingCycle);
        BillingCycle nextBillingCycle = new BillingCycle(currentBillingCycle.getEndTimestamp()+1, -1, BillingCycle.Status.OPEN);
        billingCycleRepository.save(nextBillingCycle);

    }

    private void performPayout(Account account, long cycleBegin, long cycleEnd) {
        int balance = account
                .getAuditLog()
                .stream()
                .filter(accountLogRecord -> accountLogRecord.getTimestamp() > cycleBegin && accountLogRecord.getTimestamp() < cycleEnd)
                .map(accountLogRecord -> {
                    if (AccountLogRecord.OperationType.CREDIT == accountLogRecord.getOperationType()) {
                        return accountLogRecord.getAmount();
                    }
                    else if(AccountLogRecord.OperationType.DEBIT == accountLogRecord.getOperationType()) {
                        return -accountLogRecord.getAmount();
                    }
                    return 0;
                })
                .reduce(0 , Integer::sum);

        if (balance > 0) {
            // 2 options here:
            // - send sync request to the payment service (that makes call to the bank, payment gateway, etc.)
            // - send async event to the payment service, in this case we'll need to create consumer that will listen event with result of operation
            // and in case of success, it will perform following:

            // withdraw balance amount from account
            account.addAuditLogRecord(new AccountLogRecord(balance, AccountLogRecord.OperationType.DEBIT, "Payout", System.currentTimeMillis()));

            // send email to the user
            profileRepository
                    .findById(account.getProfileId())
                    .ifPresent(
                            profile -> emailService.sendPayoutEmail(profile, balance)
                    );
        }
    }
}
