package es.unican.istr;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kohsuke.github.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GitHubService {

    // Target repository to analyze, selected by GITHUB_REPOSITORY
    private final GHRepository repository;

    // Values provided by GitHub Actions for authenticating and locating the repository under analysis.
    private static final String GITHUB_TOKEN_ENV = "GITHUB_TOKEN";
    private static final String GITHUB_REPOSITORY_ENV = "GITHUB_REPOSITORY";
    private static final String GITHUB_WORKSPACE_ENV = "GITHUB_WORKSPACE";

    // The name of the RAMA configuration file to look for in the target repository and as a fallback in RAMA's resources.
    private static final String CONFIG_FILENAME = "rama.json";

    // RAMA's configuration is read-only and can be safely shared across threads,so we use a single static ObjectMapper instance.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Private constructor to enforce the use of the factory method for instantiation.
     * 
     * @param repository the GHRepository instance representing the target repository to analyze
     */
    private GitHubService(GHRepository repository) {
        this.repository = repository;
    }

    /**
     * Factory method to create a GitHubService instance using environment variables provided by GitHub Actions.
     * 
     * @return a GitHubService instance configured with the repository specified in the environment variables
     * @throws IOException if there is an error communicating with the GitHub API or reading the configuration
     */
    public static GitHubService fromEnvironment() throws IOException {
        String token = System.getenv(GITHUB_TOKEN_ENV);
        String repoName = System.getenv(GITHUB_REPOSITORY_ENV);

        GitHub github = new GitHubBuilder().withOAuthToken(token).build();
        return new GitHubService(github.getRepository(repoName));
    }

    /**
     * Fetches the list of files affected in the specified pull request and filters them to include only
     * those with extensions defined as model or metamodel extensions in the RAMA configuration.
     * 
     * @param prNumber the number of the pull request to analyze
     * @return a list of GHPullRequestFileDetail objects representing the model files in a pull request
     * @throws IOException if there is an error communicating with the GitHub API 
     *         or reading the configuration
     */
    public List<GHPullRequestFileDetail> getModelFiles(int prNumber) throws IOException {
        RamaConfig config = loadConfig();
        List<GHPullRequestFileDetail> modelFiles = new ArrayList<>();

        for (GHPullRequestFileDetail file : repository.getPullRequest(prNumber).listFiles()) {
            if (isModelFile(file.getFilename(), config)) {
                modelFiles.add(file);
            }
        }

        return modelFiles;
    }

    /**
     * Loads the RAMA configuration from the target repository if available, or falls back
     * to the default configuration packaged with RAMA.
     * 
     * @return a RamaConfig object containing the model and metamodel file extensions and metamodels
     * @throws IOException if there is an error reading the configuration file
     */
    private static RamaConfig loadConfig() throws IOException {
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
        try (InputStream defaultConfig = GitHubService.class.getClassLoader().getResourceAsStream(CONFIG_FILENAME)) {
            if (defaultConfig == null) {
                throw new IllegalStateException("Default RAMA config not found in application resources: " + CONFIG_FILENAME);
            }
            System.out.println("Using default RAMA config.");
            return OBJECT_MAPPER.readValue(defaultConfig, RamaConfig.class);
        }
    }

    /**
     * Determines if a given filename corresponds to a model file based on the extensions defined in the RAMA configuration.
     * 
     * @param filename the name of the file to check
     * @param config the RAMA configuration containing the model and metamodel file extensions
     * @return true if the filename corresponds to a model file, false otherwise
     */
    private static boolean isModelFile(String filename, RamaConfig config) {
        return config.modelFileExtensions().stream().anyMatch(filename::endsWith);
    }

    // JSON shape of rama.json.
    private record RamaConfig(
            @JsonProperty("model_extensions") List<String> modelExtensions,
            @JsonProperty("metamodel_extensions") List<String> metamodelExtensions,
            @JsonProperty("metamodels") List<String> metamodels
    ) {
        // Both model and metamodel files are relevant PR files for RAMA analysis.
        private List<String> modelFileExtensions() {
            List<String> extensions = new ArrayList<>();
            extensions.addAll(modelExtensions == null ? List.of() : modelExtensions);
            extensions.addAll(metamodelExtensions == null ? List.of() : metamodelExtensions);
            return extensions;
        }
    }

}
