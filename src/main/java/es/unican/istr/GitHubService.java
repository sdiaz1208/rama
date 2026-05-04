package es.unican.istr;

import org.kohsuke.github.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GitHubService {

    // Target repository to analyze, selected by GITHUB_REPOSITORY
    private final GHRepository repository;

    // RAMA configuration used to select model and metamodel files from pull requests.
    private final ConfigService.RamaConfig config;

    // Values provided by GitHub Actions for authenticating and locating the repository under analysis.
    private static final String GITHUB_TOKEN_ENV = "GITHUB_TOKEN";
    private static final String GITHUB_REPOSITORY_ENV = "GITHUB_REPOSITORY";

    /**
     * Private constructor to enforce the use of the factory method for instantiation.
     * 
     * @param repository the GHRepository instance representing the target repository to analyze
     */
    private GitHubService(GHRepository repository, ConfigService.RamaConfig config) {
        this.repository = repository;
        this.config = config;
    }

    /**
     * Factory method to create a GitHubService instance using environment variables provided by GitHub Actions.
     * 
     * @param config the loaded RAMA configuration used to select relevant model files
     * @return a GitHubService instance configured with the repository specified in the environment variables
     * @throws IOException if there is an error communicating with the GitHub API or reading the configuration
     */
    public static GitHubService fromEnvironment(ConfigService.RamaConfig config) throws IOException {
        String token = System.getenv(GITHUB_TOKEN_ENV);
        String repoName = System.getenv(GITHUB_REPOSITORY_ENV);

        GitHub github = new GitHubBuilder().withOAuthToken(token).build();
        return new GitHubService(github.getRepository(repoName), config);
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
        ConfigService.RamaConfig config = configService.loadConfig();
        List<GHPullRequestFileDetail> modelFiles = new ArrayList<>();

        for (GHPullRequestFileDetail file : repository.getPullRequest(prNumber).listFiles()) {
            if (config.isModelFile(file.getFilename())) {
                modelFiles.add(file);
            }
        }

        return modelFiles;
    }

}
