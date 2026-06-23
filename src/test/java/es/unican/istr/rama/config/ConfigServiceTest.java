package es.unican.istr.rama.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigServiceTest {

    @TempDir
    Path workspace;

    @Test
    void missingConfigurationUsesDefaultAndReturnsWarning() throws Exception {
        ConfigurationLoadResult result = new ConfigService(workspace).loadConfig();

        assertDefaultConfiguration(result);
        assertTrue(result.warning().contains("was not found"));
    }

    @Test
    void emptyConfigurationUsesDefaultAndReturnsWarning() throws Exception {
        Files.writeString(workspace.resolve("rama.json"), " \n\t ");

        ConfigurationLoadResult result = new ConfigService(workspace).loadConfig();

        assertDefaultConfiguration(result);
        assertTrue(result.warning().contains("is empty"));
    }

    @Test
    void malformedConfigurationUsesDefaultAndReturnsWarning() throws Exception {
        Files.writeString(workspace.resolve("rama.json"), "{ not valid json }");

        ConfigurationLoadResult result = new ConfigService(workspace).loadConfig();

        assertDefaultConfiguration(result);
        assertTrue(result.warning().contains("is invalid or unreadable"));
    }

    @Test
    void validConfigurationIsUsedWithoutWarning() throws Exception {
        Files.writeString(workspace.resolve("rama.json"), """
                {
                  "model_extensions": [".xmi"],
                  "metamodel_extensions": [".meta"],
                  "metamodels": []
                }
                """);

        ConfigurationLoadResult result = new ConfigService(workspace).loadConfig();

        assertTrue(result.config().isRelevantFile("models/example.xmi"));
        assertTrue(result.config().isMetamodelFile("metamodels/example.meta"));
        assertFalse(result.config().isRelevantFile("models/example.model"));
        assertNull(result.warning());
    }

    private void assertDefaultConfiguration(ConfigurationLoadResult result) {
        assertNotNull(result.warning());
        assertTrue(result.config().isRelevantFile("models/example.model"));
        assertTrue(result.config().isMetamodelFile("metamodels/example.ecore"));
    }
}
