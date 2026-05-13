package es.unican.istr.rama.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.compare.Comparison;

import es.unican.istr.rama.comparison.ComparisonService;
import es.unican.istr.rama.comparison.ModelComparisonInput;
import es.unican.istr.rama.config.RamaConfig;
import es.unican.istr.rama.git.GitService;
import es.unican.istr.rama.render.FileReport;
import es.unican.istr.rama.render.MunidiffRenderer;
import es.unican.istr.rama.render.RenderedMunidiff;
import es.unican.istr.rama.render.ReportComment;
import es.unican.istr.rama.render.ReportCommentRenderer;

public class RamaApplication {

    private final RamaConfig config;
    private final GitService gitService;
    private final ComparisonService modelComparator;
    private final MunidiffRenderer munidiffRenderer;
    private final ReportCommentRenderer reportCommentRenderer;

    public RamaApplication(
            RamaConfig config,
            GitService gitService,
            ComparisonService modelComparator,
            MunidiffRenderer munidiffRenderer,
            ReportCommentRenderer reportCommentRenderer
    ) {
        this.config = config;
        this.gitService = gitService;
        this.modelComparator = modelComparator;
        this.munidiffRenderer = munidiffRenderer;
        this.reportCommentRenderer = reportCommentRenderer;
    }

    /**
     * Main method to run the application for a given change request number. It retrieves the affected model files,
     * compares them, renders the differences, and publishes a comment with the results.
     *
     * @param pullRequestNumber
     * @throws IOException
     */
    public void run(int pullRequestNumber) throws IOException {
        List<ModelComparisonInput> files = gitService.getModelFiles(pullRequestNumber);
        List<FileReport> fileReports = new ArrayList<>();

        System.out.println("Affected model files:");
        for (ModelComparisonInput file : files) {
            Comparison comparison = modelComparator.compare(file);

            System.out.println("--------------------------------");
            System.out.println("Filename: " + file.filename());
            System.out.println("Source content length: " + contentLength(file.sourceContent()));
            System.out.println("Target content length: " + contentLength(file.targetContent()));
            System.out.println("Base content length: " + contentLength(file.baseContent()));
            System.out.println("Differences: " + comparison.getDifferences().size());

            RenderedMunidiff rendered = munidiffRenderer.render(comparison, config.isMetamodelFile(file.filename()));
            fileReports.add(new FileReport(file.filename(), rendered.plantuml()));
        }

        ReportComment comment = reportCommentRenderer.render(fileReports);
        gitService.publishComment(pullRequestNumber, comment);
    }

    /**
     * Helper method to safely get the length of content, handling null values.
     *
     * @param content the content whose length is to be determined
     * @return the length of the content, or "<missing>" if the content is null
     */
    private String contentLength(String content) {
        if (content == null) {
            return "<missing>";
        }
        return String.valueOf(content.length());
    }
}
