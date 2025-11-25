package com.github.stella.springsecurityjwt.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           // [설명 1] 커스텀 필터 주입
                                           // 스프링 빈으로 등록된 커스텀 필터들을 파라미터로 주입받습니다.
                                          JwtAuthenticationFilter jwtAuthenticationFilter,
                                          RateLimitFilter rateLimitFilter) throws Exception {
        http
                // 1. CSRF 비활성화
                // REST API는 세션을 사용하지 않고 서버에 상태를 저장하지 않기 때문에(Stateless)
                // CSRF 보호가 불필요하여 끕니다.
                .csrf(csrf -> csrf.disable())

                // 2. CORS(Cross-Origin Resource Sharing) 설정 
                // React나 Vue 같은 프론트엔드(예: localhost:3000)가
                // 백엔드(localhost:8080)로 요청을 보낼 때 차단되지 않도록 허용 규칙을 설정합니다.
                // corsConfigurationSource()라는 메서드를 따로 만들어 규칙을 정의하고 여기서 불러옵니다.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. 세션 관리 설정 (STATELESS) 
                // "서버에 세션을 생성하지 않겠다"는 뜻입니다.
                // JWT 같은 토큰 방식을 쓸 때는 서버가 유저 상태를 기억하지 않으므로 필수로 설정해야 합니다.
                .formLogin((form) -> form.disable())
                .httpBasic((basic) -> basic.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. 인가(Authorization) 설정
                .authorizeHttpRequests(auth -> auth
                        // 누구나 접근 가능한 경로들 (홈, 에러 페이지, 헬스 체크)
                        .requestMatchers("/", "/error", "/actuator/health").permitAll()
                        // H2 데이터베이스 콘솔 접근 허용
                        .requestMatchers("/h2-console/**").permitAll()
                        // 회원가입, 로그인, 토큰 재발급 등 인증 관련 API는 누구나 접근 가능해야 함
                        .requestMatchers("/api/auth/signup", "/api/auth/login", "/api/auth/refresh").permitAll()
                        // [특정 메서드 제한] GET 요청으로 오는 /api/hello는 'USER' 권한이 있어야 함
                        .requestMatchers(HttpMethod.GET, "/api/hello").hasRole("USER")
                        // 그 외 모든 요청은 인증(로그인/토큰)이 필요함
                        .anyRequest().authenticated()
                )
                // 5. 헤더 설정 (Frame Options) 
                // H2 Console은 HTML의 <iframe> 태그를 사용하는데,
                // 시큐리티는 기본적으로 보안을 위해 iframe을 막아둡니다.
                // H2 Console을 정상적으로 보기 위해 이 옵션을 끕니다(disable).
                .headers(h -> h.frameOptions(frame -> frame.disable()))

                // 6. 커스텀 필터 추가 (순서 지정) 
                // addFilterBefore(A, B): B 필터가 실행되기 '직전'에 A 필터를 먼저 실행하라는 뜻입니다.

                // (1) RateLimitFilter 먼저 실행 (도배 방지 필터로 추정)
                //     : 로그인을 시도하기 전에 과도한 요청인지 먼저 검사해야 하므로 가장 앞에 둡니다.
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)

                // (2) JwtAuthenticationFilter 실행
                //     : ID/PW 검사(UsernamePassword...)를 하기 전에,
                //       요청 헤더에 유효한 '토큰'이 있는지 먼저 확인해서 인증을 처리하는 필터입니다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 1. 비밀번호 암호화기 (PasswordEncoder) Bean 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt라는 해시 함수를 이용해 패스워드를 암호화하는 구현체를 반환합니다.
        // 이것을 등록해야 DB에 비밀번호를 저장할 때 암호화할 수 있고,
        // 로그인할 때 사용자가 입력한 비밀번호와 DB의 암호화된 비밀번호가 일치하는지 비교할 수 있습니다.
        return new BCryptPasswordEncoder();
    }

    // 2. 인증 관리자 (AuthenticationManager) Bean 등록
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        // 스프링 시큐리티의 핵심인 '인증 관리자'를 스프링 컨테이너에 등록합니다.
        // 과거에는 WebSecurityConfigurerAdapter를 상속받아 오버라이딩했지만,
        // 현재는 AuthenticationConfiguration이라는 설정 클래스에서 이미 만들어진 것을 꺼내와서(get) 쓰면 됩니다.

        // *이게 왜 필요한가요?
        // JWT 로그인 방식을 만들려면, Controller에서 우리가 직접 "로그인 시도해줘!"라고 명령해야 합니다.
        // 그 명령을 수행하는 주체가 바로 이 AuthenticationManager입니다.
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // 1. CORS 설정 객체 생성
        CorsConfiguration config = new CorsConfiguration();

        // 2. 허용할 출처(Origin) 설정
        // "누가 내 서버에 문을 두드릴 수 있니?"
        // 예: 리액트 개발 서버(3000), 비트 개발 서버(5173)에서의 요청만 허락하겠다.
        // 주의: 실무에서는 배포된 프론트엔드 도메인(https://my-domain.com)도 추가해야 합니다.
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));

        // 3. 허용할 HTTP 메서드 설정
        // "어떤 행동을 할 수 있니?"
        // 단순 조회(GET), 저장(POST) 뿐만 아니라 수정(PUT), 삭제(DELETE)도 허용하겠다.
        // OPTIONS는 브라우저가 "내가 요청 보내도 돼?"라고 찔러보는(Preflight) 용도라서 필수입니다.
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 4. 허용할 헤더 설정
        // "요청할 때 봉투 겉면에 어떤 스티커를 붙여도 되니?"
        // JWT 로그인을 위해 'Authorization' 헤더를 허용해야 토큰을 받을 수 있습니다.
        // JSON 데이터를 주고받기 위해 'Content-Type' 헤더도 허용합니다.
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // 5. 자격 증명(쿠키/인증정보) 허용 설정
        // "내 서버가 쿠키나 인증 헤더를 포함한 요청을 받아줘도 될까?"
        // true로 설정하면 프론트엔드에서 credentials: 'include' 옵션을 쓸 수 있습니다.
        // *주의: 이걸 true로 켜면 setAllowedOrigins에 와일드카드("*")를 쓸 수 없고, 구체적인 주소를 적어야 합니다.
        config.setAllowCredentials(true);

        // 6. URL 기반 설정 소스 생성 및 등록
        // "이 규칙을 어디에 적용할까?"
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // "/**"는 "내 서버의 모든 경로(API)"에 대해 위에서 만든 config 규칙을 적용하겠다는 뜻입니다.
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
