package es.unican.istr.rama;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.compare.Comparison;

import es.unican.istr.rama.comparison.ModelComparisonInput;
import es.unican.istr.rama.comparison.SimpleEMFCompare;
import es.unican.istr.rama.config.ConfigService;
import es.unican.istr.rama.config.GitHubService;
import es.unican.istr.rama.render.MunidiffRenderer;

public class Main {
    public static void main(String[] args) throws Exception {
        // The GitHub Action passes the pull request number as the first argument.
        // RAMA uses that PR number to fetch changed files and publish the final report.
        int prNumber = Integer.parseInt(args[0]);

        // Load the RAMA configuration before creating services so file selection,
        // metamodel handling, and comparison behavior share the same settings.
        ConfigService.RamaConfig config = new ConfigService().loadConfig();

        // Reuse one GitHub service for every GitHub API operation in this run:
        // reading PR files, fetching file contents, and posting the PR comment.
        GitHubService gitHubService = GitHubService.fromEnvironment(config);
        List<ModelComparisonInput> files = gitHubService.getModelFiles(prNumber);

        // Prepare the local analysis and rendering services. GitHubService has already
        // converted remote PR files into ModelComparisonInput objects for comparison.
        SimpleEMFCompare emfCompare = new SimpleEMFCompare(config);
        MunidiffRenderer munidiffRenderer = new MunidiffRenderer();
        List<GitHubService.PlantUmlReport> plantUmlReports = new ArrayList<>();

        // Analyze each relevant model file and keep lightweight console output for
        // GitHub Actions logs. The PlantUML report is saved for the PR comment.
        System.out.println("Affected model files:");
        for (ModelComparisonInput file : files) {
            // EMF Compare computes the semantic/model-level differences between
            // the source, target, and merge-base versions fetched from GitHub.
            Comparison comparison = emfCompare.compare(file);

            System.out.println("--------------------------------");
            System.out.println("Filename: " + file.filename());
            System.out.println("Source content length: " + contentLength(file.sourceContent()));
            System.out.println("Target content length: " + contentLength(file.targetContent()));
            System.out.println("Base content length: " + contentLength(file.baseContent()));
            System.out.println("Differences: " + comparison.getDifferences().size());

            // Munidiff needs a different matcher/formatter for Ecore metamodels.
            // The configured metamodel extensions decide which rendering path to use.
            boolean ecoreDiff = config.metamodelExtensions().stream()
                    .anyMatch(extension -> file.filename().endsWith(extension));
            MunidiffRenderer.RenderedMunidiff rendered = munidiffRenderer.render(comparison, ecoreDiff);

            // Store the PlantUML source per file so GitHubService can encode it
            // as a renderable SVG image in one PR comment update.
            plantUmlReports.add(new GitHubService.PlantUmlReport(file.filename(), rendered.plantuml()));
        }

        // Publish the full analysis result back to the pull request. Existing RAMA
        // comments are updated in place so repeated workflow runs do not duplicate them.
        gitHubService.postRenderedSvgReport(prNumber, plantUmlReports);
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
