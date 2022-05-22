package com.ates.tasks;

import com.ates.event.EventUtils;
import com.ates.messages.EventHeaders;
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
    @Autowired
    private KafkaTemplate<String, Task.Completed> taskCompletedTemplate;

    private static final String TASK_LIFECYCLE_TOPIC_NAME = "task-lifecycle";

    @Bean
    public NewTopic topic() {
        return TopicBuilder
                .name(TASK_LIFECYCLE_TOPIC_NAME)
                .build();
    }

    public void sendTaskAddedEvent(com.ates.tasks.Task task) {
        String publicId = task.getId().toString();
        EventHeaders eventHeaders = EventUtils.getEventHeaders(
                1,
                Task.Added.getDescriptor().getFullName(),
                this.getClass().getSimpleName()
        );
        Task.Added taskAddedMsg = Task.Added
                .newBuilder()
                .setHeaders(eventHeaders)
                .setPublicId(publicId)
                .setDescription(task.getDescription())
                .build();
        taskAddedTemplate.send(TASK_LIFECYCLE_TOPIC_NAME, publicId, taskAddedMsg);
    }

    public void sendTaskAssignedEvent(com.ates.tasks.Task task) {
        String publicId = task.getId().toString();
        String assigneeId = task.getAssigneeId().toString();
        EventHeaders eventHeaders = EventUtils.getEventHeaders(
                1,
                Task.Assigned.getDescriptor().getFullName(),
                this.getClass().getSimpleName()
        );
        Task.Assigned taskAssignedMsg = Task.Assigned
                .newBuilder()
                .setHeaders(eventHeaders)
                .setPublicId(publicId)
                .setAssigneeId(assigneeId)
                .build();
        taskAssignedTemplate.send(TASK_LIFECYCLE_TOPIC_NAME, publicId, taskAssignedMsg);
    }

    public void sendTaskCompletedEvent(com.ates.tasks.Task task) {
        String publicId = task.getId().toString();
        String assigneeId = task.getAssigneeId().toString();
        EventHeaders eventHeaders = EventUtils.getEventHeaders(
                1,
                Task.Completed.getDescriptor().getFullName(),
                this.getClass().getSimpleName()
        );
        Task.Completed taskCompletedMsg = Task.Completed
                .newBuilder()
                .setHeaders(eventHeaders)
                .setPublicId(publicId)
                .setCompletedById(assigneeId)
                .build();
        taskCompletedTemplate.send(TASK_LIFECYCLE_TOPIC_NAME, publicId, taskCompletedMsg);
    }
}
