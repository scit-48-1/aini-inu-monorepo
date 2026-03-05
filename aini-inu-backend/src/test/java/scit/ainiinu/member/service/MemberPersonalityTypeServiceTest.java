package scit.ainiinu.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import scit.ainiinu.member.dto.response.MemberPersonalityTypeResponse;
import scit.ainiinu.member.entity.MemberPersonalityType;
import scit.ainiinu.member.repository.MemberPersonalityTypeRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberPersonalityTypeServiceTest {

    @InjectMocks
    private MemberPersonalityTypeService memberPersonalityTypeService;

    @Mock
    private MemberPersonalityTypeRepository memberPersonalityTypeRepository;

    @DisplayName("모든 회원 성격 유형 목록을 조회한다.")
    @Test
    void getAllPersonalityTypes() {
        // given
        MemberPersonalityType type1 = new MemberPersonalityType("동네친구", "LOCAL_FRIEND");
        MemberPersonalityType type2 = new MemberPersonalityType("반려견정보공유", "PET_INFO_SHARING");
        
        given(memberPersonalityTypeRepository.findAll())
            .willReturn(List.of(type1, type2));

        // when
        List<MemberPersonalityTypeResponse> responses = memberPersonalityTypeService.getAllPersonalityTypes();

        // then
        assertThat(responses).hasSize(2)
            .extracting("code")
            .containsExactly("LOCAL_FRIEND", "PET_INFO_SHARING");
            
        assertThat(responses).extracting("name")
            .containsExactly("동네친구", "반려견정보공유");
    }
}
