package es.unican.istr;

import org.kohsuke.github.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
    private static final String COMMENT_MARKER = "<!-- RAMA:SVG-REPORT -->";

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
     * Fetches the model/metamodel files affected in the specified pull request and returns the
     * contents of the source, target, and merge-base versions.
     * 
     * @param prNumber the number of the pull request to analyze
     * @return a list of model comparison inputs representing the model files in a pull request
     * @throws IOException if there is an error communicating with the GitHub API
     */
    public List<ModelComparisonInput> getModelFiles(int prNumber) throws IOException {
        // Fetch the pull request and its source and target branches.
        GHPullRequest pullRequest = repository.getPullRequest(prNumber);
        GHCommitPointer sourceBranch = pullRequest.getHead();
        GHCommitPointer targetBranch = pullRequest.getBase();
        String baseCommitSha = findMergeBaseCommitSha(sourceBranch, targetBranch);

        List<ModelComparisonInput> modelFiles = new ArrayList<>();

        for (GHPullRequestFileDetail file : pullRequest.listFiles()) {
            if (isRelevantModelFile(file, config)) {
                modelFiles.add(toModelComparisonInput(file, sourceBranch, targetBranch, baseCommitSha));
            }
        }

        return modelFiles;
    }

    /**
     * Posts or updates a pull request comment containing the rendered SVG report.
     *
     * @param prNumber the pull request number to comment on
     * @param reports the rendered SVG reports to publish
     * @throws IOException if there is an error communicating with the GitHub API
     */
    public void postRenderedSvgReport(int prNumber, List<RenderedSvgReport> reports) throws IOException {
        String body = buildCommentBody(reports);
        GHIssue issue = repository.getIssue(prNumber);

        for (GHIssueComment comment : issue.listComments()) {
            if (comment.getBody() != null && comment.getBody().contains(COMMENT_MARKER)) {
                comment.update(body);
                return;
            }
        }

        issue.comment(body);
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
     * @param config the RAMA configuration containing the model file extensions
     * @return true if the file is relevant for RAMA analysis, false otherwise
     */
    private boolean isRelevantModelFile(GHPullRequestFileDetail file, ConfigService.RamaConfig config) {
        return config.isModelFile(file.getFilename())
                || (file.getPreviousFilename() != null && config.isModelFile(file.getPreviousFilename()));
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

        return new ModelComparisonInput(file.getFilename(), sourceContent, targetContent, baseContent);
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

    /**
     * Builds the body of a GitHub comment containing the rendered SVG reports.
     * 
     * @param reports the list of rendered SVG reports
     * @return the body of the GitHub comment as a String
     */
    private String buildCommentBody(List<RenderedSvgReport> reports) {
        StringBuilder body = new StringBuilder();
        body.append(COMMENT_MARKER).append("\n");
        body.append("## RAMA Analysis Report").append("\n\n");

        if (reports.isEmpty()) {
            body.append("No model files were analyzed.");
            return body.toString();
        }

        for (RenderedSvgReport report : reports) {
            body.append("### <code>").append(escapeHtml(report.filename())).append("</code>\n\n");
            body.append("<details>\n");
            body.append("<summary>Rendered SVG</summary>\n\n");
            body.append("```svg\n");
            body.append(report.svg()).append("\n");
            body.append("```\n\n");
            body.append("</details>\n\n");
        }

        return body.toString();
    }

    /**
     * Escapes special HTML characters in a string to prevent rendering issues in GitHub comments.
     * 
     * @param text the text to escape
     * @return the escaped text
     */
    private String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * A record representing a rendered SVG report for a model file.
     * 
     * @param filename the name of the model file
     * @param svg the rendered SVG content
     */
    public record RenderedSvgReport(String filename, String svg) {
    }

}
