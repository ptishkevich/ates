package com.ates.event;

import com.ates.messages.EventHeaders;

import java.util.UUID;

public class EventUtils {
    private EventUtils() {}

    public static EventHeaders getEventHeaders(int version, String eventType, String producerName) {
        return EventHeaders
                .newBuilder()
                .setId(UUID.randomUUID().toString())
                .setVersion(version)
                .setProducer(producerName)
                .setTime(System.currentTimeMillis())
                .setMessageType(eventType)
                .build();
    }
}
