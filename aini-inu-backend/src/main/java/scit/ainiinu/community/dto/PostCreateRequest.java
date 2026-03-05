package scit.ainiinu.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//게시글작성
@Data
@NoArgsConstructor
public class PostCreateRequest {
    @Size(max = 2000, message = "게시글 내용은 2000자를 초과할 수 없습니다.")
    @Schema(description = "본문 내용입니다.", example = "강아지와 즐거운 산책을 했어요.")
    private String content;

    @Size(max = 2000, message = "게시글 캡션은 2000자를 초과할 수 없습니다.")
    @Schema(description = "캡션 텍스트입니다.", example = "예시 문자열")
    private String caption; // FE UI 라벨 호환용

    @Size(max = 5)
    @Schema(description = "이미지 URL 목록입니다.", example = "[\"https://cdn.example.com/sample.jpg\"]")
    private List<String> imageUrls;

    @AssertTrue(message = "content 또는 caption 중 하나는 필수입니다.")
    public boolean isContentProvided() {
        return hasText(content) || hasText(caption);
    }

    public String getResolvedContent() {
        if (hasText(content)) {
            return content;
        }
        return caption;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
