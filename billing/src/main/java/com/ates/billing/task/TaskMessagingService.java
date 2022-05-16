package com.ates.billing.task;

import com.ates.billing.account.Account;
import com.ates.billing.account.AccountLogRecord;
import com.ates.billing.account.AccountRepository;
import com.ates.billing.profile.Profile;
import com.ates.billing.profile.ProfileRepository;
import com.ates.messages.Task;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
public class TaskMessagingService {

    private final Random random = new Random();

    @Autowired
    TaskRepository taskRepository;
    @Autowired
    ProfileRepository profileRepository;
    @Autowired
    AccountRepository accountRepository;

    @KafkaListener(topics = "task-lifecycle", groupId = "billing_task")
    public void listen(@Payload ConsumerRecord<String, DynamicMessage> data, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key) throws InvalidProtocolBufferException {

        System.out.println("############## Received message key= " + key + " , value= " + data);
        DynamicMessage message = data.value();
        String messageType = message.getDescriptorForType().getFullName();

        switch (messageType) {
            case "task.Added":
                handleTaskAdded(message);
                break;
            case "task.Assigned":
                handleTaskAssigned(message);
                break;
            case "task.Completed":
                handleTaskCompleted(message);
                break;
        }
    }

    private void handleTaskAdded(DynamicMessage message) throws InvalidProtocolBufferException {
        Task.Added addedMsg = Task.Added
                .newBuilder()
                .build()
                .getParserForType()
                .parseFrom(message.toByteArray());

        UUID taskPublicId = UUID.fromString(addedMsg.getPublicId());

        com.ates.billing.task.Task task = new com.ates.billing.task.Task();
        task.setPublicId(taskPublicId);
        task.setDescription(addedMsg.getDescription());
        task.setCreditAmount(random.nextInt(21) + 20);
        task.setDebitAmount(random.nextInt(11) + 10);

        taskRepository.save(task);
    }

    private void handleTaskAssigned(DynamicMessage message) throws InvalidProtocolBufferException {
        Task.Assigned assignedMsg = Task.Assigned
                .newBuilder()
                .build()
                .getParserForType()
                .parseFrom(message.toByteArray());

        UUID assigneePublicId = UUID.fromString(assignedMsg.getAssigneeId());
        UUID taskPublicId = UUID.fromString(assignedMsg.getPublicId());

        createAuditLogRecord(assigneePublicId, taskPublicId, AccountLogRecord.OperationType.DEBIT);
    }

    private void handleTaskCompleted(DynamicMessage message) throws InvalidProtocolBufferException {
        Task.Completed taskCompletedMsg = Task.Completed
                .newBuilder()
                .build()
                .getParserForType()
                .parseFrom(message.toByteArray());

        UUID completedByPublicId = UUID.fromString(taskCompletedMsg.getCompletedById());
        UUID taskPublicId = UUID.fromString(taskCompletedMsg.getPublicId());

        createAuditLogRecord(completedByPublicId, taskPublicId, AccountLogRecord.OperationType.CREDIT);
    }

    private void createAuditLogRecord(UUID profilePublicId, UUID taskPublicId, AccountLogRecord.OperationType operationType) {
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

        account
                .getAuditLog()
                .add(new AccountLogRecord(
                        amount,
                        operationType,
                        task.getDescription(),
                        System.currentTimeMillis()
                ));

        accountRepository.save(account);
    }

    private Profile findProfile(UUID profilePublicId) {
        Optional<Profile> profileOptional = StreamSupport
                .stream(profileRepository.findAll().spliterator(), false)
                .filter(profile -> profilePublicId.equals(profile.getPublicId()))
                .findFirst();

        if (profileOptional.isEmpty()) {
            throw new RuntimeException("Profile " + profilePublicId + " not found during assignment fee calculation");
        }

        return profileOptional.get();
    }

    private com.ates.billing.task.Task findTask(UUID taskPublicId) {
        Optional<com.ates.billing.task.Task> taskOptional = StreamSupport
                .stream(taskRepository.findAll().spliterator(), false)
                .filter(task -> taskPublicId.equals(task.getPublicId()))
                .findFirst();

        if (taskOptional.isEmpty()) {
            throw new RuntimeException("Task " + taskPublicId + " not found during assignment fee calculation");
        }
        return taskOptional.get();
    }

}
