package com.ates.billing.account;

import com.ates.billing.BillingEventSender;
import com.ates.billing.FakeEmailService;
import com.ates.billing.profile.Profile;
import com.ates.billing.profile.ProfileRepository;
import com.ates.billing.task.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
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
    @Autowired
    TaskRepository taskRepository;
    @Autowired
    AccountEventSender accountEventSender;
    @Autowired
    BillingEventSender billingEventSender;

    private final Random random = new Random();

    public void applyFeeForTaskAssignment(UUID assigneePublicId, UUID taskPublicId) {
        AccountLogRecord auditLogRecord = createAuditLogRecord(assigneePublicId, taskPublicId, AccountLogRecord.OperationType.DEBIT);
        accountEventSender.sendTaskFeeAppliedEvent(
                assigneePublicId.toString(),
                taskPublicId.toString(),
                auditLogRecord.getAmount(),
                auditLogRecord.getTimestamp()
        );
    }

    public void applyPaymentForTaskCompletion(UUID completedByPublicId, UUID taskPublicId) {
        AccountLogRecord auditLogRecord = createAuditLogRecord(completedByPublicId, taskPublicId, AccountLogRecord.OperationType.CREDIT);
        accountEventSender.sendTaskPaymentAppliedEvent(
                completedByPublicId.toString(),
                taskPublicId.toString(),
                auditLogRecord.getAmount(),
                auditLogRecord.getTimestamp()
        );
    }

    public void calculateTaskPrices(UUID taskPublicId, String description) {
        com.ates.billing.task.Task task = new com.ates.billing.task.Task();
        task.setPublicId(taskPublicId);
        task.setDescription(description);
        int creditAmount = random.nextInt(21) + 20;
        int debitAmount = random.nextInt(11) + 10;
        task.setCreditAmount(creditAmount);
        task.setDebitAmount(debitAmount);

        taskRepository.save(task);
        billingEventSender.sendTaskPriceCalculatedEvent(taskPublicId.toString(), creditAmount);
    }

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

    private AccountLogRecord createAuditLogRecord(UUID profilePublicId, UUID taskPublicId, AccountLogRecord.OperationType operationType) {
        Profile profile = findProfile(profilePublicId);

        // update or create account
        Account account = StreamSupport
                .stream(accountRepository.findAll().spliterator(), false)
                .filter(acc -> profile.getId().equals(acc.getProfileId()))
                .findFirst()
                .orElseGet(() -> {
                    Account newAccount = new Account();
                    newAccount.setProfileId(profile.getId());
                    newAccount.setAuditLog(new ArrayList<>());
                    return accountRepository.save(newAccount);
                });

        com.ates.billing.task.Task task = findTask(taskPublicId);
        int amount = AccountLogRecord.OperationType.DEBIT == operationType
                ? task.getDebitAmount()
                : task.getCreditAmount();


        AccountLogRecord accountLogRecord = new AccountLogRecord(
                amount,
                operationType,
                task.getDescription(),
                System.currentTimeMillis()
        );
        account
                .getAuditLog()
                .add(accountLogRecord);

        return accountLogRecord;
    }

    private Profile findProfile(UUID profilePublicId) {
        Optional<Profile> profileOptional = StreamSupport
                .stream(profileRepository.findAll().spliterator(), false)
                .filter(profile -> profilePublicId.equals(profile.getPublicId()))
                .findFirst();

        if (profileOptional.isEmpty()) {
            throw new RuntimeException("Profile " + profilePublicId + " not found");
        }

        return profileOptional.get();
    }

    private com.ates.billing.task.Task findTask(UUID taskPublicId) {
        Optional<com.ates.billing.task.Task> taskOptional = StreamSupport
                .stream(taskRepository.findAll().spliterator(), false)
                .filter(task -> taskPublicId.equals(task.getPublicId()))
                .findFirst();

        if (taskOptional.isEmpty()) {
            throw new RuntimeException("Task " + taskPublicId + " not found");
        }
        return taskOptional.get();
    }

}
