package scit.ainiinu.lostpet.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import scit.ainiinu.lostpet.domain.Sighting;
import scit.ainiinu.lostpet.dto.SightingCreateRequest;
import scit.ainiinu.lostpet.dto.SightingResponse;
import scit.ainiinu.lostpet.integration.ai.LostPetAiClient;
import scit.ainiinu.lostpet.repository.SightingRepository;
import scit.ainiinu.lostpet.service.SightingService;

@ExtendWith(MockitoExtension.class)
class SightingServiceUnitTest {

    @Mock
    private SightingRepository sightingRepository;

    @Mock
    private LostPetAiClient lostPetAiClient;

    @InjectMocks
    private SightingService sightingService;

    @Test
    @DisplayName("목격 제보 저장 후 벡터 인덱싱을 시도한다")
    void createAndIndex() {
        SightingCreateRequest request = SightingCreateRequest.builder()
                .photoUrl("https://cdn/sightings/1.jpg")
                .foundAt(LocalDateTime.now())
                .foundLocation("Yeoksam")
                .memo("memo")
                .build();

        given(sightingRepository.save(any(Sighting.class))).willAnswer(invocation -> {
            Sighting sighting = invocation.getArgument(0);
            sighting.assignIdForTest(1L);
            return sighting;
        });

        SightingResponse response = sightingService.create(22L, request);

        assertThat(response.sightingId()).isEqualTo(1L);
        verify(lostPetAiClient).indexSighting(any(Sighting.class));
    }

    @Test
    @DisplayName("인덱싱 실패가 발생해도 제보 생성은 성공한다")
    void createWhenIndexFails() {
        SightingCreateRequest request = SightingCreateRequest.builder()
                .photoUrl("https://cdn/sightings/1.jpg")
                .foundAt(LocalDateTime.now())
                .foundLocation("Yeoksam")
                .memo("memo")
                .build();

        given(sightingRepository.save(any(Sighting.class))).willAnswer(invocation -> {
            Sighting sighting = invocation.getArgument(0);
            sighting.assignIdForTest(2L);
            return sighting;
        });
        doThrow(new RuntimeException("index failed")).when(lostPetAiClient).indexSighting(any(Sighting.class));

        SightingResponse response = sightingService.create(22L, request);

        assertThat(response.sightingId()).isEqualTo(2L);
    }
}
