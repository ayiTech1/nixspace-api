package com.nixspace.infrastructure.kafka.events;

public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String MESSAGE_EVENTS     = "nixspace.message.events";
    public static final String FILE_EVENTS        = "nixspace.file.events";
    public static final String PRESENCE_EVENTS    = "nixspace.presence.events";
    public static final String NOTIFICATION_EVENTS = "nixspace.notification.events";
}
