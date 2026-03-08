package scit.ainiinu.lostpet.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import scit.ainiinu.lostpet.domain.Sighting;
import scit.ainiinu.lostpet.dto.LostPetAnalyzeRequest;
import scit.ainiinu.lostpet.integration.ai.LostPetAiCandidate;
import scit.ainiinu.lostpet.integration.ai.LostPetAiClientImpl;
import scit.ainiinu.lostpet.integration.ai.LostPetAiResult;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LostPetAiClientImplUnitTest {

    @Mock
    private ObjectProvider<VectorStore> vectorStoreProvider;

    @Mock
    private VectorStore vectorStore;

    private LostPetAiClientImpl lostPetAiClient;

    @BeforeEach
    void setUp() {
        this.lostPetAiClient = new LostPetAiClientImpl(vectorStoreProvider);
        ReflectionTestUtils.setField(lostPetAiClient, "vectorTopK", 7);
    }

    @Nested
    @DisplayName("analyze")
    class Analyze {

        @Test
        @DisplayName("spring-ai 파이프라인에서 후보를 점수 정규화/중복 제거해 반환한다")
        void analyzeWithSpringAi() {
            given(vectorStoreProvider.getIfAvailable()).willReturn(vectorStore);

            Document highScore = Document.builder()
                    .id("sighting-101")
                    .text("high score")
                    .metadata("sightingId", 101L)
                    .metadata("finderId", 11L)
                    .score(1.8)
                    .build();
            Document lowScore = Document.builder()
                    .id("sighting-102")
                    .text("low score")
                    .metadata("sightingId", 102L)
                    .metadata("finderId", 12L)
                    .score(-0.4)
                    .build();
            Document duplicated = Document.builder()
                    .id("sighting-101")
                    .text("duplicated candidate")
                    .metadata("sightingId", 101L)
                    .metadata("finderId", 11L)
                    .score(0.5)
                    .build();
            Document malformed = Document.builder()
                    .id("invalid")
                    .text("malformed")
                    .score(0.7)
                    .build();

            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willReturn(List.of(highScore, lowScore, duplicated, malformed));

            LostPetAiResult result = lostPetAiClient.analyze(sampleRequest());

            assertThat(result.summary()).isEqualTo("spring_ai_vector_search");
            assertThat(result.candidates()).hasSize(2);
            LostPetAiCandidate first = result.candidates().get(0);
            LostPetAiCandidate second = result.candidates().get(1);
            assertThat(first.sightingId()).isEqualTo(101L);
            assertThat(first.similarityTotal()).hasToString("1.00000");
            assertThat(second.sightingId()).isEqualTo(102L);
            assertThat(second.similarityTotal()).hasToString("0.00000");

            ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
            verify(vectorStore).similaritySearch(requestCaptor.capture());
            assertThat(requestCaptor.getValue().getTopK()).isEqualTo(7);
            assertThat(requestCaptor.getValue().getQuery()).contains("LOST");
        }

        @Test
        @DisplayName("쿼리 텍스트에 imageUrl이 포함되지 않고 queryText만 포함된다")
        void queryTextDoesNotContainImageUrl() {
            given(vectorStoreProvider.getIfAvailable()).willReturn(vectorStore);
            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willReturn(List.of());

            LostPetAnalyzeRequest request = LostPetAnalyzeRequest.builder()
                    .lostPetId(1L)
                    .imageUrl("https://cdn/should-not-appear.jpg")
                    .mode("LOST")
                    .queryText("하얀 포메라니안 서울시 강남구")
                    .build();

            lostPetAiClient.analyze(request);

            ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
            verify(vectorStore).similaritySearch(captor.capture());
            String query = captor.getValue().getQuery();
            assertThat(query).doesNotContain("https://cdn/should-not-appear.jpg");
            assertThat(query).contains("하얀 포메라니안 서울시 강남구");
            assertThat(query).contains("LOST");
        }

        @Test
        @DisplayName("queryText와 좌표가 모두 포함된 쿼리를 생성한다")
        void queryIncludesQueryTextAndCoordinates() {
            given(vectorStoreProvider.getIfAvailable()).willReturn(vectorStore);
            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willReturn(List.of());

            LostPetAnalyzeRequest request = LostPetAnalyzeRequest.builder()
                    .lostPetId(1L)
                    .imageUrl("https://cdn/photo.jpg")
                    .mode("LOST")
                    .queryText("갈색 말티즈 빨간 목줄")
                    .latitude(37.50)
                    .longitude(127.03)
                    .build();

            lostPetAiClient.analyze(request);

            ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
            verify(vectorStore).similaritySearch(captor.capture());
            String query = captor.getValue().getQuery();
            assertThat(query).contains("갈색 말티즈 빨간 목줄");
            assertThat(query).contains("37.5");
            assertThat(query).contains("127.03");
            assertThat(query).doesNotContain("cdn/photo.jpg");
        }

        @Test
        @DisplayName("queryText가 없으면 mode만으로 쿼리가 구성된다")
        void queryWithOnlyMode() {
            given(vectorStoreProvider.getIfAvailable()).willReturn(vectorStore);
            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willReturn(List.of());

            LostPetAnalyzeRequest request = LostPetAnalyzeRequest.builder()
                    .lostPetId(1L)
                    .imageUrl("https://cdn/photo.jpg")
                    .build();

            lostPetAiClient.analyze(request);

            ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
            verify(vectorStore).similaritySearch(captor.capture());
            String query = captor.getValue().getQuery();
            assertThat(query).isEqualTo("LOST");
        }

        @Test
        @DisplayName("spring-ai 파이프라인에서 VectorStore 빈이 없으면 예외를 던진다")
        void analyzeWithSpringAiWithoutVectorStore() {
            given(vectorStoreProvider.getIfAvailable()).willReturn(null);

            assertThatThrownBy(() -> lostPetAiClient.analyze(sampleRequest()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("VectorStore");
        }

    }

    @Test
    @DisplayName("sighting 저장 후 spring-ai 파이프라인이면 벡터 인덱싱을 시도한다")
    void indexSightingWithSpringAi() {
        given(vectorStoreProvider.getIfAvailable()).willReturn(vectorStore);

        Sighting sighting = Sighting.create(
                20L,
                "https://cdn/sighting.jpg",
                LocalDateTime.of(2026, 2, 26, 10, 0),
                "Gangnam",
                "white poodle"
        );
        sighting.assignIdForTest(501L);

        lostPetAiClient.indexSighting(sighting);

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        Document indexed = captor.getValue().get(0);
        assertThat(indexed.getId()).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        assertThat(indexed.getMetadata()).containsEntry("sightingId", 501L);
        assertThat(indexed.getMetadata()).containsEntry("finderId", 20L);
    }

    private LostPetAnalyzeRequest sampleRequest() {
        return LostPetAnalyzeRequest.builder()
                .lostPetId(1L)
                .imageUrl("https://cdn/query.jpg")
                .mode("LOST")
                .queryText("white poodle with blue collar")
                .latitude(37.50)
                .longitude(127.03)
                .build();
    }
}
