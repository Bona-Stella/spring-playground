package com.github.stella.springmsamq.common.exception;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CommonExceptionHandler.class)
public class CommonExceptionConfig {
}
