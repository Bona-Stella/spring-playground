package com.github.stella.springttdrest.dto;

public record LoginRequest(
        String username, // 여기서는 간단히 ID를 username으로 씁니다
        String password  // 실무에선 암호화해야 하지만 여기선 생략
) {}