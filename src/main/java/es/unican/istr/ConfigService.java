package es.unican.istr;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigService {

    // Environment variable pointing to the GitHub Actions workspace, where the target repository is checked out.
    private static final String GITHUB_WORKSPACE_ENV = "GITHUB_WORKSPACE";

    // Name of the RAMA configuration file expected in the target repository and packaged with RAMA.
    private static final String CONFIG_FILENAME = "rama.json";

    // RAMA's configuration is read-only and can be safely shared across threads.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Loads the RAMA configuration from the target repository if available, or falls back
     * to the default configuration packaged with RAMA.
     *
     * @return a RamaConfig object containing the model and metamodel file extensions and metamodels
     * @throws IOException if there is an error reading the configuration file
     */
    public RamaConfig loadConfig() throws IOException {
        String workspace = System.getenv(GITHUB_WORKSPACE_ENV);

        if (workspace != null && !workspace.isBlank()) {
            // Prefer the target repository config checked out by actions/checkout.
            Path targetRepositoryConfig = Path.of(workspace).resolve(CONFIG_FILENAME);

            if (Files.exists(targetRepositoryConfig)) {
                System.out.println("Using RAMA config from target repository.");
                return OBJECT_MAPPER.readValue(targetRepositoryConfig.toFile(), RamaConfig.class);
            }
        }

        // Fall back to RAMA's packaged default config when the target repository has none.
        try (InputStream defaultConfig = ConfigService.class.getClassLoader().getResourceAsStream(CONFIG_FILENAME)) {
            if (defaultConfig == null) {
                throw new IllegalStateException("Default RAMA config not found in application resources: " + CONFIG_FILENAME);
            }
            System.out.println("Using default RAMA config.");
            return OBJECT_MAPPER.readValue(defaultConfig, RamaConfig.class);
        }
    }

    // JSON shape of rama.json.
    public record RamaConfig(
            @JsonProperty("model_extensions") List<String> modelExtensions,
            @JsonProperty("metamodel_extensions") List<String> metamodelExtensions,
            @JsonProperty("metamodels") List<String> metamodels
    ) {
        public boolean isModelFile(String filename) {
            return modelFileExtensions().stream().anyMatch(filename::endsWith);
        }

        // Both model and metamodel files are relevant PR files for RAMA analysis.
        private List<String> modelFileExtensions() {
            List<String> extensions = new ArrayList<>();
            extensions.addAll(modelExtensions == null ? List.of() : modelExtensions);
            extensions.addAll(metamodelExtensions == null ? List.of() : metamodelExtensions);
            return extensions;
        }
    }

}
