package scit.ainiinu.common.contract;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import scit.ainiinu.common.config.OpenApiEndpointContractRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiErrorMatrixSyncContractTest {

    private static final Path ERROR_MATRIX_PATH = Path.of("docs/openapi/ERROR_CODE_MATRIX.md");
    private static final String MATRIX_START = "<!-- MATRIX:START -->";
    private static final String MATRIX_END = "<!-- MATRIX:END -->";

    private static final Pattern ROW_PATTERN = Pattern.compile(
            "^\\|\\s*([A-Z]+)\\s*\\|\\s*`([^`]+)`\\s*\\|\\s*(\\d{3})\\s*\\|\\s*`([^`]+)`\\s*\\|\\s*(.*?)\\s*\\|\\s*(.*?)\\s*\\|\\s*$"
    );

    @Test
    @DisplayName("에러 매트릭스 문서는 레지스트리 행과 1:1로 동기화되어야 한다")
    void errorMatrixMustMatchRegistryRows() throws IOException {
        assertThat(Files.exists(ERROR_MATRIX_PATH))
                .as("error matrix file missing: %s", ERROR_MATRIX_PATH)
                .isTrue();

        String markdown = Files.readString(ERROR_MATRIX_PATH);
        String matrixBody = extractMatrixBody(markdown);

        Set<MatrixRow> actualRows = parseRows(matrixBody);
        Set<MatrixRow> expectedRows = new LinkedHashSet<>();

        for (OpenApiEndpointContractRegistry.DomainErrorRow row : OpenApiEndpointContractRegistry.domainErrorRows()) {
            expectedRows.add(new MatrixRow(
                    row.method(),
                    row.path(),
                    row.status(),
                    row.code(),
                    row.condition(),
                    row.exampleMessage()
            ));
        }

        assertThat(actualRows)
                .as("matrix row count mismatch")
                .hasSize(expectedRows.size());
        assertThat(actualRows)
                .as("matrix rows must match registry rows exactly")
                .containsExactlyInAnyOrderElementsOf(expectedRows);
    }

    private String extractMatrixBody(String markdown) {
        int start = markdown.indexOf(MATRIX_START);
        int end = markdown.indexOf(MATRIX_END);

        assertThat(start)
                .as("matrix start marker missing")
                .isGreaterThanOrEqualTo(0);
        assertThat(end)
                .as("matrix end marker missing")
                .isGreaterThan(start);

        return markdown.substring(start + MATRIX_START.length(), end);
    }

    private Set<MatrixRow> parseRows(String matrixBody) {
        Set<MatrixRow> rows = new LinkedHashSet<>();
        List<String> invalidLines = new ArrayList<>();

        String[] lines = matrixBody.split("\\R");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (!line.startsWith("|")) {
                continue;
            }
            if (line.startsWith("| Method ") || line.startsWith("|---")) {
                continue;
            }

            Matcher matcher = ROW_PATTERN.matcher(line);
            if (!matcher.matches()) {
                invalidLines.add(line);
                continue;
            }

            rows.add(new MatrixRow(
                    matcher.group(1),
                    matcher.group(2),
                    Integer.parseInt(matcher.group(3)),
                    matcher.group(4),
                    matcher.group(5),
                    matcher.group(6)
            ));
        }

        assertThat(invalidLines)
                .as("invalid matrix row format")
                .isEmpty();
        return rows;
    }

    private record MatrixRow(
            String method,
            String path,
            int status,
            String code,
            String condition,
            String exampleMessage
    ) {
    }
}
