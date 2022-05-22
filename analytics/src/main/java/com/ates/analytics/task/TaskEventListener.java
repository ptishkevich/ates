package com.ates.analytics.task;

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
public class TaskEventListener {
    @Autowired
    TaskRepository taskRepository;

    @KafkaListener(topics = "task-lifecycle", groupId = "analytics_task")
    public void listen(@Payload ConsumerRecord<String, DynamicMessage> data, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key) throws InvalidProtocolBufferException {
        System.out.println("############## Received message key= " + key + " , value= " + data);
        DynamicMessage message = data.value();
        String messageType = message.getDescriptorForType().getFullName();

        switch (messageType) {
            case "task.Added":
                handleTaskAdded(message);
                break;
            case "task.PriceCalculated":
                handleTaskPriceCalculated(message);
                break;
            case "task.Completed":
                handleTaskCompleted(message);
                break;
        }

    }

    private void handleTaskAdded(DynamicMessage message) throws InvalidProtocolBufferException {
        com.ates.messages.Task.Added addedMsg = com.ates.messages.Task.Added
                .newBuilder()
                .build()
                .getParserForType()
                .parseFrom(message.toByteArray());

        int eventVersion = addedMsg.getHeaders().getVersion();
        if (1 == eventVersion) {
            List<Task> existingTasks = taskRepository.findByPublicTaskId(addedMsg.getPublicId());
            if (existingTasks.isEmpty()) {
                Task newTask = new Task();
                newTask.setPublicTaskId(addedMsg.getPublicId());
                newTask.setDescription(addedMsg.getDescription());
                taskRepository.save(newTask);
            }
            else {
                Task task = existingTasks.get(0);
                task.setDescription(addedMsg.getDescription());
                taskRepository.save(task);
            }
        }
        else {
            throw new RuntimeException("Unsupported version " + eventVersion + " of Task.Added event");
        }
    }

    private void handleTaskPriceCalculated(DynamicMessage dynamicMessage) throws InvalidProtocolBufferException {
        com.ates.messages.Task.PriceCalculated taskPriceCalculatedMsg = com.ates.messages.Task.PriceCalculated
                .newBuilder()
                .build()
                .getParserForType()
                .parseFrom(dynamicMessage.toByteArray());

        int eventVersion = taskPriceCalculatedMsg.getHeaders().getVersion();
        if (1 == eventVersion) {
            List<Task> existingTasks = taskRepository.findByPublicTaskId(taskPriceCalculatedMsg.getTaskPublicId());
            if (existingTasks.isEmpty()) {
                Task newTask = new Task();
                newTask.setPublicTaskId(taskPriceCalculatedMsg.getTaskPublicId());
                newTask.setPrice(taskPriceCalculatedMsg.getAmount());
                taskRepository.save(newTask);
            }
            else {
                Task task = existingTasks.get(0);
                task.setPrice(taskPriceCalculatedMsg.getAmount());
                taskRepository.save(task);
            }
        }
        else {
            throw new RuntimeException("Unsupported version " + eventVersion + " Task.PriceCalculated event");
        }
    }

    private void handleTaskCompleted(DynamicMessage message) throws InvalidProtocolBufferException {
        com.ates.messages.Task.Completed completedMsg = com.ates.messages.Task.Completed
                .newBuilder()
                .build()
                .getParserForType()
                .parseFrom(message.toByteArray());

        int eventVersion = completedMsg.getHeaders().getVersion();
        if (1 == eventVersion) {
            List<Task> existingTasks = taskRepository.findByPublicTaskId(completedMsg.getPublicId());
            if(!existingTasks.isEmpty()) {
                Task task = existingTasks.get(0);
                task.setCompletedAt(completedMsg.getCompletedAt());
                taskRepository.save(task);
            }
        }
        else {
            throw new RuntimeException("Unsupported version " + eventVersion + " of Task.Completed event");
        }
    }
}
