package com.github.stella.springmsamq.worker.chat;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "chatLog")
@Getter
@NoArgsConstructor // Spring Data MongoDB가 객체 생성 시 필요
@AllArgsConstructor // Builder 패턴 사용 시 필요
@Builder            // 빌더 패턴 적용
@ToString
public class ChatLog {

    @Id
    private String id;

    private String roomId;
    private String sender;
    private String content;
    private long timestamp;
}