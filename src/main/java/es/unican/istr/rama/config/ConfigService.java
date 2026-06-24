package es.unican.istr.rama.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.*;

public class ConfigService {

    // Environment variable pointing to the GitHub Actions workspace, where the target repository is checked out.
    private static final String GITHUB_WORKSPACE_ENV = "GITHUB_WORKSPACE";

    // Name of the RAMA configuration file expected in the target repository and packaged with RAMA.
    private static final String CONFIG_FILENAME = "rama.json";

    // RAMA's configuration is read-only and can be safely shared across threads.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Path configuredWorkspace;

    public ConfigService() {
        this(null);
    }

    ConfigService(Path configuredWorkspace) {
        this.configuredWorkspace = configuredWorkspace;
    }

    /**
     * Loads configuration and retains a user-facing warning when RAMA has to use the packaged
     * default. A missing, empty, unreadable, or malformed repository configuration always uses
     * that default instead of aborting the analysis.
     *
     * @return the selected configuration and an optional warning for the report
     * @throws IOException if the packaged default configuration cannot be read
     */
    public ConfigurationLoadResult loadConfig() throws IOException {
        Path workspace = configurationWorkspacePath();

        if (workspace != null) {
            // Prefer the target repository config checked out by actions/checkout.
            Path targetRepositoryConfig = workspace.resolve(CONFIG_FILENAME);

            if (Files.exists(targetRepositoryConfig)) {
                try {
                    String content = Files.readString(targetRepositoryConfig);

                    if (content.isBlank()) {
                        return useDefaultConfiguration(
                                "`rama.json` is empty. RAMA is using the packaged default configuration."
                        );
                    }

                    System.out.println("Using RAMA config from target repository.");
                    return new ConfigurationLoadResult(
                            OBJECT_MAPPER.readValue(content, RamaConfig.class),
                            null
                    );
                }
                catch (IOException | RuntimeException ex) {
                    System.err.println("Could not load rama.json. Using default RAMA configuration.");
                    return useDefaultConfiguration(
                            "`rama.json` is invalid or unreadable. RAMA is using the packaged default configuration."
                    );
                }
            }
        }

        return useDefaultConfiguration(
                "`rama.json` was not found. RAMA is using the packaged default configuration."
        );
    }

    private ConfigurationLoadResult useDefaultConfiguration(String warning) throws IOException {
        System.out.println("Using default RAMA config.");

        try (InputStream defaultConfig = ConfigService.class.getClassLoader().getResourceAsStream(CONFIG_FILENAME)) {
            if (defaultConfig == null) {
                throw new IllegalStateException(
                        "Default RAMA config not found in application resources: " + CONFIG_FILENAME
                );
            }
            return new ConfigurationLoadResult(
                    OBJECT_MAPPER.readValue(defaultConfig, RamaConfig.class),
                    warning
            );
        }
    }

    /**
     * Resolves the workspace where repository-relative configuration paths should be interpreted.
     *
     * @return the configured workspace path, or the current working directory when no workspace was configured
     */
    public Path workspacePath() {
        if (configuredWorkspace != null) {
            return configuredWorkspace;
        }

        Path workspace = workspacePathFromEnvironment();
        return workspace == null ? Path.of("") : workspace;
    }

    private Path configurationWorkspacePath() {
        if (configuredWorkspace != null) {
            return configuredWorkspace;
        }

        return workspacePathFromEnvironment();
    }

    private Path workspacePathFromEnvironment() {
        String workspace = System.getenv(GITHUB_WORKSPACE_ENV);

        if (workspace == null || workspace.isBlank()) {
            return null;
        }

        return Path.of(workspace);
    }

}
