package scit.ainiinu.pet.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.member.service.MemberService;
import scit.ainiinu.pet.dto.request.PetCreateRequest;
import scit.ainiinu.pet.dto.request.PetUpdateRequest;
import scit.ainiinu.pet.dto.response.BreedResponse;
import scit.ainiinu.pet.dto.response.MainPetChangeResponse;
import scit.ainiinu.pet.dto.response.PersonalityResponse;
import scit.ainiinu.pet.dto.response.PetResponse;
import scit.ainiinu.pet.dto.response.WalkingStyleResponse;
import scit.ainiinu.pet.entity.Breed;
import scit.ainiinu.pet.entity.Personality;
import scit.ainiinu.pet.entity.Pet;
import scit.ainiinu.pet.entity.WalkingStyle;
import scit.ainiinu.pet.entity.enums.PetGender;
import scit.ainiinu.pet.entity.enums.PetSize;
import scit.ainiinu.pet.exception.PetErrorCode;
import scit.ainiinu.pet.repository.BreedRepository;
import scit.ainiinu.pet.repository.PersonalityRepository;
import scit.ainiinu.pet.repository.PetRepository;
import scit.ainiinu.pet.repository.WalkingStyleRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @InjectMocks
    private PetService petService;

    @Mock
    private PetRepository petRepository;
    @Mock
    private BreedRepository breedRepository;
    @Mock
    private PersonalityRepository personalityRepository;
    @Mock
    private WalkingStyleRepository walkingStyleRepository;
    @Mock
    private MemberService memberService;

    @Nested
    @DisplayName("반려견 등록")
    class CreatePet {

        @Test
        @DisplayName("성공: 첫 반려견 등록 시 자동으로 메인 반려견이 되고 memberType이 PET_OWNER로 변경된다")
        void success_first_pet_auto_main_and_upgrade_member_type() {
            // given
            Long memberId = 1L;
            PetCreateRequest request = createRequest();

            Breed breed = mock(Breed.class);


            given(petRepository.countByMemberId(memberId)).willReturn(0); // 0마리
            given(breedRepository.findById(request.getBreedId())).willReturn(Optional.of(breed));
            given(petRepository.save(any(Pet.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            PetResponse response = petService.createPet(memberId, request);

            // then
            assertThat(response.getIsMain()).isTrue();
            then(petRepository).should(times(1)).save(any(Pet.class));
            then(memberService).should(times(1)).upgradeToPetOwner(memberId); // memberType 변경 확인
        }

        @Test
        @DisplayName("성공: 두 번째 반려견 등록 시 memberType 변경 호출되지 않음")
        void success_second_pet_no_member_type_change() {
            // given
            Long memberId = 1L;
            PetCreateRequest request = createRequest();

            Breed breed = mock(Breed.class);


            given(petRepository.countByMemberId(memberId)).willReturn(1); // 이미 1마리 있음
            given(breedRepository.findById(request.getBreedId())).willReturn(Optional.of(breed));
            given(petRepository.save(any(Pet.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            petService.createPet(memberId, request);

            // then
            then(memberService).should(never()).upgradeToPetOwner(memberId); // memberType 변경 호출 안 됨
        }

        @Test
        @DisplayName("성공: 이미 반려견이 있을 때 isMain=true로 등록하면 기존 메인이 해제된다")
        void success_change_main_pet() {
            // given
            Long memberId = 1L;
            PetCreateRequest request = createRequest();
            request.setIsMain(true);

            Pet existingMainPet = mock(Pet.class);
            Breed breed = mock(Breed.class);


            given(petRepository.countByMemberId(memberId)).willReturn(1);
            given(breedRepository.findById(request.getBreedId())).willReturn(Optional.of(breed));
            given(petRepository.findByMemberIdAndIsMainTrue(memberId)).willReturn(Optional.of(existingMainPet));
            given(petRepository.save(any(Pet.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            PetResponse response = petService.createPet(memberId, request);

            // then
            assertThat(response.getIsMain()).isTrue();
            then(existingMainPet).should(times(1)).setMain(false);
        }

        @Test
        @DisplayName("성공: 산책 스타일과 성향을 포함하여 등록한다")
        void success_with_styles_and_personalities() {
            // given
            Long memberId = 1L;
            PetCreateRequest request = createRequest();
            request.setWalkingStyles(List.of("RUN", "SNIFF"));
            request.setPersonalityIds(List.of(1L));

            WalkingStyle style1 = mock(WalkingStyle.class);
            WalkingStyle style2 = mock(WalkingStyle.class);
            Breed breed = mock(Breed.class);


            given(petRepository.countByMemberId(memberId)).willReturn(0);
            given(breedRepository.findById(any())).willReturn(Optional.of(breed));
            given(walkingStyleRepository.findByCodeIn(anyList())).willReturn(List.of(style1, style2));
            given(personalityRepository.findById(1L)).willReturn(Optional.of(mock(Personality.class)));
            given(petRepository.save(any(Pet.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            PetResponse response = petService.createPet(memberId, request);

            // then
            then(walkingStyleRepository).should().findByCodeIn(request.getWalkingStyles());
            then(personalityRepository).should().findById(1L);
        }

        @Test
        @DisplayName("실패: 등록 가능 마릿수(10마리)를 초과하면 예외 발생 (P002)")
        void fail_limit_exceeded() {
            // given
            given(petRepository.countByMemberId(1L)).willReturn(10);

            // when & then
            assertThatThrownBy(() -> petService.createPet(1L, createRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.PET_LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("실패: 유효하지 않은 산책 스타일 코드가 있으면 예외 발생")
        void fail_invalid_walking_style() {
            // given
            PetCreateRequest request = createRequest();
            request.setWalkingStyles(List.of("INVALID", "VALID"));

            given(petRepository.countByMemberId(1L)).willReturn(0);
            given(breedRepository.findById(any())).willReturn(Optional.of(mock(Breed.class)));
            given(walkingStyleRepository.findByCodeIn(anyList())).willReturn(List.of(mock(WalkingStyle.class)));

            // when & then
            assertThatThrownBy(() -> petService.createPet(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.INVALID_PET_INFO);
        }

        private PetCreateRequest createRequest() {
            PetCreateRequest req = new PetCreateRequest();
            req.setName("Mong");
            req.setBreedId(1L);
            req.setBirthDate(LocalDate.of(2021, 1, 1));
            req.setGender(PetGender.MALE);
            req.setSize(PetSize.SMALL);
            req.setIsNeutered(true);
            req.setPhotoUrl("url");
            req.setIsMain(false);
            return req;
        }
    }

    @Nested
    @DisplayName("회원 반려견 목록 조회")
    class GetUserPets {

        @Test
        @DisplayName("성공: 회원의 반려견 목록을 조회한다")
        void success_get_user_pets() {
            // given
            Long memberId = 1L;

            Breed breed = mock(Breed.class);
            Pet mainPet = Pet.builder()
                    .memberId(memberId)
                    .breed(breed)
                    .name("MainDog")
                    .age(5)
                    .gender(PetGender.MALE)
                    .size(PetSize.SMALL)
                    .isMain(true)
                    .build();

            Pet subPet = Pet.builder()
                    .memberId(memberId)
                    .breed(breed)
                    .name("SubDog")
                    .age(2)
                    .gender(PetGender.FEMALE)
                    .size(PetSize.MEDIUM)
                    .isMain(false)
                    .build();

            given(petRepository.findAllByMemberIdOrderByIsMainDesc(memberId))
                    .willReturn(List.of(mainPet, subPet));

            // when
            List<PetResponse> responses = petService.getUserPets(memberId);

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getName()).isEqualTo("MainDog");
            assertThat(responses.get(0).getIsMain()).isTrue();
            assertThat(responses.get(1).getName()).isEqualTo("SubDog");
            assertThat(responses.get(1).getIsMain()).isFalse();

            then(petRepository).should(times(1)).findAllByMemberIdOrderByIsMainDesc(memberId);
        }
    }

    @Nested
    @DisplayName("반려견 정보 수정")
    class UpdatePet {

        @Test
        @DisplayName("성공: 반려견 기본 정보와 관계 정보를 수정한다")
        void success_update_pet() {
            // given
            Long memberId = 1L;
            Long petId = 100L;
            PetUpdateRequest request = updateRequest();

            Breed breed = mock(Breed.class);
            Pet pet = Pet.builder()
                    .id(petId)
                    .memberId(memberId)
                    .breed(breed)
                    .name("OldName")
                    .age(2)
                    .gender(PetGender.MALE)
                    .size(PetSize.SMALL)
                    .isNeutered(false)
                    .isMain(true)
                    .build();

            Pet existingPet = org.mockito.Mockito.spy(pet);

            given(petRepository.findById(petId)).willReturn(Optional.of(existingPet));
            given(personalityRepository.findById(1L)).willReturn(Optional.of(mock(Personality.class)));
            given(walkingStyleRepository.findByCodeIn(anyList())).willReturn(List.of(mock(WalkingStyle.class)));

            // when
            petService.updatePet(memberId, petId, request);

            // then
            assertThat(existingPet.getName()).isEqualTo(request.getName());
            assertThat(existingPet.getAge()).isEqualTo(request.resolveAge());
            assertThat(existingPet.getIsNeutered()).isEqualTo(request.getIsNeutered());

            then(existingPet).should().updateBasicInfo(any(), any(), any(), any(), any());
            then(existingPet).should().clearPersonalities();
            then(existingPet).should().clearWalkingStyles();
        }

        @Test
        @DisplayName("실패: 본인의 반려견이 아니면 수정할 수 없다 (P006)")
        void fail_not_owner() {
            // given
            Long memberId = 1L;
            Long otherMemberId = 2L;
            Long petId = 100L;
            PetUpdateRequest request = updateRequest();

            Pet pet = mock(Pet.class);
            given(pet.getMemberId()).willReturn(otherMemberId);
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));

            // when & then
            assertThatThrownBy(() -> petService.updatePet(memberId, petId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.NOT_YOUR_PET);
        }

        private PetUpdateRequest updateRequest() {
            return PetUpdateRequest.builder()
                    .name("UpdatedName")
                    .birthDate(LocalDate.of(2019, 1, 1))
                    .isNeutered(true)
                    .mbti("INTJ")
                    .photoUrl("new_url")
                    .personalityIds(List.of(1L))
                    .walkingStyleCodes(List.of("RUN"))
                    .build();
        }
    }

    @Nested
    @DisplayName("반려견 삭제")
    class DeletePet {

        @Test
        @DisplayName("성공: 메인 반려견을 삭제하면 남은 아이 중 하나가 메인이 된다")
        void success_delete_main_pet_promote_other() {
            // given
            Long memberId = 1L;
            Long petId = 100L;
            Pet mainPet = mock(Pet.class);
            Pet otherPet = mock(Pet.class);

            given(mainPet.getMemberId()).willReturn(memberId);
            given(mainPet.getIsMain()).willReturn(true);

            given(petRepository.findById(petId)).willReturn(Optional.of(mainPet));
            given(petRepository.findAllByMemberIdOrderByIsMainDesc(memberId)).willReturn(List.of(otherPet));

            // when
            petService.deletePet(memberId, petId);

            // then
            then(petRepository).should().delete(mainPet);
            then(otherPet).should().setMain(true);
            then(memberService).should(never()).downgradeToNonPetOwner(memberId);
        }

        @Test
        @DisplayName("성공: 마지막 반려견을 삭제하면 memberType이 NON_PET_OWNER로 변경된다")
        void success_delete_last_pet_downgrade_member_type() {
            // given
            Long memberId = 1L;
            Long petId = 100L;
            Pet pet = mock(Pet.class);

            given(pet.getMemberId()).willReturn(memberId);
            given(pet.getIsMain()).willReturn(true);

            given(petRepository.findById(petId)).willReturn(Optional.of(pet));
            given(petRepository.findAllByMemberIdOrderByIsMainDesc(memberId)).willReturn(List.of());

            // when
            petService.deletePet(memberId, petId);

            // then
            then(petRepository).should().delete(pet);
            then(memberService).should(times(1)).downgradeToNonPetOwner(memberId);
        }

        @Test
        @DisplayName("성공: 비메인 반려견을 삭제해도 메인 반려견은 변경되지 않는다")
        void success_delete_non_main_pet() {
            // given
            Long memberId = 1L;
            Long petId = 100L;
            Pet nonMainPet = mock(Pet.class);
            Pet mainPet = mock(Pet.class);

            given(nonMainPet.getMemberId()).willReturn(memberId);
            given(nonMainPet.getIsMain()).willReturn(false); // 비메인 반려견

            given(petRepository.findById(petId)).willReturn(Optional.of(nonMainPet));
            given(petRepository.findAllByMemberIdOrderByIsMainDesc(memberId)).willReturn(List.of(mainPet));

            // when
            petService.deletePet(memberId, petId);

            // then
            then(petRepository).should().delete(nonMainPet);
            then(mainPet).should(never()).setMain(true); // 메인 변경 안 됨
            then(memberService).should(never()).downgradeToNonPetOwner(memberId);
        }

        @Test
        @DisplayName("실패: 본인의 반려견이 아니면 삭제할 수 없다")
        void fail_delete_not_owner() {
            // given
            Long memberId = 1L;
            Long otherMemberId = 2L;
            Long petId = 100L;
            Pet pet = mock(Pet.class);
            given(pet.getMemberId()).willReturn(otherMemberId);
            given(petRepository.findById(petId)).willReturn(Optional.of(pet));

            // when & then
            assertThatThrownBy(() -> petService.deletePet(memberId, petId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.NOT_YOUR_PET);
        }
    }

    @Nested
    @DisplayName("메인 반려견 변경")
    class ChangeMainPet {

        @Test
        @DisplayName("성공: 다른 반려견을 메인으로 변경한다")
        void success_change_main_pet() {
            // given
            Long memberId = 1L;
            Long petId = 2L;

            Pet currentMainPet = mock(Pet.class);
            Pet newMainPet = mock(Pet.class);

            given(newMainPet.getId()).willReturn(petId);
            given(newMainPet.getName()).willReturn("콩이");
            given(newMainPet.getMemberId()).willReturn(memberId);
            given(newMainPet.getIsMain()).willReturn(false);

            given(petRepository.findById(petId)).willReturn(Optional.of(newMainPet));
            given(petRepository.findByMemberIdAndIsMainTrue(memberId)).willReturn(Optional.of(currentMainPet));

            // when
            MainPetChangeResponse response = petService.changeMainPet(memberId, petId);

            // then
            then(currentMainPet).should().setMain(false); // 기존 메인 해제
            then(newMainPet).should().setMain(true); // 새 메인 설정
            assertThat(response.getId()).isEqualTo(petId);
            assertThat(response.getName()).isEqualTo("콩이");
        }

        @Test
        @DisplayName("성공: 이미 메인인 반려견을 다시 메인으로 변경 요청하면 변경 없음")
        void success_already_main_no_change() {
            // given
            Long memberId = 1L;
            Long petId = 1L;

            Pet alreadyMainPet = mock(Pet.class);
            given(alreadyMainPet.getId()).willReturn(petId);
            given(alreadyMainPet.getName()).willReturn("몽이");
            given(alreadyMainPet.getMemberId()).willReturn(memberId);
            given(alreadyMainPet.getIsMain()).willReturn(true); // 이미 메인

            given(petRepository.findById(petId)).willReturn(Optional.of(alreadyMainPet));

            // when
            MainPetChangeResponse response = petService.changeMainPet(memberId, petId);

            // then
            then(petRepository).should(never()).findByMemberIdAndIsMainTrue(memberId);
            then(alreadyMainPet).should(never()).setMain(any(boolean.class));
            assertThat(response.getIsMain()).isTrue();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 반려견은 메인으로 변경할 수 없다 (P001)")
        void fail_pet_not_found() {
            // given
            Long memberId = 1L;
            Long petId = 999L;

            given(petRepository.findById(petId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> petService.changeMainPet(memberId, petId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.PET_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 본인의 반려견이 아니면 메인으로 변경할 수 없다 (P006)")
        void fail_not_your_pet() {
            // given
            Long memberId = 1L;
            Long otherMemberId = 2L;
            Long petId = 1L;

            Pet pet = mock(Pet.class);
            given(pet.getMemberId()).willReturn(otherMemberId);

            given(petRepository.findById(petId)).willReturn(Optional.of(pet));

            // when & then
            assertThatThrownBy(() -> petService.changeMainPet(memberId, petId))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetErrorCode.NOT_YOUR_PET);
        }
    }

    @Nested
    @DisplayName("마스터 데이터 조회")
    class MasterData {

        @Test
        @DisplayName("견종 목록 조회에 성공한다")
        void getAllBreeds_success() {
            Breed breed = mock(Breed.class);
            given(breed.getId()).willReturn(1L);
            given(breed.getName()).willReturn("말티즈");
            given(breed.getSize()).willReturn(PetSize.SMALL);
            given(breedRepository.findAll()).willReturn(List.of(breed));

            List<BreedResponse> response = petService.getAllBreeds();

            assertThat(response).hasSize(1);
            assertThat(response.get(0).getName()).isEqualTo("말티즈");
        }

        @Test
        @DisplayName("성향 목록 조회에 성공한다")
        void getAllPersonalities_success() {
            Personality personality = mock(Personality.class);
            given(personality.getId()).willReturn(1L);
            given(personality.getName()).willReturn("활발해요");
            given(personality.getCode()).willReturn("ACTIVE");
            given(personalityRepository.findAll()).willReturn(List.of(personality));

            List<PersonalityResponse> response = petService.getAllPersonalities();

            assertThat(response).hasSize(1);
            assertThat(response.get(0).getCode()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("산책 스타일 목록 조회에 성공한다")
        void getAllWalkingStyles_success() {
            WalkingStyle walkingStyle = mock(WalkingStyle.class);
            given(walkingStyle.getId()).willReturn(1L);
            given(walkingStyle.getName()).willReturn("느긋한 산책");
            given(walkingStyle.getCode()).willReturn("RELAXED");
            given(walkingStyleRepository.findAll()).willReturn(List.of(walkingStyle));

            List<WalkingStyleResponse> response = petService.getAllWalkingStyles();

            assertThat(response).hasSize(1);
            assertThat(response.get(0).getName()).isEqualTo("느긋한 산책");
        }
    }
}
