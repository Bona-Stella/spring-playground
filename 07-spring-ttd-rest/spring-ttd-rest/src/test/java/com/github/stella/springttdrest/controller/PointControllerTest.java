package com.github.stella.springttdrest.controller;

import com.github.stella.springttdrest.dto.PointChargeRequest;
import com.github.stella.springttdrest.domain.UserPoint;
import com.github.stella.springttdrest.dto.PointUseRequest;
import com.github.stella.springttdrest.service.PointService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class) // Controller만 떼어서 테스트
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean // Spring Context에 가짜 Bean 등록
    private PointService pointService;

    @Autowired
    private ObjectMapper objectMapper; // 객체 -> JSON 변환용

    @Test
    @DisplayName("포인트 충전 API - 성공 시 변경된 포인트 정보를 반환한다")
    void charge_api_success() throws Exception {
        // given
        long userId = 1L;
        long amount = 5000L;
        PointChargeRequest request = new PointChargeRequest(userId, amount);

        // Service가 충전 후 5000원짜리 객체를 리턴한다고 가정 (Mocking)
        UserPoint responsePoint = UserPoint.builder().userId(userId).point(amount).build();
        given(pointService.charge(userId, amount)).willReturn(responsePoint);

        // when & then
        mockMvc.perform(patch("/point/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))) // 객체를 JSON 문자열로 변환
                .andDo(print()) // 로그 출력
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.point").value(amount));
    }

    @Test
    @DisplayName("포인트 사용 API - 성공 시 잔액이 감소된 포인트 정보를 반환한다")
    void use_api_success() throws Exception {
        // given
        long userId = 1L;
        long amount = 1000L;
        PointUseRequest request = new PointUseRequest(userId, amount);

        // Service가 사용 후 남은 잔액(2000원)을 리턴한다고 가정
        UserPoint responsePoint = UserPoint.builder().userId(userId).point(2000L).build();
        given(pointService.use(userId, amount)).willReturn(responsePoint);

        // when & then
        mockMvc.perform(patch("/point/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.point").value(2000L));
    }
}