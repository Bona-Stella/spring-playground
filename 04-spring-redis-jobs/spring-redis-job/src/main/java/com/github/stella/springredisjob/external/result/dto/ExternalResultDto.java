package com.github.stella.springredisjob.external.result.dto;

import com.github.stella.springredisjob.external.result.ExternalResult;

public record ExternalResultDto(
        Long id,
        String city,
        double temperatureC,
        String description,
        String fetchedAt,
        String savedAt
) {
    public static ExternalResultDto from(ExternalResult e) {
        return new ExternalResultDto(
                e.getId(),
                e.getCity(),
                e.getTemperatureC(),
                e.getDescription(),
                e.getFetchedAt(),
                e.getSavedAt()
        );
    }
}
