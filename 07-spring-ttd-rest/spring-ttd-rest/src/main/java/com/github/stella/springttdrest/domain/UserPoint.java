package com.github.stella.springttdrest.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // 유저 식별자
    private long point;  // 현재 포인트

    // 비즈니스 로직을 도메인 안에 넣는 것이 좋습니다 (객체지향적)
    public void addPoint(long amount) {
        this.point += amount;
    }

    public void usePoint(long amount) {
        if (this.point < amount) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }
        this.point -= amount;
    }
}
