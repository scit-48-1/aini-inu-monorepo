package scit.ainiinu.community.dto;

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
    private String content;

    @Size(max = 2000, message = "게시글 캡션은 2000자를 초과할 수 없습니다.")
    private String caption; // FE UI 라벨 호환용

    @Size(max = 5)
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
