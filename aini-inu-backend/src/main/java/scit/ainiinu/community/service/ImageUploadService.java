package scit.ainiinu.community.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.common.exception.BusinessException;
import scit.ainiinu.community.config.CommunityStorageProperties;
import scit.ainiinu.community.dto.PresignedImageRequest;
import scit.ainiinu.community.dto.PresignedImageResponse;
import scit.ainiinu.community.dto.UploadPurpose;
import scit.ainiinu.community.exception.CommunityErrorCode;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageUploadService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final CommunityStorageProperties properties;
    private final Map<String, PresignedUploadContext> presignedUploadContexts = new ConcurrentHashMap<>();

    @Transactional
    public PresignedImageResponse createPresignedUrl(Long memberId, PresignedImageRequest request) {
        UploadPurpose purpose = UploadPurpose.from(request.getPurpose());
        String normalizedContentType = normalizeContentType(request.getContentType());
        String objectKey = buildObjectKey(memberId, purpose, normalizedContentType);
        String token = UUID.randomUUID().toString();
        long expiresInSeconds = properties.getPresigned().getExpiresSeconds();
        Instant expiresAt = Instant.now().plusSeconds(expiresInSeconds);

        presignedUploadContexts.put(token, new PresignedUploadContext(objectKey, normalizedContentType, expiresAt));

        return PresignedImageResponse.builder()
                .uploadUrl(buildUploadUrl(token))
                .imageUrl(buildImageUrl(objectKey))
                .expiresIn(expiresInSeconds)
                .maxFileSizeBytes(MAX_FILE_SIZE_BYTES)
                .build();
    }

    @Transactional
    public void uploadByToken(String token, String contentType, byte[] payload) {
        PresignedUploadContext context = consumeContext(token);
        if (!Instant.now().isBefore(context.expiresAt())) {
            throw new BusinessException(CommunityErrorCode.UPLOAD_URL_EXPIRED_OR_INVALID);
        }

        validatePayload(contentType, payload, context.contentType());
        Path target = resolveAndValidatePath(context.objectKey());
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, payload);
        } catch (IOException e) {
            throw new BusinessException(CommunityErrorCode.STORAGE_UNAVAILABLE, e);
        }
    }

    public Resource getLocalImage(String key) {
        String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);
        Path path = resolveAndValidatePath(decodedKey);
        if (!Files.exists(path)) {
            throw new BusinessException(CommunityErrorCode.UPLOAD_URL_EXPIRED_OR_INVALID);
        }
        return new FileSystemResource(path);
    }

    private void validatePayload(String contentType, byte[] payload, String expectedContentType) {
        if (payload == null || payload.length == 0) {
            throw new BusinessException(CommunityErrorCode.INVALID_UPLOAD_PURPOSE);
        }

        if (payload.length > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(CommunityErrorCode.FILE_SIZE_EXCEEDED);
        }

        String normalized = normalizeContentType(contentType);
        if (!normalized.equals(expectedContentType)) {
            throw new BusinessException(CommunityErrorCode.INVALID_UPLOAD_MIME);
        }
    }

    private PresignedUploadContext consumeContext(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(CommunityErrorCode.UPLOAD_URL_EXPIRED_OR_INVALID);
        }
        PresignedUploadContext context = presignedUploadContexts.remove(token);
        if (context == null) {
            throw new BusinessException(CommunityErrorCode.UPLOAD_URL_EXPIRED_OR_INVALID);
        }
        return context;
    }

    private String buildObjectKey(Long memberId, UploadPurpose purpose, String contentType) {
        LocalDate today = LocalDate.now();
        String extension = resolveExtension(contentType);
        return String.join("/", List.of(
                "community",
                purpose.name().toLowerCase(),
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth()),
                String.valueOf(memberId),
                UUID.randomUUID() + "." + extension
        ));
    }

    private String buildUploadUrl(String token) {
        return "/api/v1/images/presigned-upload/" + token;
    }

    private Path resolveAndValidatePath(String objectKey) {
        Path baseDir = Path.of(properties.getLocal().getBaseDir()).toAbsolutePath().normalize();
        Path resolved = baseDir.resolve(objectKey).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new BusinessException(CommunityErrorCode.INVALID_UPLOAD_PURPOSE);
        }
        return resolved;
    }

    private String buildImageUrl(String objectKey) {
        String encodedKey = URLEncoder.encode(objectKey, StandardCharsets.UTF_8);
        return "/api/v1/images/local?key=" + encodedKey;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessException(CommunityErrorCode.INVALID_UPLOAD_MIME);
        }
        String normalized = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_MIME_TYPES.contains(normalized)) {
            throw new BusinessException(CommunityErrorCode.INVALID_UPLOAD_MIME);
        }
        return normalized;
    }

    private String resolveExtension(String contentType) {
        if ("image/png".equals(contentType)) {
            return "png";
        }
        if ("image/webp".equals(contentType)) {
            return "webp";
        }
        return "jpg";
    }

    private record PresignedUploadContext(
            String objectKey,
            String contentType,
            Instant expiresAt
    ) {}
}
