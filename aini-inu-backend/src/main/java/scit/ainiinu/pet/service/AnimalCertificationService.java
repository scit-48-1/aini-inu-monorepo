package scit.ainiinu.pet.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.pet.exception.PetErrorCode;

import scit.ainiinu.pet.entity.enums.PetGender;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 동물등록번호 검증 서비스 (이슈 #36)
 * 실제 공공데이터 API 연동
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnimalCertificationService {

    @Value("${animal.registry.api.key}")
    private String serviceKey;

    @Value("${animal.registry.api.url}")
    private String apiUrl;

    // 15자리 숫자 정규식
    private static final Pattern CERTIFICATION_NUMBER_PATTERN = Pattern.compile("^\\d{15}$");

    private final RestClient restClient = RestClient.create();

    /**
     * 동물등록번호 검증 (기본 - 존재 여부만 확인)
     *
     * @param certificationNumber 동물등록번호 (15자리 숫자)
     * @return 검증 성공 여부
     */
    public boolean verify(String certificationNumber) {
        return verify(certificationNumber, null, null);
    }

    /**
     * 동물등록번호 검증 (견종 및 성별 일치 여부 확인)
     *
     * @param certificationNumber 동물등록번호 (15자리 숫자)
     * @param breedName 견종 이름 (null이면 검증 생략)
     * @param gender 성별 (null이면 검증 생략)
     * @return 검증 성공 여부
     */
    public boolean verify(String certificationNumber, String breedName, PetGender gender) {
        if (certificationNumber == null || certificationNumber.isBlank()) {
            return false;
        }

        // 1. 형식 검증
        if (!CERTIFICATION_NUMBER_PATTERN.matcher(certificationNumber).matches()) {
            throw new BusinessException(PetErrorCode.INVALID_CERTIFICATION_NUMBER);
        }

        // 2. 외부 API 호출
        try {
            return callExternalApi(certificationNumber, breedName, gender);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Animal Certification API Error", e);
            throw new BusinessException(PetErrorCode.CERTIFICATION_API_ERROR);
        }
    }

    private boolean callExternalApi(String certificationNumber, String breedName, PetGender gender) {
        log.info("Calling Animal Registry API for number: {}", certificationNumber);

        // API 요청 URL 생성
        // 주의: 공공데이터포털 키는 인코딩 이슈가 잦으므로 URI를 직접 빌드하여 처리하는 것이 안전함
        // 여기서는 Decoding된 키를 사용한다고 가정하고 UriComponentsBuilder 사용
        URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("dog_reg_no", certificationNumber)
                .queryParam("owner_nm", "") // 소유자명 필수 여부에 따라 빈 값 또는 사용자명 삽입 필요. 현재는 빈 값 시도.
                .queryParam("_type", "json")
                .build()
                .toUri();
        
        try {
            AnimalApiDto response = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(AnimalApiDto.class);

            if (response == null || response.getResponse() == null || response.getResponse().getBody() == null) {
                log.error("API Response is null");
                throw new BusinessException(PetErrorCode.CERTIFICATION_API_ERROR);
            }

            AnimalApiDto.Body body = response.getResponse().getBody();
            
            // 결과 아이템이 존재하면 등록된 동물로 간주
            if (body.getItems() != null && body.getItems().getItem() != null && !body.getItems().getItem().isEmpty()) {
                AnimalApiDto.Item item = body.getItems().getItem().get(0);
                log.info("Animal Verification Found: kindNm={}, sexNm={}", item.getKindNm(), item.getSexNm());

                // 견종 이름 검증 (요청 시 전달된 경우에만)
                if (breedName != null && item.getKindNm() != null) {
                    if (!item.getKindNm().contains(breedName) && !breedName.contains(item.getKindNm())) {
                        log.warn("Breed mismatch: expected={}, actual={}", breedName, item.getKindNm());
                        throw new BusinessException(PetErrorCode.CERTIFICATION_INFO_MISMATCH);
                    }
                }

                // 성별 검증 (요청 시 전달된 경우에만)
                if (gender != null && item.getSexNm() != null) {
                    String expectedSex = gender == PetGender.MALE ? "수컷" : "암컷";
                    if (!item.getSexNm().contains(expectedSex)) {
                        log.warn("Gender mismatch: expected={}, actual={}", expectedSex, item.getSexNm());
                        throw new BusinessException(PetErrorCode.CERTIFICATION_INFO_MISMATCH);
                    }
                }

                log.info("Animal Verification Success. Found {} items.", body.getItems().getItem().size());
                return true;
            } else {
                log.warn("Animal Verification Failed. No items found.");
                // 정보 불일치 혹은 데이터 없음 -> P011 (정보 불일치)로 통일하거나 P001 등 사용
                throw new BusinessException(PetErrorCode.CERTIFICATION_INFO_MISMATCH);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call API", e);
            throw new BusinessException(PetErrorCode.CERTIFICATION_API_ERROR);
        }
    }

    // --- Inner DTOs for JSON Response ---
    @Data
    static class AnimalApiDto {
        private Response response;

        @Data
        static class Response {
            private Header header;
            private Body body;
        }

        @Data
        static class Header {
            private String reqNo;
            private String resultCode;
            private String resultMsg;
        }

        @Data
        static class Body {
            private Items items;
        }

        @Data
        static class Items {
            private List<Item> item;
        }

        @Data
        static class Item {
            @JsonProperty("dogRegNo")
            private String dogRegNo;
            
            @JsonProperty("dogNm")
            private String dogNm;
            
            @JsonProperty("sexNm")
            private String sexNm;
            
            @JsonProperty("kindNm")
            private String kindNm;
            
            @JsonProperty("neuterYn")
            private String neuterYn;
        }
    }
}
