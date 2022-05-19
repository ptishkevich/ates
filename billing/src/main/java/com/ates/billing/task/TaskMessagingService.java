package com.ates.billing.task;

import com.ates.billing.account.BillingLogic;
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

import java.util.UUID;

@Service
public class TaskMessagingService {
    @Autowired
    BillingLogic billingLogic;

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

        billingLogic.calculateTaskPrices(taskPublicId, addedMsg.getDescription());
    }

    private void handleTaskAssigned(DynamicMessage message) throws InvalidProtocolBufferException {
        Task.Assigned assignedMsg = Task.Assigned
                .newBuilder()
                .build()
                .getParserForType()
                .parseFrom(message.toByteArray());

        UUID assigneePublicId = UUID.fromString(assignedMsg.getAssigneeId());
        UUID taskPublicId = UUID.fromString(assignedMsg.getPublicId());

        billingLogic.applyFeeForTaskAssignment(assigneePublicId, taskPublicId);
    }

    private void handleTaskCompleted(DynamicMessage message) throws InvalidProtocolBufferException {
        Task.Completed taskCompletedMsg = Task.Completed
                .newBuilder()
                .build()
                .getParserForType()
                .parseFrom(message.toByteArray());

        UUID completedByPublicId = UUID.fromString(taskCompletedMsg.getCompletedById());
        UUID taskPublicId = UUID.fromString(taskCompletedMsg.getPublicId());

        billingLogic.applyPaymentForTaskCompletion(completedByPublicId, taskPublicId);
    }
}
