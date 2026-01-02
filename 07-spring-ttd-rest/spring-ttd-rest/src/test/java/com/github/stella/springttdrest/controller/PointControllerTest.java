package com.github.stella.springttdrest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.stella.springttdrest.domain.PointHistory;
import com.github.stella.springttdrest.domain.TransactionType;
import com.github.stella.springttdrest.domain.UserPoint;
import com.github.stella.springttdrest.dto.PointChargeRequest;
import com.github.stella.springttdrest.dto.PointUseRequest;
import com.github.stella.springttdrest.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PointService pointService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("포인트 충전 API - 성공 시 변경된 포인트 정보를 반환한다")
    @WithMockUser(username = "1") // Security Context에 ID "1"인 유저가 로그인했다고 가정
    void charge_api_success() throws Exception {
        // given
        long userId = 1L; // @WithMockUser("1")과 일치해야 함
        long amount = 5000L;
        // DTO에는 이제 amount만 들어갑니다 (userId 제거됨)
        PointChargeRequest request = new PointChargeRequest(amount);

        // Service는 Controller가 Principal에서 꺼낸 ID(1L)로 호출됩니다.
        UserPoint responsePoint = UserPoint.builder().userId(userId).point(amount).build();
        given(pointService.charge(userId, amount)).willReturn(responsePoint);

        // when & then
        mockMvc.perform(patch("/point/charge")
//                        .with(csrf()) // POST, PATCH 요청 필수
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.point").value(amount));
    }

    @Test
    @DisplayName("포인트 사용 API - 성공 시 잔액이 감소된 포인트 정보를 반환한다")
    @WithMockUser(username = "1") // "tester" -> "1"로 수정 (Long 파싱 위해)
    void use_api_success() throws Exception {
        // given
        long userId = 1L;
        long amount = 1000L;
        // DTO에는 amount만 들어갑니다
        PointUseRequest request = new PointUseRequest(amount);

        UserPoint responsePoint = UserPoint.builder().userId(userId).point(2000L).build();
        given(pointService.use(userId, amount)).willReturn(responsePoint);

        // when & then
        mockMvc.perform(patch("/point/use")
//                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.point").value(2000L));
    }

    @Test
    @DisplayName("포인트 내역 조회 API - 로그인한 유저의 내역 리스트를 반환한다")
    @WithMockUser(username = "1") // "tester" -> "1"로 수정
    void history_api_success() throws Exception {
        // given
        long userId = 1L;
        List<PointHistory> historyList = List.of(
                PointHistory.builder().userId(userId).amount(1000L).type(TransactionType.CHARGE).build(),
                PointHistory.builder().userId(userId).amount(500L).type(TransactionType.USE).build()
        );

        given(pointService.getHistory(userId)).willReturn(historyList);

        // when & then
        // URL 수정: /point/{id}/histories -> /point/histories (내 정보 조회)
        mockMvc.perform(get("/point/histories"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].amount").value(1000L))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[1].amount").value(500L))
                .andExpect(jsonPath("$[1].type").value("USE"));
    }

    @Test
    @DisplayName("포인트 충전 실패 - 잘못된 금액 입력 시 에러 응답 포맷을 검증한다")
    @WithMockUser(username = "1") // "tester" -> "1"로 수정
    void charge_api_fail_invalid_amount() throws Exception {
        // given
        long userId = 1L;
        long invalidAmount = -500L;
        // DTO 수정
        PointChargeRequest request = new PointChargeRequest(invalidAmount);

        // Service에서 예외 발생 가정
        given(pointService.charge(userId, invalidAmount))
                .willThrow(new IllegalArgumentException("충전 금액은 0보다 커야 합니다."));

        // when & then
        mockMvc.perform(patch("/point/charge")
//                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("충전 금액은 0보다 커야 합니다."));
    }
}