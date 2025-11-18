package com.github.stella.springredisjob.external.mock.dto;

public record ExternalWeatherResponse(
        String city,
        double temperatureC,
        String description,
        String fetchedAt
) {}
