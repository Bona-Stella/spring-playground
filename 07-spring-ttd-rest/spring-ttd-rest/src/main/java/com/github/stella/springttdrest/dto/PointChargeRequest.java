package com.github.stella.springttdrest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PointChargeRequest(
        @NotNull(message = "금액은 필수입니다.")
        @Min(value = 1, message = "금액은 0보다 커야 합니다.") // 0원 이하는 자동 거부
        Long amount // 기본 타입 long 대신 Wrapper class(Long) 권장 (null 체크 가능하도록)
) {}