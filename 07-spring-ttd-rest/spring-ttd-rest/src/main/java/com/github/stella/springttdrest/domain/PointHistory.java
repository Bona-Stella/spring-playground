package com.github.stella.springttdrest.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class) // 생성 시간 자동 기록
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private long amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type; // CHARGE or USE

    @CreatedDate
    private LocalDateTime updateMillis; // 생성 시간
}