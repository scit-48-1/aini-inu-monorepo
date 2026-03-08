package scit.ainiinu.lostpet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.event.ContentCreatedEvent;
import scit.ainiinu.common.event.TimelineEventType;
import scit.ainiinu.lostpet.domain.Sighting;
import scit.ainiinu.lostpet.dto.SightingCreateRequest;
import scit.ainiinu.lostpet.dto.SightingResponse;
import scit.ainiinu.lostpet.integration.ai.LostPetAiClient;
import scit.ainiinu.lostpet.repository.SightingRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class SightingService {

    private final SightingRepository sightingRepository;
    private final LostPetAiClient lostPetAiClient;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SightingResponse create(Long memberId, SightingCreateRequest request) {
        Sighting saved = sightingRepository.save(Sighting.create(
                memberId,
                request.getPhotoUrl(),
                request.getFoundAt(),
                request.getFoundLocation(),
                request.getMemo()
        ));
        try {
            lostPetAiClient.indexSighting(saved);
        } catch (Exception exception) {
            log.warn(
                    "lostpet.sighting.index failed sightingId={} reason={}",
                    saved.getId(),
                    exception.getClass().getSimpleName()
            );
        }
        eventPublisher.publishEvent(ContentCreatedEvent.of(
                memberId, saved.getId(), TimelineEventType.SIGHTING_CREATED,
                saved.getFoundLocation(), null, saved.getPhotoUrl()));

        return SightingResponse.builder()
                .sightingId(saved.getId())
                .status(saved.getStatus().name())
                .foundAt(saved.getFoundAt())
                .build();
    }
}
