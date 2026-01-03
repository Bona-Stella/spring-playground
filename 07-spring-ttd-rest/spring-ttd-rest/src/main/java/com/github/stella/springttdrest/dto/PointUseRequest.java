package com.github.stella.springttdrest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PointUseRequest(
        @NotNull(message = "금액은 필수입니다.")
        @Min(value = 1, message = "사용 금액은 0보다 커야 합니다.") // ★ 여기도 필수!
        Long amount
) {}