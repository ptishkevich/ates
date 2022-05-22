package com.ates.billing.account;

import com.ates.event.EventUtils;
import com.ates.messages.Account;
import com.ates.messages.EventHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AccountEventSender {
    @Autowired
    private KafkaTemplate<String, Account.TaskCompletionPaymentApplied> taskPaymentTemplate;
    @Autowired
    private KafkaTemplate<String, Account.TaskAssignmentFeeApplied> taskFeeTemplate;

    private static final String ACCOUNT_TRANSACTIONS_TOPIC_NAME = "account-transactions";

    public void sendTaskPaymentAppliedEvent(String accountOwnerId, String taskId, int amount, long performedAt) {
        EventHeaders eventHeaders = EventUtils.getEventHeaders(
                1,
                Account.TaskCompletionPaymentApplied.getDescriptor().getFullName(),
                this.getClass().getSimpleName()
        );

        Account.TaskCompletionPaymentApplied taskPaymentMsg = Account.TaskCompletionPaymentApplied
                .newBuilder()
                .setHeaders(eventHeaders)
                .setAccountOwnerPublicId(accountOwnerId)
                .setTaskPublicId(taskId)
                .setAmount(amount)
                .setPerformedAt(performedAt)
                .build();

        taskPaymentTemplate.send(ACCOUNT_TRANSACTIONS_TOPIC_NAME, accountOwnerId, taskPaymentMsg);
    }

    public void sendTaskFeeAppliedEvent(String accountOwnerId, String taskId, int amount, long performedAt) {
        EventHeaders eventHeaders = EventUtils.getEventHeaders(
                1,
                Account.TaskAssignmentFeeApplied.getDescriptor().getFullName(),
                this.getClass().getSimpleName()
        );

        Account.TaskAssignmentFeeApplied taskFeeMsg = Account.TaskAssignmentFeeApplied
                .newBuilder()
                .setHeaders(eventHeaders)
                .setAccountOwnerPublicId(accountOwnerId)
                .setTaskPublicId(taskId)
                .setAmount(amount)
                .setPerformedAt(performedAt)
                .build();

        taskFeeTemplate.send(ACCOUNT_TRANSACTIONS_TOPIC_NAME, accountOwnerId, taskFeeMsg);
    }
}
