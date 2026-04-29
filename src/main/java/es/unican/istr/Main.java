package es.unican.istr;

import org.kohsuke.github.GHPullRequestFileDetail;

import java.util.List;

public class Main {
	public static void main(String[] args) throws Exception {
        // Parse the pull request number from the command line arguments.
        // In a real GitHub Action, this would come from the environment or action inputs.
        int prNumber = Integer.parseInt(args[0]);

        // Create a GitHubService instance using environment variables provided by GitHub Actions.
        GitHubService gitHubService = GitHubService.fromEnvironment();
        List<GHPullRequestFileDetail> files = gitHubService.getModelFiles(prNumber);

        // Print the affected model files in the pull request.
        System.out.println("Affected files:");
        for (GHPullRequestFileDetail file : files) {
            System.out.println("--------------------------------");
            System.out.println("Path: " + file.getFilename());
        }
	}
}
