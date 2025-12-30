package com.github.stella.springttdrest.service;

import com.github.stella.springttdrest.domain.PointRepository;
import com.github.stella.springttdrest.domain.UserPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private PointRepository pointRepository;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("포인트 충전 성공 - 기존 잔액에 충전 금액이 더해져야 한다")
    void charge_success() {
        // given
        Long userId = 1L;
        long chargeAmount = 1000L;
        UserPoint existingPoint = UserPoint.builder().userId(userId).point(500L).build();

        given(pointRepository.findByUserId(userId)).willReturn(Optional.of(existingPoint));

        // ★ 추가된 부분: save 호출 시, 들어온 객체(Argument)를 그대로 반환한다고 가정
        given(pointRepository.save(any(UserPoint.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        UserPoint result = pointService.charge(userId, chargeAmount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPoint()).isEqualTo(1500L);
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
}