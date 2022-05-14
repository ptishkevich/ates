package com.ates.billing.task;

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

import java.util.Random;
import java.util.UUID;

@Service
public class TaskMessagingService {

    private Random random = new Random();

    @Autowired
    TaskRepository taskRepository;

    @KafkaListener(topics = "task-lifecycle", groupId = "billing_task")
    public void listen(@Payload ConsumerRecord<String, DynamicMessage> data, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key) throws InvalidProtocolBufferException {

        System.out.println("############## Received message key= " + key + " , value= " + data);
        DynamicMessage message = data.value();
        String messageType = message.getDescriptorForType().getFullName();

        if ("task.Added".equals(messageType)) {
            handleTaskAdded(message);
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
        task.setCreditAmount(random.nextInt(21) + 20);
        task.setDebitAmount(random.nextInt(11) + 10);

        taskRepository.save(task);
    }

}
