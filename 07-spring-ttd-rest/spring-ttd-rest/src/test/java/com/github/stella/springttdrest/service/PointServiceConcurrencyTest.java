package com.github.stella.springttdrest.service;

import com.github.stella.springttdrest.domain.PointRepository;
import com.github.stella.springttdrest.domain.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // 실제 스프링 컨텍스트를 띄움 (H2 DB 사용)
class PointServiceConcurrencyTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private PointRepository pointRepository;

    @Test
    @DisplayName("동시에 10번 충전 요청 - 동시성 제어가 안되면 실패한다")
    void concurrent_charge() throws InterruptedException {
        // given
        long userId = 1L;
        long amount = 100L;
        int threadCount = 10;

        // ★ 추가: 동시성 테스트의 정확성을 위해 유저를 미리 생성해둡니다.
        pointRepository.save(UserPoint.builder().userId(userId).point(0L).build());

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.charge(userId, amount);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        UserPoint userPoint = pointRepository.findByUserId(userId).orElseThrow();
        assertThat(userPoint.getPoint()).isEqualTo(100L * threadCount);
    }
}