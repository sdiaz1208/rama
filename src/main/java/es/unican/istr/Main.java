package es.unican.istr;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.compare.Comparison;

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
        SimpleEMFCompare emfCompare = new SimpleEMFCompare(config);
        MunidiffRenderer munidiffRenderer = new MunidiffRenderer();
        List<GitHubService.RenderedSvgReport> renderedSvgReports = new ArrayList<>();

        // Print the affected model files in the pull request and their EMF comparison result.
        System.out.println("Affected model files:");
        for (ModelComparisonInput file : files) {
            Comparison comparison = emfCompare.compare(file);

            System.out.println("--------------------------------");
            System.out.println("Filename: " + file.filename());
            System.out.println("Source content length: " + contentLength(file.sourceContent()));
            System.out.println("Target content length: " + contentLength(file.targetContent()));
            System.out.println("Base content length: " + contentLength(file.baseContent()));
            System.out.println("Differences: " + comparison.getDifferences().size());

            boolean ecoreDiff = config.metamodelExtensions().stream()
                    .anyMatch(extension -> file.filename().endsWith(extension));
            MunidiffRenderer.RenderedMunidiff rendered = munidiffRenderer.render(comparison, ecoreDiff);

            renderedSvgReports.add(new GitHubService.RenderedSvgReport(file.filename(), rendered.svg()));
        }

        gitHubService.postRenderedSvgReport(prNumber, renderedSvgReports);
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
