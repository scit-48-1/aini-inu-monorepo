package scit.ainiinu.lostpet.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import scit.ainiinu.lostpet.domain.LostPetReportStatus;
import scit.ainiinu.lostpet.dto.LostPetCreateRequest;
import scit.ainiinu.lostpet.dto.LostPetDetailResponse;
import scit.ainiinu.lostpet.dto.LostPetResponse;
import scit.ainiinu.lostpet.dto.LostPetSummaryResponse;

public interface LostPetService {
    LostPetResponse create(Long memberId, LostPetCreateRequest request);

    Slice<LostPetSummaryResponse> list(Long memberId, LostPetReportStatus status, Pageable pageable);

    LostPetDetailResponse detail(Long memberId, Long lostPetId);
}
