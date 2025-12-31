package com.github.stella.springttdrest.service;

import com.github.stella.springttdrest.domain.PointRepository;
import com.github.stella.springttdrest.domain.UserPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;

    @Transactional
    public UserPoint charge(Long userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }

        // ★ 수정: findByUserId -> findByUserIdWithLock
        // 주의: 락은 데이터가 존재해야 걸립니다. 없으면 생성 로직으로 넘어갑니다.
        UserPoint userPoint = pointRepository.findByUserIdWithLock(userId)
                .orElse(UserPoint.builder()
                        .userId(userId)
                        .point(0L)
                        .build());

        userPoint.addPoint(amount);

        return pointRepository.save(userPoint);
    }

    @Transactional
    public UserPoint use(Long userId, long amount) {
        // ★ 수정: findByUserId -> findByUserIdWithLock
        UserPoint userPoint = pointRepository.findByUserIdWithLock(userId)
                .orElse(UserPoint.builder()
                        .userId(userId)
                        .point(0L)
                        .build());

        userPoint.usePoint(amount);

        return pointRepository.save(userPoint);
    }
}
