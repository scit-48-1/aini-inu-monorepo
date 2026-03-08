package scit.ainiinu.lostpet.integration.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import scit.ainiinu.lostpet.domain.Sighting;
import scit.ainiinu.lostpet.dto.LostPetAnalyzeRequest;

@Component
@Slf4j
public class LostPetAiClientImpl implements LostPetAiClient {

    private final ObjectProvider<VectorStore> vectorStoreProvider;

    @Value("${lostpet.ai.vector-top-k:50}")
    private int vectorTopK;

    public LostPetAiClientImpl(ObjectProvider<VectorStore> vectorStoreProvider) {
        this.vectorStoreProvider = vectorStoreProvider;
    }

    @Override
    public LostPetAiResult analyze(LostPetAnalyzeRequest request) {
        return analyzeWithSpringAi(request);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void indexSighting(Sighting sighting) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            log.warn("lostpet.vector.index skipped reason=vector-store-unavailable sightingId={}", sighting.getId());
            return;
        }
        if (sighting.getId() == null) {
            log.warn("lostpet.vector.index skipped reason=sighting-id-null");
            return;
        }
        try {
            vectorStore.add(List.of(toSightingDocument(sighting)));
            log.info("lostpet.vector.index success sightingId={} finderId={}", sighting.getId(), sighting.getFinderId());
        } catch (Exception exception) {
            log.warn(
                    "lostpet.vector.index failed sightingId={} reason={}",
                    sighting.getId(),
                    exception.getClass().getSimpleName()
            );
        }
    }

    private LostPetAiResult analyzeWithSpringAi(LostPetAnalyzeRequest request) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            throw new IllegalStateException("Spring AI VectorStore is not available");
        }
        String queryText = buildAnalyzeQueryText(request);
        List<Document> searched = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(queryText)
                        .topK(vectorTopK)
                        .build()
        );
        if (searched == null || searched.isEmpty()) {
            return new LostPetAiResult("spring_ai_vector_search_empty", List.of());
        }

        List<LostPetAiCandidate> candidates = searched.stream()
                .map(this::toCandidate)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                LostPetAiCandidate::sightingId,
                                candidate -> candidate,
                                (left, right) -> left,
                                LinkedHashMap::new
                        ),
                        map -> List.copyOf(map.values())
                ));

        return new LostPetAiResult("spring_ai_vector_search", candidates);
    }

    private Document toSightingDocument(Sighting sighting) {
        String content = String.join(
                " ",
                safe(sighting.getPhotoUrl()),
                safe(sighting.getFoundLocation()),
                safe(sighting.getMemo()),
                sighting.getFoundAt() == null ? "" : sighting.getFoundAt().toString()
        ).trim();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sightingId", sighting.getId());
        metadata.put("finderId", sighting.getFinderId());
        metadata.put("foundLocation", sighting.getFoundLocation());
        metadata.put("foundAt", sighting.getFoundAt() == null ? null : sighting.getFoundAt().toString());
        metadata.put("photoUrl", sighting.getPhotoUrl());
        metadata.put("memo", sighting.getMemo());

        return new Document(UUID.randomUUID().toString(), content, metadata);
    }

    private String buildAnalyzeQueryText(LostPetAnalyzeRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(safe(request.resolveMode())).append(' ');
        builder.append(safe(request.getQueryText())).append(' ');
        if (request.getLatitude() != null && request.getLongitude() != null) {
            builder.append(request.getLatitude()).append(',').append(request.getLongitude());
        }
        return builder.toString().trim();
    }

    private LostPetAiCandidate toCandidate(Document document) {
        Long sightingId = toLong(document.getMetadata().get("sightingId"));
        if (sightingId == null && document.getId() != null && document.getId().startsWith("sighting-")) {
            sightingId = toLong(document.getId().substring("sighting-".length()));
        }
        if (sightingId == null) {
            return null;
        }
        Long finderId = toLong(document.getMetadata().get("finderId"));
        return new LostPetAiCandidate(sightingId, finderId, normalizeScore(document.getScore()));
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private BigDecimal normalizeScore(double score) {
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            return BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP);
        }
        BigDecimal normalized = BigDecimal.valueOf(score).setScale(5, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP);
        }
        if (normalized.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE.setScale(5, RoundingMode.HALF_UP);
        }
        return normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
