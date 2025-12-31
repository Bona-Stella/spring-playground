package com.github.stella.springttdrest.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointRepository extends JpaRepository<UserPoint, Long> {
    Optional<UserPoint> findByUserId(Long userId);

    // ★ 추가된 부분: 비관적 락(쓰기 락)을 걸고 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserPoint p where p.userId = :userId")
    Optional<UserPoint> findByUserIdWithLock(@Param("userId") Long userId);
}