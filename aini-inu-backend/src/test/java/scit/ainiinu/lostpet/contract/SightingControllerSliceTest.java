package scit.ainiinu.lostpet.contract;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.argThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;
import scit.ainiinu.lostpet.controller.SightingController;
import scit.ainiinu.lostpet.dto.SightingCreateRequest;
import scit.ainiinu.lostpet.dto.SightingResponse;
import scit.ainiinu.lostpet.service.SightingService;

@WebMvcTest(SightingController.class)
class SightingControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SightingService sightingService;

    @MockitoBean
    private JwtAuthInterceptor jwtAuthInterceptor;

    @MockitoBean
    private CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @BeforeEach
    void setUp() throws Exception {
        given(jwtAuthInterceptor.preHandle(any(), any(), any())).willReturn(true);
        given(currentMemberArgumentResolver.supportsParameter(
                argThat(parameter -> parameter.hasParameterAnnotation(CurrentMember.class))
        )).willReturn(true);
        given(currentMemberArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(22L);
    }

    @Test
    @WithMockUser
    @DisplayName("목격 제보 생성이 성공한다")
    void createSighting() throws Exception {
        SightingResponse response = SightingResponse.builder()
                .sightingId(1L)
                .status("OPEN")
                .foundAt(LocalDateTime.now())
                .build();
        given(sightingService.create(anyLong(), any(SightingCreateRequest.class))).willReturn(response);

        String request = """
                {
                  "photoUrl": "https://cdn/sightings/1.jpg",
                  "foundAt": "2026-02-26T11:10:00",
                  "foundLocation": "Yeoksam",
                  "memo": "brown collar"
                }
                """;

        mockMvc.perform(post("/api/v1/sightings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sightingId").value(1L));
    }
}
