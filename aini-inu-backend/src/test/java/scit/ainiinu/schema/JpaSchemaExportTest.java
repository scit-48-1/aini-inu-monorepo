package scit.ainiinu.schema;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import scit.ainiinu.testsupport.IntegrationTestProfile;

@DataJpaTest
@IntegrationTestProfile
class JpaSchemaExportTest {

    @Test
    void exportSchemaScript() {
        // Context initialization is enough. The Gradle task injects the export properties.
    }
}
