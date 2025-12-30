package com.github.stella.springttdrest.dto;

public record PointChargeRequest(
        long userId,
        long amount
) {}