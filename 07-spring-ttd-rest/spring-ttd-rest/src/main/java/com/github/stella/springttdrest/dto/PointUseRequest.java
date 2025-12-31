package com.github.stella.springttdrest.dto;

public record PointUseRequest(
        long userId,
        long amount
) {
}