package com.github.stella.springapiboard.board.dto;

import java.time.LocalDate;

public class StatsDtos {

    public record DailyCount(
            LocalDate day,
            long count
    ) {}

    public record TopItem(
            Long id,
            String name,
            long count
    ) {}
}
