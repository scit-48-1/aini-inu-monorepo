package scit.ainiinu.pet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.common.security.annotation.CurrentMember;
import scit.ainiinu.common.security.interceptor.JwtAuthInterceptor;
import scit.ainiinu.common.security.resolver.CurrentMemberArgumentResolver;
import scit.ainiinu.pet.dto.request.PetCreateRequest;
import scit.ainiinu.pet.dto.request.PetUpdateRequest;
import scit.ainiinu.pet.dto.response.BreedResponse;
import scit.ainiinu.pet.dto.response.MainPetChangeResponse;
import scit.ainiinu.pet.dto.response.PersonalityResponse;
import scit.ainiinu.pet.dto.response.PetResponse;
import scit.ainiinu.pet.dto.response.WalkingStyleResponse;
import scit.ainiinu.pet.entity.enums.PetGender;
import scit.ainiinu.pet.entity.enums.PetSize;
import scit.ainiinu.pet.exception.PetErrorCode;
import scit.ainiinu.pet.service.PetService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import org.springframework.context.annotation.Import;
import scit.ainiinu.common.exception.GlobalExceptionHandler;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PetController.class)
@Import(GlobalExceptionHandler.class)
class PetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PetService petService;

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
        given(currentMemberArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(1L);
    }

    @Nested
    @DisplayName("반려견 등록 API")
    class CreatePet {

        @Test
        @DisplayName("성공: 반려견 정보가 유효하면 등록에 성공한다")
        @WithMockUser
        void createPet_Success() throws Exception {
            // given
            PetCreateRequest request = new PetCreateRequest();
            request.setName("몽이");
            request.setBreedId(1L);
            request.setBirthDate(LocalDate.of(2021, 1, 1));
            request.setGender(PetGender.MALE);
            request.setSize(PetSize.SMALL);
            request.setIsNeutered(true);
            request.setIsMain(true);

            PetResponse mockResponse = PetResponse.builder()
                    .id(1L)
                    .name("몽이")
                    .breed(new BreedResponse(1L, "말티즈", PetSize.SMALL))
                    .gender(PetGender.MALE.name())
                    .size(PetSize.SMALL.name())
                    .age(3)
                    .isMain(true)
                    .isNeutered(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(petService.createPet(any(), any(PetCreateRequest.class))).willReturn(mockResponse);

            // when & then
            mockMvc.perform(post("/api/v1/pets")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("몽이"))
                    .andExpect(jsonPath("$.data.breed.name").value("말티즈"));
        }

        @Test
        @DisplayName("실패: 필수값 누락 시 400 Bad Request")
        @WithMockUser
        void createPet_ValidationFail() throws Exception {
            // given: 이름이 없는 잘못된 요청
            PetCreateRequest request = new PetCreateRequest();
            request.setBreedId(1L);
            request.setBirthDate(LocalDate.of(2021, 1, 1));

            // when & then
            mockMvc.perform(post("/api/v1/pets")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("내 반려견 목록 조회 API")
    class GetMyPets {

        @Test
        @DisplayName("성공: 회원의 반려견 목록을 조회한다")
        @WithMockUser
        void getMyPets_Success() throws Exception {
            // given
            List<PetResponse> mockResponse = List.of(
                    PetResponse.builder()
                            .id(1L)
                            .name("몽이")
                            .breed(new BreedResponse(1L, "말티즈", PetSize.SMALL))
                            .isMain(true)
                            .createdAt(LocalDateTime.now())
                            .build(),
                    PetResponse.builder()
                            .id(2L)
                            .name("콩이")
                            .breed(new BreedResponse(2L, "푸들", PetSize.SMALL))
                            .isMain(false)
                            .createdAt(LocalDateTime.now())
                            .build()
            );

            given(petService.getUserPets(any())).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/v1/pets")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].name").value("몽이"))
                    .andExpect(jsonPath("$.data[0].isMain").value(true))
                    .andExpect(jsonPath("$.data[1].name").value("콩이"))
                    .andExpect(jsonPath("$.data[1].isMain").value(false));
        }

        @Test
        @DisplayName("성공: 반려견이 없으면 빈 목록을 반환한다")
        @WithMockUser
        void getMyPets_EmptyList() throws Exception {
            // given
            given(petService.getUserPets(any())).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/pets")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    @Nested
    @DisplayName("반려견 정보 수정 API")
    class UpdatePet {

        @Test
        @DisplayName("성공: 반려견 정보를 수정한다")
        @WithMockUser
        void updatePet_Success() throws Exception {
            // given
            Long petId = 1L;
            PetUpdateRequest request = PetUpdateRequest.builder()
                    .name("수정된이름")
                    .birthDate(LocalDate.of(2020, 1, 1))
                    .isNeutered(true)
                    .mbti("INTJ")
                    .build();

            PetResponse mockResponse = PetResponse.builder()
                    .id(petId)
                    .name("수정된이름")
                    .breed(new BreedResponse(1L, "말티즈", PetSize.SMALL))
                    .age(5)
                    .isNeutered(true)
                    .mbti("INTJ")
                    .createdAt(LocalDateTime.now())
                    .build();

            given(petService.updatePet(any(), eq(petId), any(PetUpdateRequest.class))).willReturn(mockResponse);

            // when & then
            mockMvc.perform(patch("/api/v1/pets/{petId}", petId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("수정된이름"))
                    .andExpect(jsonPath("$.data.age").value(5));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 반려견 수정 시 404 Not Found (P001)")
        @WithMockUser
        void updatePet_NotFound() throws Exception {
            // given
            Long petId = 999L;
            PetUpdateRequest request = PetUpdateRequest.builder()
                    .name("수정된이름")
                    .build();

            given(petService.updatePet(any(), eq(petId), any(PetUpdateRequest.class)))
                    .willThrow(new BusinessException(PetErrorCode.PET_NOT_FOUND));

            // when & then
            mockMvc.perform(patch("/api/v1/pets/{petId}", petId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("P001"));
        }

        @Test
        @DisplayName("실패: 본인의 반려견이 아닌 경우 403 Forbidden (P006)")
        @WithMockUser
        void updatePet_NotYourPet() throws Exception {
            // given
            Long petId = 1L;
            PetUpdateRequest request = PetUpdateRequest.builder()
                    .name("수정된이름")
                    .build();

            given(petService.updatePet(any(), eq(petId), any(PetUpdateRequest.class)))
                    .willThrow(new BusinessException(PetErrorCode.NOT_YOUR_PET));

            // when & then
            mockMvc.perform(patch("/api/v1/pets/{petId}", petId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("P006"));
        }
    }

    @Nested
    @DisplayName("반려견 삭제 API")
    class DeletePet {

        @Test
        @DisplayName("성공: 반려견을 삭제한다")
        @WithMockUser
        void deletePet_Success() throws Exception {
            // given
            Long petId = 1L;
            willDoNothing().given(petService).deletePet(any(), eq(petId));

            // when & then
            mockMvc.perform(delete("/api/v1/pets/{petId}", petId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 반려견 삭제 시 404 Not Found (P001)")
        @WithMockUser
        void deletePet_NotFound() throws Exception {
            // given
            Long petId = 999L;
            willThrow(new BusinessException(PetErrorCode.PET_NOT_FOUND))
                    .given(petService).deletePet(any(), eq(petId));

            // when & then
            mockMvc.perform(delete("/api/v1/pets/{petId}", petId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("P001"));
        }

        @Test
        @DisplayName("실패: 본인의 반려견이 아닌 경우 403 Forbidden (P006)")
        @WithMockUser
        void deletePet_NotYourPet() throws Exception {
            // given
            Long petId = 1L;
            willThrow(new BusinessException(PetErrorCode.NOT_YOUR_PET))
                    .given(petService).deletePet(any(), eq(petId));

            // when & then
            mockMvc.perform(delete("/api/v1/pets/{petId}", petId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("P006"));
        }
    }

    @Nested
    @DisplayName("메인 반려견 변경 API")
    class ChangeMainPet {

        @Test
        @DisplayName("성공: 메인 반려견을 변경한다")
        @WithMockUser
        void changeMainPet_Success() throws Exception {
            // given
            Long petId = 2L;
            MainPetChangeResponse mockResponse = MainPetChangeResponse.builder()
                    .id(petId)
                    .name("콩이")
                    .isMain(true)
                    .build();

            given(petService.changeMainPet(any(), eq(petId))).willReturn(mockResponse);

            // when & then
            mockMvc.perform(patch("/api/v1/pets/{petId}/main", petId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(petId))
                    .andExpect(jsonPath("$.data.name").value("콩이"))
                    .andExpect(jsonPath("$.data.isMain").value(true));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 반려견을 메인으로 변경 시 404 Not Found (P001)")
        @WithMockUser
        void changeMainPet_NotFound() throws Exception {
            // given
            Long petId = 999L;
            given(petService.changeMainPet(any(), eq(petId)))
                    .willThrow(new BusinessException(PetErrorCode.PET_NOT_FOUND));

            // when & then
            mockMvc.perform(patch("/api/v1/pets/{petId}/main", petId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("P001"));
        }

        @Test
        @DisplayName("실패: 본인의 반려견이 아닌 경우 403 Forbidden (P006)")
        @WithMockUser
        void changeMainPet_NotYourPet() throws Exception {
            // given
            Long petId = 1L;
            given(petService.changeMainPet(any(), eq(petId)))
                    .willThrow(new BusinessException(PetErrorCode.NOT_YOUR_PET));

            // when & then
            mockMvc.perform(patch("/api/v1/pets/{petId}/main", petId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("P006"));
        }
    }

    @Nested
    @DisplayName("마스터 데이터 조회 API")
    class MasterData {

        @Test
        @DisplayName("견종 목록 조회 API 성공 테스트")
        @WithMockUser
        void getBreeds_Success() throws Exception {
            // given
            List<BreedResponse> mockResponse = List.of(
                    new BreedResponse(1L, "말티즈", PetSize.SMALL),
                    new BreedResponse(2L, "골든 리트리버", PetSize.LARGE)
            );
            given(petService.getAllBreeds()).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/v1/breeds")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].name").value("말티즈"))
                    .andExpect(jsonPath("$.data[1].name").value("골든 리트리버"));
        }

        @Test
        @DisplayName("성격 목록 조회 API 성공 테스트")
        @WithMockUser
        void getPersonalities_Success() throws Exception {
            // given
            List<PersonalityResponse> mockResponse = List.of(
                    new PersonalityResponse(1L, "소심해요", "SHY"),
                    new PersonalityResponse(2L, "활발해요", "ACTIVE")
            );
            given(petService.getAllPersonalities()).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/v1/personalities")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].name").value("소심해요"))
                    .andExpect(jsonPath("$.data[0].code").value("SHY"));
        }

        @Test
        @DisplayName("산책 스타일 목록 조회 API 성공 테스트")
        @WithMockUser
        void getWalkingStyles_Success() throws Exception {
            // given
            List<WalkingStyleResponse> mockResponse = List.of(
                    WalkingStyleResponse.builder().id(1L).name("느긋한 산책").code("RELAXED").build(),
                    WalkingStyleResponse.builder().id(2L).name("활동적인 산책").code("ACTIVE").build()
            );
            given(petService.getAllWalkingStyles()).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/v1/walking-styles")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].name").value("느긋한 산책"))
                    .andExpect(jsonPath("$.data[0].code").value("RELAXED"));
        }
    }
}
