package com.github.stella.springapiboard.common.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// 1. 상속받는 자식 클래스에게 컬럼 정보만 제공 -> 자체적으로 테이블 생성하지 않음
@MappedSuperclass
// 2. 엔티티의 변화를 감지하는 리스너를 등록 -> 없으면 작동하지 않음
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    // 생성 시 자동 주입
    @CreatedDate
    // updatable -> 수정 불가능하게 막기
    @Column(name = "created_at", nullable = false, updatable = false)
    protected LocalDateTime createdAt;

    // 수정 시 자동 주입
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    protected LocalDateTime updatedAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
