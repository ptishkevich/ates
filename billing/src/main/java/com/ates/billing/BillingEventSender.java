package com.ates.billing;

import com.ates.event.EventUtils;
import com.ates.messages.EventHeaders;
import com.ates.messages.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class BillingEventSender {
    @Autowired
    private KafkaTemplate<String, Task.PriceCalculated> taskPriceCalculatedTemplate;

    private static final String TASK_LIFECYCLE_TOPIC_NAME = "task-lifecycle";

    public void sendTaskPriceCalculatedEvent(String taskPublicId, int amount) {
        EventHeaders eventHeaders = EventUtils.getEventHeaders(
                1,
                Task.PriceCalculated.getDescriptor().getFullName(),
                this.getClass().getSimpleName()
        );

        Task.PriceCalculated priceCalculatedMsg = Task.PriceCalculated
                .newBuilder()
                .setHeaders(eventHeaders)
                .setTaskPublicId(taskPublicId)
                .setAmount(amount)
                .build();

        taskPriceCalculatedTemplate.send(TASK_LIFECYCLE_TOPIC_NAME, taskPublicId, priceCalculatedMsg);
    }
}
