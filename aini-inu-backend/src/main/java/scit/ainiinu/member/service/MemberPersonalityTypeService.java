package scit.ainiinu.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.member.dto.response.MemberPersonalityTypeResponse;
import scit.ainiinu.member.repository.MemberPersonalityTypeRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberPersonalityTypeService {

    private final MemberPersonalityTypeRepository personalityTypeRepository;

    public List<MemberPersonalityTypeResponse> getAllPersonalityTypes() {
        return personalityTypeRepository.findAll().stream()
            .map(MemberPersonalityTypeResponse::from)
            .collect(Collectors.toList());
    }
}
