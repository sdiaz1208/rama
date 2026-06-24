package es.unican.istr.rama.app;

import java.io.IOException;
import java.util.*;

import org.eclipse.emf.compare.*;

import es.unican.istr.rama.comparison.*;
import es.unican.istr.rama.config.RamaConfig;
import es.unican.istr.rama.git.GitService;
import es.unican.istr.rama.render.*;

public class RamaApplication {

    private final RamaConfig config;
    private final GitService gitService;
    private final ComparisonService modelComparator;
    private final MunidiffRenderer munidiffRenderer;
    private final ReportCommentRenderer reportCommentRenderer;
    private final String configurationWarning;

    public RamaApplication(
            RamaConfig config,
            GitService gitService,
            ComparisonService modelComparator,
            MunidiffRenderer munidiffRenderer,
            ReportCommentRenderer reportCommentRenderer
    ) {
        this(
                config,
                gitService,
                modelComparator,
                munidiffRenderer,
                reportCommentRenderer,
                null
        );
    }

    public RamaApplication(
            RamaConfig config,
            GitService gitService,
            ComparisonService modelComparator,
            MunidiffRenderer munidiffRenderer,
            ReportCommentRenderer reportCommentRenderer,
            String configurationWarning
    ) {
        this.config = config;
        this.gitService = gitService;
        this.modelComparator = modelComparator;
        this.munidiffRenderer = munidiffRenderer;
        this.reportCommentRenderer = reportCommentRenderer;
        this.configurationWarning = configurationWarning;
    }

    /**
     * Main method to run the application for a given change request number. It retrieves the affected model files,
     * compares them, renders the differences, and publishes a comment with the results.
     *
     * @param pullRequestNumber
     * @throws IOException
     */
    public void run(int pullRequestNumber) throws IOException {
        List<ModelComparisonInput> files;
        try {
            files = gitService.getModelFiles(pullRequestNumber);
        }
        catch (Exception ex) {
            System.err.println("Could not fetch affected model files.");
            ex.printStackTrace(System.err);

            ReportComment comment = reportCommentRenderer.renderFailure(
                    diagnostic("Could not fetch affected model files.", ex),
                    configurationWarning
            );
            gitService.publishComment(pullRequestNumber, comment);
            return;
        }

        List<FileReport> fileReports = new ArrayList<>();

        System.out.println("Affected model files:");
        for (ModelComparisonInput file : files) {
            System.out.println("--------------------------------");
            System.out.println("Filename: " + file.filename());
            System.out.println("Source content length: " + contentLength(file.sourceContent()));
            System.out.println("Target content length: " + contentLength(file.targetContent()));
            System.out.println("Base content length: " + contentLength(file.baseContent()));

            try {
                if (isDeletedWithoutRename(file)) {
                    fileReports.add(FileReport.message(
                            file.filename(),
                            "This file was deleted."
                    ));
                    continue;
                }

                Comparison comparison = modelComparator.compare(file);

                System.out.println("Differences: " + comparison.getDifferences().size());
                System.out.println("Conflicts: " + comparison.getConflicts().size());

                if (comparison.getConflicts().isEmpty()) {
                    if (comparison.getDifferences().isEmpty()) {
                        fileReports.add(FileReport.message(
                                file.filename(),
                                "No model-level changes were detected in this file."
                        ));
                    }
                    else {
                        RenderedMunidiff rendered = render(comparison, file);
                        fileReports.add(new FileReport(
                                file.filename(),
                                rendered.plantuml(),
                                rendered.unifiedDiff(),
                                config.isMetamodelFile(file.filename())
                        ));
                    }
                }
                else {
                    fileReports.add(FileReport.conflict(file.filename(), renderConflictReport(comparison, file)));
                }
            }
            catch (Exception ex) {
                System.err.println("Could not analyze model file: " + file.filename());
                ex.printStackTrace(System.err);

                fileReports.add(FileReport.failure(
                        file.filename(),
                        diagnostic("Could not analyze this file.", ex)
                ));
            }
        }

        ReportComment comment;
        try {
            comment = reportCommentRenderer.render(fileReports, configurationWarning);
        }
        catch (Exception ex) {
            System.err.println("Could not render RAMA report comment.");
            ex.printStackTrace(System.err);

            comment = reportCommentRenderer.renderFailure(
                    diagnostic("Could not render the RAMA report comment.", ex),
                    configurationWarning
            );
        }

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

    private ConflictReport renderConflictReport(Comparison comparison, ModelComparisonInput file) throws IOException {
        RenderedMunidiff leftChanges = renderChangesAgainstBase(file, file.sourceContent());
        RenderedMunidiff rightChanges = renderChangesAgainstBase(file, file.targetContent());

        return new ConflictReport(
                comparison.getConflicts().size(),
                countConflicts(comparison, ConflictKind.REAL),
                countConflicts(comparison, ConflictKind.PSEUDO),
                leftChanges.plantuml(),
                rightChanges.plantuml()
        );
    }

    private RenderedMunidiff renderChangesAgainstBase(
            ModelComparisonInput original,
            String branchContent
    ) throws IOException {
        ModelComparisonInput branchAgainstBase = new ModelComparisonInput(
                original.filename(),
                original.previousFilename(),
                branchContent,
                original.baseContent(),
                null
        );

        return render(modelComparator.compare(branchAgainstBase), original);
    }

    private RenderedMunidiff render(Comparison comparison, ModelComparisonInput file) {
        return munidiffRenderer.render(
                comparison,
                config.isMetamodelFile(file.filename())
        );
    }

    private int countConflicts(Comparison comparison, ConflictKind kind) {
        return (int) comparison.getConflicts()
                .stream()
                .filter(conflict -> conflict.getKind() == kind)
                .count();
    }

    private boolean isDeletedWithoutRename(ModelComparisonInput file) {
        return !file.isRename()
                && file.sourceContent() == null
                && (file.targetContent() != null || file.baseContent() != null);
    }

    private String diagnostic(String context, Exception ex) {
        StringBuilder diagnostic = new StringBuilder(context);
        diagnostic.append(System.lineSeparator());
        diagnostic.append(ex.getClass().getName());

        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            diagnostic.append(": ").append(ex.getMessage());
        }

        Throwable cause = ex.getCause();
        if (cause != null && cause != ex) {
            diagnostic.append(System.lineSeparator());
            diagnostic.append("Caused by: ").append(cause.getClass().getName());

            if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                diagnostic.append(": ").append(cause.getMessage());
            }
        }

        diagnostic.append(System.lineSeparator());
        diagnostic.append("See the GitHub Actions logs for the full stack trace.");

        return diagnostic.toString();
    }
}
