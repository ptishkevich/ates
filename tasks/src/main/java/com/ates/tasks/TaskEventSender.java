package com.ates.tasks;

import com.ates.messages.Task;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TaskEventSender {
    @Autowired
    private KafkaTemplate<String, Task.Added> taskAddedTemplate;
    @Autowired
    private KafkaTemplate<String, Task.Assigned> taskAssignedTemplate;

    private static final String TASK_LIFECYCLE_TOPIC_NAME = "task-lifecycle";

    @Bean
    public NewTopic topic() {
        return TopicBuilder
                .name("topic1")
                .build();
    }

    public void sendTaskAddedEvent(com.ates.tasks.Task task) {
        String publicId = task.getId().toString();
        Task.Added taskAddedMsg = Task.Added
                .newBuilder()
                .setPublicId(publicId)
                .setDescription(task.getDescription())
                .build();
        taskAddedTemplate.send(TASK_LIFECYCLE_TOPIC_NAME, publicId, taskAddedMsg);
    }

    public void sendTaskAssignedEvent(com.ates.tasks.Task task) {
        String publicId = task.getId().toString();
        String assigneeId = task.getAssigneeId().toString();
        Task.Assigned taskAssignedMsg = Task.Assigned
                .newBuilder()
                .setPublicId(publicId)
                .setAssigneeId(assigneeId)
                .build();
        taskAssignedTemplate.send(TASK_LIFECYCLE_TOPIC_NAME, publicId, taskAssignedMsg);
    }
}
