package com.github.stella.springmsamq.worker.chat;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatLogRepository extends MongoRepository<ChatLog, String> {
}
