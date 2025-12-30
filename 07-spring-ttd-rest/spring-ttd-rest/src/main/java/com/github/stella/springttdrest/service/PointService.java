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

    @Transactional // DB 상태 변경이 일어나므로 트랜잭션 필수
    public UserPoint charge(Long userId, long amount) {
        // 1. 검증 로직
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }

        // 2. 조회 (없으면 0원으로 초기화된 객체 생성)
        UserPoint userPoint = pointRepository.findByUserId(userId)
                .orElse(UserPoint.builder()
                        .userId(userId)
                        .point(0L)
                        .build());

        // 3. 도메인 로직 실행
        userPoint.addPoint(amount);

        // 4. 저장 및 반환
        return pointRepository.save(userPoint);
    }

    @Transactional
    public UserPoint use(Long userId, long amount) {
        // 1. 유저 조회 (없으면 포인트 0원인 상태로 간주 -> 당연히 잔액 부족 뜸)
        UserPoint userPoint = pointRepository.findByUserId(userId)
                .orElse(UserPoint.builder()
                        .userId(userId)
                        .point(0L)
                        .build());

        // 2. 도메인 로직 실행 (잔액 부족 시 여기서 예외 발생)
        userPoint.usePoint(amount);

        // 3. 저장 및 반환
        return pointRepository.save(userPoint);
    }
}
