package scit.ainiinu.member.dto.response;

import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.member.entity.MemberPersonalityType;

@Getter
public class MemberPersonalityTypeResponse {
    private final Long id;
    private final String name;
    private final String code;

    @Builder
    private MemberPersonalityTypeResponse(Long id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
    }

    public static MemberPersonalityTypeResponse from(MemberPersonalityType entity) {
        return MemberPersonalityTypeResponse.builder()
            .id(entity.getId())
            .name(entity.getName())
            .code(entity.getCode())
            .build();
    }
}
