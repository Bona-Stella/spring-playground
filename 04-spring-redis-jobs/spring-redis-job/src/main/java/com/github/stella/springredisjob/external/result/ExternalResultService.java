package com.github.stella.springredisjob.external.result;

import com.github.stella.springredisjob.external.mock.dto.ExternalWeatherResponse;
import com.github.stella.springredisjob.external.result.dto.ExternalResultDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalResultService {
    private final ExternalResultRepository repository;

    public ExternalResultService(ExternalResultRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ExternalResultDto saveFromResponse(ExternalWeatherResponse res) {
        ExternalResult entity = new ExternalResult(
                res.city(),
                res.temperatureC(),
                res.description(),
                res.fetchedAt(),
                java.time.ZonedDateTime.now().toString()
        );
        ExternalResult saved = repository.save(entity);
        return ExternalResultDto.from(saved);
    }
}
