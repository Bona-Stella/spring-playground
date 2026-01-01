package com.github.stella.springttdrest.service;

import com.github.stella.springttdrest.domain.*; // 패키지 경로 확인
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;
    private final PointHistoryRepository pointHistoryRepository; // ★ 추가됨

    @Transactional
    public UserPoint charge(Long userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }

        UserPoint userPoint = pointRepository.findByUserIdWithLock(userId)
                .orElse(UserPoint.builder()
                        .userId(userId)
                        .point(0L)
                        .build());

        userPoint.addPoint(amount);

        // ★ 히스토리 저장 로직 추가
        pointHistoryRepository.save(PointHistory.builder()
                .userId(userId)
                .amount(amount)
                .type(TransactionType.CHARGE)
                .updateMillis(LocalDateTime.now()) // 시간 직접 기록
                .build());

        return pointRepository.save(userPoint);
    }

    @Transactional
    public UserPoint use(Long userId, long amount) {
        UserPoint userPoint = pointRepository.findByUserIdWithLock(userId)
                .orElse(UserPoint.builder()
                        .userId(userId)
                        .point(0L)
                        .build());

        userPoint.usePoint(amount);

        // ★ 히스토리 저장 로직 추가
        pointHistoryRepository.save(PointHistory.builder()
                .userId(userId)
                .amount(amount)
                .type(TransactionType.USE)
                .updateMillis(LocalDateTime.now())
                .build());

        return pointRepository.save(userPoint);
    }
}