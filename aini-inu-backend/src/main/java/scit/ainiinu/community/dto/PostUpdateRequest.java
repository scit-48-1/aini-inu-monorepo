package scit.ainiinu.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PostUpdateRequest {

    @Size(max = 2000, message = "게시글 내용은 2000자를 초과할 수 없습니다.")
    @Schema(description = "게시글 본문입니다. null이면 변경하지 않습니다.", example = "강아지와 즐거운 산책을 했어요.")
    private String content;

    @Size(max = 2000, message = "게시글 캡션은 2000자를 초과할 수 없습니다.")
    @Schema(description = "게시글 캡션입니다. null이면 변경하지 않습니다.", example = "예시 문자열")
    private String caption; // FE UI 라벨 호환용

    @Size(max = 5, message = "이미지는 최대 5개까지 업로드 가능합니다.")
    @Schema(description = "게시글 이미지 URL 목록입니다. null이면 변경하지 않고, [] 전달 시 전체 해제합니다.", example = "[\"https://cdn.example.com/sample.jpg\"]")
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
