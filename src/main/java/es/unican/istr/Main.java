package es.unican.istr;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        // Parse the pull request number from the command line arguments.
        // In a real GitHub Action, this would come from the environment or action inputs.
        int prNumber = Integer.parseInt(args[0]);

        // Load RAMA configuration once at application startup.
        ConfigService.RamaConfig config = new ConfigService().loadConfig();

        // Create a GitHubService instance using environment variables provided by GitHub Actions.
        GitHubService gitHubService = GitHubService.fromEnvironment(config);
        List<ModelComparisonInput> files = gitHubService.getModelFiles(prNumber);

        // Print the affected model files in the pull request.
        System.out.println("Affected model files:");
        for (ModelComparisonInput file : files) {
            System.out.println("--------------------------------");
            System.out.println("Filename: " + file.filename());
            System.out.println("Source content length: " + contentLength(file.sourceContent()));
            System.out.println("Target content length: " + contentLength(file.targetContent()));
            System.out.println("Base content length: " + contentLength(file.baseContent()));
        }
    }

    /**
     * Helper method to safely get the length of content, handling null values.
     * 
     * @param content the content whose length is to be determined
     * @return the length of the content, or "<missing>" if the content is null
     */
    private static String contentLength(String content) {
        if (content == null) {
            return "<missing>";
        }
        return String.valueOf(content.length());
    }
}
