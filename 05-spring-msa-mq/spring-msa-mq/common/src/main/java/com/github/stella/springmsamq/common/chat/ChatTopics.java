package com.github.stella.springmsamq.common.chat;

public final class ChatTopics {
    private ChatTopics() {}
    public static final String REDIS_CHANNEL = "chat:messages"; // Redis Pub/Sub channel
    public static final String KAFKA_TOPIC = "chat.messages";   // Kafka topic
}
