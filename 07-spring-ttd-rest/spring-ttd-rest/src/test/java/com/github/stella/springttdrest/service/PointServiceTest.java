package com.github.stella.springttdrest.service;

import com.github.stella.springttdrest.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("포인트 충전 성공 - 포인트 증가 및 내역 저장이 되어야 한다") // 설명 수정
    void charge_success() {
        // given
        Long userId = 1L;
        long chargeAmount = 1000L;
        UserPoint existingPoint = UserPoint.builder().userId(userId).point(500L).build();

        given(pointRepository.findByUserIdWithLock(userId)) // 락 메서드로 바뀐 것 주의
                .willReturn(Optional.of(existingPoint));

        // save mocking
        given(pointRepository.save(any(UserPoint.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        UserPoint result = pointService.charge(userId, chargeAmount);

        // then
        assertThat(result.getPoint()).isEqualTo(1500L);

        // ★ 추가된 검증: History 저장 메서드가 1번 호출되었는지 확인
        verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
    }

    @Test
    @DisplayName("포인트 충전 실패 - 0원 이하의 금액은 예외 발생")
    void charge_fail_invalid_amount() {
        // given
        Long userId = 1L;
        long invalidAmount = -500L;

        // when & then
        assertThatThrownBy(() -> pointService.charge(userId, invalidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("충전 금액은 0보다 커야 합니다.");
    }

    @Test
    @DisplayName("포인트 사용 성공 - 잔액이 감소해야 한다")
    void use_success() {
        // given
        Long userId = 1L;
        long useAmount = 400L;
        UserPoint existingPoint = UserPoint.builder().userId(userId).point(1000L).build();

        // ★ 수정됨: findByUserId -> findByUserIdWithLock
        given(pointRepository.findByUserIdWithLock(userId)).willReturn(Optional.of(existingPoint));

        // save mocking
        given(pointRepository.save(any(UserPoint.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        UserPoint result = pointService.use(userId, useAmount);

        // then
        assertThat(result.getPoint()).isEqualTo(600L);

        // 내역 저장 검증도 추가하면 좋습니다
        verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
    }

    @Test
    @DisplayName("포인트 사용 실패 - 잔액보다 많은 금액 사용 시 예외 발생")
    void use_fail_insufficient_balance() {
        // given
        Long userId = 1L;
        long useAmount = 2000L;
        UserPoint existingPoint = UserPoint.builder().userId(userId).point(1000L).build();

        // ★ 수정됨: findByUserId -> findByUserIdWithLock
        given(pointRepository.findByUserIdWithLock(userId)).willReturn(Optional.of(existingPoint));

        // when & then
        assertThatThrownBy(() -> pointService.use(userId, useAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔액이 부족합니다.");
    }

    @Test
    @DisplayName("포인트 내역 조회 - 유저의 내역 리스트를 반환해야 한다")
    void getHistory_success() {
        // given
        Long userId = 1L;
        // 가짜 데이터 2개 생성
        List<PointHistory> historyList = List.of(
                PointHistory.builder().userId(userId).amount(1000L).type(TransactionType.CHARGE).build(),
                PointHistory.builder().userId(userId).amount(500L).type(TransactionType.USE).build()
        );

        // Repository가 이 리스트를 반환한다고 가정
        given(pointHistoryRepository.findAllByUserIdOrderByUpdateMillisDesc(userId))
                .willReturn(historyList);

        // when
        // ★ 아직 getHistory 메서드가 없어서 컴파일 에러 발생 (RED)
        List<PointHistory> result = pointService.getHistory(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAmount()).isEqualTo(1000L);
        assertThat(result.get(1).getType()).isEqualTo(TransactionType.USE);
    }

}