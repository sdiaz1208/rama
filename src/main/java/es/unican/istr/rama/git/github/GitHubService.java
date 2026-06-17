package es.unican.istr.rama.git.github;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import es.unican.istr.rama.comparison.ModelComparisonInput;
import es.unican.istr.rama.config.RamaConfig;
import es.unican.istr.rama.git.GitService;
import es.unican.istr.rama.render.ReportComment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GitHubService implements GitService {

    // Target repository to analyze, selected by GITHUB_REPOSITORY
    private final GHRepository repository;

    // RAMA configuration used to select model and metamodel files from pull requests.
    private final RamaConfig config;

    // Values provided by GitHub Actions for authenticating and locating the repository under analysis.
    private static final String GITHUB_TOKEN_ENV = "GITHUB_TOKEN";
    private static final String GITHUB_REPOSITORY_ENV = "GITHUB_REPOSITORY";

    /**
     * Private constructor to enforce the use of the factory method for instantiation.
     *
     * @param repository the GHRepository instance representing the target repository to analyze
     */
    private GitHubService(
            GHRepository repository,
            RamaConfig config
    ) {
        this.repository = repository;
        this.config = config;
    }

    /**
     * Factory method to create a GitHubService instance using environment variables provided by GitHub Actions.
     *
     * @param config the loaded RAMA configuration used to select relevant model/metamodel files
     * @return a GitHubService instance configured with the repository specified in the environment variables
     * @throws IOException if there is an error communicating with the GitHub API or reading the configuration
     */
    public static GitHubService fromEnvironment(RamaConfig config) throws IOException {
        String token = System.getenv(GITHUB_TOKEN_ENV);
        String repoName = System.getenv(GITHUB_REPOSITORY_ENV);

        GitHub github = new GitHubBuilder().withOAuthToken(token).build();
        return new GitHubService(github.getRepository(repoName), config);
    }

    @Override
    public List<ModelComparisonInput> getModelFiles(int pullRequestNumber) throws IOException {
        // Fetch the pull request and its source and target branches.
        GHPullRequest pullRequest = repository.getPullRequest(pullRequestNumber);
        GHCommitPointer sourceBranch = pullRequest.getHead();
        GHCommitPointer targetBranch = pullRequest.getBase();
        String baseCommitSha = findMergeBaseCommitSha(sourceBranch, targetBranch);

        List<ModelComparisonInput> modelFiles = new ArrayList<>();

        for (GHPullRequestFileDetail file : pullRequest.listFiles()) {
            if (isRelevantFile(file, config)) {
                modelFiles.add(toModelComparisonInput(file, sourceBranch, targetBranch, baseCommitSha));
            }
        }

        return modelFiles;
    }

    @Override
    public void publishComment(int pullRequestNumber, ReportComment comment) throws IOException {
        GHIssue issue = repository.getIssue(pullRequestNumber);
        issue.comment(comment.body());
    }

    /**
     * Finds the merge base commit SHA for the source and target branches of a pull request.
     *
     * @param sourceBranch the source branch of the pull request
     * @param targetBranch the target branch of the pull request
     * @return the SHA of the merge base commit
     * @throws IOException if there is an error communicating with the GitHub API
     */
    private String findMergeBaseCommitSha(
        GHCommitPointer sourceBranch,
        GHCommitPointer targetBranch
    ) throws IOException {
        GHRepository targetRepository = targetBranch.getRepository();
        GHCommit targetCommit = targetRepository.getCommit(targetBranch.getSha());
        GHCommit sourceCommit = sourceBranch.getRepository().getCommit(sourceBranch.getSha());

        return targetRepository.getCompare(targetCommit, sourceCommit)
                .getMergeBaseCommit()
                .getSHA1();
    }

    /**
     * Determines if a given pull request file is relevant for RAMA analysis based on its filename and
     * the RAMA configuration.
     *
     * @param file the GHPullRequestFileDetail representing the file changed in the pull request
     * @param config the RAMA configuration containing the relevant file extensions
     * @return true if the file is relevant for RAMA analysis, false otherwise
     */
    private boolean isRelevantFile(GHPullRequestFileDetail file, RamaConfig config) {
        return config.isRelevantFile(file.getFilename())
                || (file.getPreviousFilename() != null && config.isRelevantFile(file.getPreviousFilename()));
    }

    /**
     * Converts a pull request file detail into a ModelComparisonInput by fetching the contents of the file
     * from the source branch, target branch, and merge base commit.
     *
     * @param file the GHPullRequestFileDetail representing the file changed in the pull request
     * @param sourceBranch the source branch of the pull request
     * @param targetBranch the target branch of the pull request
     * @param baseCommitSha the SHA of the merge base commit
     * @return a ModelComparisonInput containing the contents of the file from the source, target, and base commits
     * @throws IOException if there is an error communicating with the GitHub API
     */
    private ModelComparisonInput toModelComparisonInput(
            GHPullRequestFileDetail file,
            GHCommitPointer sourceBranch,
            GHCommitPointer targetBranch,
            String baseCommitSha
    ) throws IOException {
        String sourcePath = file.getFilename();
        String targetPath = file.getPreviousFilename() == null ? file.getFilename() : file.getPreviousFilename();

        String sourceContent = fetchFileContent(sourceBranch.getRepository(), sourcePath, sourceBranch.getSha());
        String targetContent = fetchFileContent(targetBranch.getRepository(), targetPath, targetBranch.getSha());
        String baseContent = fetchFileContent(targetBranch.getRepository(), targetPath, baseCommitSha);

        return new ModelComparisonInput(
                file.getFilename(),
                file.getPreviousFilename(),
                sourceContent,
                targetContent,
                baseContent
        );
    }

    /**
     * Fetches the content of a file from a specific commit in the repository.
     * If the file does not exist in that commit, returns null.
     *
     * @param repository the GHRepository object representing the repository
     * @param repositoryPath the path of the file in the repository
     * @param commitSha the SHA of the commit from which to fetch the file
     * @return the content of the file as a String, or null if the file does not exist in the specified commit
     * @throws IOException if there is an error communicating with the GitHub API
     */
    private String fetchFileContent(
            GHRepository repository,
            String repositoryPath,
            String commitSha
    ) throws IOException {
        try {
            GHContent content = repository.getFileContent(repositoryPath, commitSha);
            try (InputStream contentStream = content.read()) {
                return new String(contentStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (GHFileNotFoundException e) {
            return null;
        }
    }

}
