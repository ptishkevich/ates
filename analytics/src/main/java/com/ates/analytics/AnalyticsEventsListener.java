package com.ates.analytics;

import com.ates.analytics.task.Task;
import com.ates.analytics.task.TaskBillingOperation;
import com.ates.analytics.task.TaskBillingOperationRepository;
import com.ates.analytics.task.TaskRepository;
import com.ates.messages.Account;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyticsEventsListener {
    @Autowired
    TaskRepository taskRepository;
    @Autowired
    TaskBillingOperationRepository taskBillingOperationRepository;

    @KafkaListener(topics = "account-transactions", groupId = "analytics_account")
    public void listen(@Payload ConsumerRecord<String, DynamicMessage> data, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key) throws InvalidProtocolBufferException {

        System.out.println("############## Received message key= " + key + " , value= " + data);
        DynamicMessage message = data.value();
        String messageType = message.getDescriptorForType().getFullName();

        if ("account.TaskCompletionPaymentApplied".equals(messageType)) {
            handleTaskCompletionPaymentApplied(message);
        }
        else if ("account.TaskAssignmentFeeApplied".equals(messageType)) {
            handleTaskAssignmentFeeApplied(message);
        }
    }

    private void handleTaskCompletionPaymentApplied(DynamicMessage message) throws InvalidProtocolBufferException {
        Account.TaskCompletionPaymentApplied paymentMsg = Account.TaskCompletionPaymentApplied
                .newBuilder()
                .build()
                .getParserForType()
                .parseFrom(message.toByteArray());

        // find or create task
        Task task = getTask(paymentMsg.getTaskPublicId());
        // create record for billing
        TaskBillingOperation operation = new TaskBillingOperation();
        operation.setOperationType(TaskBillingOperation.OperationType.PAYMENT);
        operation.setTaskId(task.getId());
        operation.setAmount(paymentMsg.getAmount());
        operation.setAccountOwnerPublicId(paymentMsg.getAccountOwnerPublicId());
        operation.setPerformedAt(paymentMsg.getPerformedAt());
        taskBillingOperationRepository.save(operation);
    }

    private void handleTaskAssignmentFeeApplied(DynamicMessage message) throws InvalidProtocolBufferException {
        Account.TaskAssignmentFeeApplied assignmentMsg = Account.TaskAssignmentFeeApplied
                .newBuilder()
                .build()
                .getParserForType()
                .parseFrom(message.toByteArray());

        // find or create task
        Task task = getTask(assignmentMsg.getTaskPublicId());
        // create record for billing
        TaskBillingOperation operation = new TaskBillingOperation();
        operation.setOperationType(TaskBillingOperation.OperationType.FEE);
        operation.setTaskId(task.getId());
        operation.setAmount(assignmentMsg.getAmount());
        operation.setAccountOwnerPublicId(assignmentMsg.getAccountOwnerPublicId());
        operation.setPerformedAt(assignmentMsg.getPerformedAt());
        taskBillingOperationRepository.save(operation);
    }

    private Task getTask(String publicTaskId) {
        List<Task> tasks = taskRepository.findByPublicTaskId(publicTaskId);
        if (tasks.isEmpty()) {
            Task newTask = new Task();
            newTask.setPublicTaskId(publicTaskId);
            return taskRepository.save(newTask);
        }
        return tasks.get(0);
    }
}
