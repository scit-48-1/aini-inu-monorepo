package scit.ainiinu.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;
import scit.ainiinu.member.entity.MemberPersonalityType;

@Getter
public class MemberPersonalityTypeResponse {
    @Schema(description = "리소스 식별자입니다.", example = "101")
    private final Long id;
    @Schema(description = "이름입니다.", example = "몽이")
    private final String name;
    @Schema(description = "코드 문자열입니다.", example = "C002")
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
