package scit.ainiinu.pet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import scit.ainiinu.pet.exception.PetErrorCode;
import scit.ainiinu.pet.repository.BreedRepository;
import scit.ainiinu.pet.repository.PersonalityRepository;
import scit.ainiinu.pet.repository.PetRepository;
import scit.ainiinu.pet.repository.WalkingStyleRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private final PetRepository petRepository;
    private final BreedRepository breedRepository;
    private final PersonalityRepository personalityRepository;
    private final WalkingStyleRepository walkingStyleRepository;
    private final MemberService memberService;

    /**
     * 반려견 등록
     */
    @Transactional
    public PetResponse createPet(Long memberId, PetCreateRequest request) {
        // 1. 등록 가능 마릿수(10마리) 확인
        int currentCount = petRepository.countByMemberId(memberId);
        if (currentCount >= 10) {
            throw new BusinessException(PetErrorCode.PET_LIMIT_EXCEEDED);
        }

        Integer resolvedAge = request.resolveAge();
        if (resolvedAge == null) {
            throw new BusinessException(PetErrorCode.INVALID_PET_INFO);
        }

        // 2. 견종 조회
        Breed breed = breedRepository.findById(request.getBreedId())
                .orElseThrow(() -> new BusinessException(PetErrorCode.BREED_NOT_FOUND));

        // 3. 메인 반려견 설정 로직
        boolean isMain = false;
        if (currentCount == 0) {
            isMain = true;
        } else if (request.getIsMain() != null && request.getIsMain()) {
            petRepository.findByMemberIdAndIsMainTrue(memberId)
                    .ifPresent(mainPet -> mainPet.setMain(false));
            isMain = true;
        }

        // 4. Pet 엔티티 생성
        Pet pet = Pet.builder()
                .memberId(memberId)
                .breed(breed)
                .name(request.getName())
                .age(resolvedAge)
                .gender(request.getGender())
                .size(request.getSize())
                .mbti(request.getMbti())
                .isNeutered(request.getIsNeutered())
                .photoUrl(request.getPhotoUrl())
                .isMain(isMain)
                .build();

        // 5. 성향(Personality) 및 산책 스타일 관계 설정
        if (request.getPersonalityIds() != null) {
            for (Long pId : request.getPersonalityIds()) {
                Personality personality = personalityRepository.findById(pId)
                        .orElseThrow(() -> new BusinessException(PetErrorCode.PERSONALITY_NOT_FOUND));
                pet.addPersonality(personality);
            }
        }

        if (request.getWalkingStyles() != null && !request.getWalkingStyles().isEmpty()) {
            List<WalkingStyle> styles = walkingStyleRepository.findByCodeIn(request.getWalkingStyles());
            if (styles.size() != request.getWalkingStyles().size()) {
                 throw new BusinessException(PetErrorCode.INVALID_PET_INFO);
            }
            styles.forEach(pet::addWalkingStyle);
        }

        // 6. 저장
        Pet savedPet = petRepository.save(pet);

        // 7. 첫 반려견 등록 시 회원 타입 업그레이드
        if (currentCount == 0) {
            memberService.upgradeToPetOwner(memberId);
        }

        return toResponse(savedPet);
    }

    /**
     * 반려견 정보 수정
     */
    @Transactional
    public PetResponse updatePet(Long memberId, Long petId, PetUpdateRequest request) {
        // 1. 반려견 조회
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(PetErrorCode.PET_NOT_FOUND));

        // 2. 권한 확인 (내 반려견인지?)
        if (!pet.getMemberId().equals(memberId)) {
            throw new BusinessException(PetErrorCode.NOT_YOUR_PET);
        }

        // 3. 기본 정보 수정 (Dirty Checking)
        pet.updateBasicInfo(
                request.getName(),
                request.resolveAge(),
                request.getIsNeutered(),
                request.getMbti(),
                request.getPhotoUrl()
        );

        // 4. 성향(Personality) 수정
        if (request.getPersonalityIds() != null) {
            pet.clearPersonalities();
            for (Long pId : request.getPersonalityIds()) {
                Personality personality = personalityRepository.findById(pId)
                        .orElseThrow(() -> new BusinessException(PetErrorCode.PERSONALITY_NOT_FOUND));
                pet.addPersonality(personality);
            }
        }

        // 5. 산책 스타일 수정
        List<String> resolvedWalkingStyleCodes = request.resolveWalkingStyleCodes();
        if (resolvedWalkingStyleCodes != null) {
            pet.clearWalkingStyles();
            if (!resolvedWalkingStyleCodes.isEmpty()) {
                List<WalkingStyle> styles = walkingStyleRepository.findByCodeIn(resolvedWalkingStyleCodes);
                if (styles.size() != resolvedWalkingStyleCodes.size()) {
                    throw new BusinessException(PetErrorCode.INVALID_PET_INFO);
                }
                styles.forEach(pet::addWalkingStyle);
            }
        }

        return toResponse(pet);
    }

    /**
     * 반려견 삭제
     */
    @Transactional
    public void deletePet(Long memberId, Long petId) {
        // 1. 반려견 조회
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(PetErrorCode.PET_NOT_FOUND));

        // 2. 권한 확인
        if (!pet.getMemberId().equals(memberId)) {
            throw new BusinessException(PetErrorCode.NOT_YOUR_PET);
        }

        boolean wasMain = pet.getIsMain();

        // 3. 삭제
        petRepository.delete(pet);

        // 4. 후속 처리 로직
        List<Pet> remainingPets = petRepository.findAllByMemberIdOrderByIsMainDesc(memberId);

        if (remainingPets.isEmpty()) {
            // 마지막 반려견 삭제 시 회원 타입 다운그레이드
            memberService.downgradeToNonPetOwner(memberId);
        } else if (wasMain) {
            // 삭제된 반려견이 메인이었다면, 남은 반려견 중 하나를 자동으로 메인으로 승격
            Pet newMainPet = remainingPets.get(0);
            newMainPet.setMain(true);
        }
    }

    /**
     * 회원의 반려견 목록 조회
     */
    public List<PetResponse> getUserPets(Long memberId) {
        return petRepository.findAllByMemberIdOrderByIsMainDesc(memberId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 메인 반려견 변경
     */
    @Transactional
    public MainPetChangeResponse changeMainPet(Long memberId, Long petId) {
        // 1. 변경 대상 반려견 조회
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(PetErrorCode.PET_NOT_FOUND));

        // 2. 권한 확인
        if (!pet.getMemberId().equals(memberId)) {
            throw new BusinessException(PetErrorCode.NOT_YOUR_PET);
        }

        // 3. 이미 메인인 경우 변경 없음
        if (pet.getIsMain()) {
            return MainPetChangeResponse.from(pet);
        }

        // 4. 기존 메인 반려견 해제
        petRepository.findByMemberIdAndIsMainTrue(memberId)
                .ifPresent(mainPet -> mainPet.setMain(false));

        // 5. 새로운 메인 반려견 설정
        pet.setMain(true);

        return MainPetChangeResponse.from(pet);
    }

    /**
     * 전체 견종 목록 조회
     */
    public List<BreedResponse> getAllBreeds() {
        return breedRepository.findAll().stream()
                .map(BreedResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 전체 성격 목록 조회
     */
    public List<PersonalityResponse> getAllPersonalities() {
        return personalityRepository.findAll().stream()
                .map(PersonalityResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 전체 산책 스타일 목록 조회
     */
    public List<WalkingStyleResponse> getAllWalkingStyles() {
        return walkingStyleRepository.findAll().stream()
                .map(WalkingStyleResponse::from)
                .collect(Collectors.toList());
    }
    
    private PetResponse toResponse(Pet pet) {
        List<String> walkingStyleCodes = pet.getPetWalkingStyles().stream()
                .map(pws -> pws.getWalkingStyle().getCode())
                .collect(Collectors.toList());
        
        List<PersonalityResponse> personalityResponses = pet.getPetPersonalities().stream()
                .map(pp -> PersonalityResponse.from(pp.getPersonality()))
                .collect(Collectors.toList());

        return PetResponse.from(pet, walkingStyleCodes, personalityResponses);
    }
}
