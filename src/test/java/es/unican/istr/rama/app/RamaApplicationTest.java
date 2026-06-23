package es.unican.istr.rama.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.eclipse.emf.compare.CompareFactory;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.Match;
import org.junit.jupiter.api.Test;

import es.unican.istr.rama.comparison.ComparisonService;
import es.unican.istr.rama.comparison.ModelComparisonInput;
import es.unican.istr.rama.config.RamaConfig;
import es.unican.istr.rama.git.GitService;
import es.unican.istr.rama.render.MunidiffRenderer;
import es.unican.istr.rama.render.RenderedMunidiff;
import es.unican.istr.rama.render.ReportComment;
import es.unican.istr.rama.render.ReportCommentRenderer;

class RamaApplicationTest {

    @Test
    void semanticNoOpChangeShowsInformationalMessageAndSkipsRendering() throws Exception {
        ModelComparisonInput file = new ModelComparisonInput("models/example.model", "<new/>", "<old/>", null);
        FakeGitService gitService = new FakeGitService(List.of(file));
        StubComparisonService comparator = new StubComparisonService(comparisonWithoutDifferences());
        TrackingMunidiffRenderer renderer = new TrackingMunidiffRenderer();

        RamaApplication app = new RamaApplication(
                new RamaConfig(List.of(".model"), List.of(".ecore"), List.of()),
                gitService,
                comparator,
                renderer,
                new ReportCommentRenderer(new StubPlantUMLEncoderService())
        );

        app.run(42);

        assertEquals(42, gitService.publishedPullRequestNumber);
        assertFalse(renderer.renderCalled);
        assertTrue(gitService.publishedComment.body().contains("No model-level changes were detected in this file."));
        assertFalse(gitService.publishedComment.body().contains("<summary>Graphical Report</summary>"));
    }

    @Test
    void semanticChangeStillRendersDiagram() throws Exception {
        ModelComparisonInput file = new ModelComparisonInput("models/example.model", "<new/>", "<old/>", null);
        FakeGitService gitService = new FakeGitService(List.of(file));
        StubComparisonService comparator = new StubComparisonService(comparisonWithOneDifference());
        TrackingMunidiffRenderer renderer = new TrackingMunidiffRenderer();

        RamaApplication app = new RamaApplication(
                new RamaConfig(List.of(".model"), List.of(".ecore"), List.of()),
                gitService,
                comparator,
                renderer,
                new ReportCommentRenderer(new StubPlantUMLEncoderService())
        );

        app.run(7);

        assertTrue(renderer.renderCalled);
        assertTrue(gitService.publishedComment.body().contains("<summary>Graphical Report</summary>"));
        assertTrue(gitService.publishedComment.body().contains("<summary>Textual Report</summary>"));
        assertTrue(gitService.publishedComment.body().contains("https://plantuml.test/rendered-plantuml"));
        assertTrue(gitService.publishedComment.body().contains("rendered-unified-diff"));
    }

    @Test
    void deletedFileShowsDeletionMessageBeforeComparison() throws Exception {
        ModelComparisonInput file = new ModelComparisonInput(
                "models/deleted.model",
                null,
                "<old/>",
                "<old/>"
        );
        FakeGitService gitService = new FakeGitService(List.of(file));
        StubComparisonService comparator = new StubComparisonService(comparisonWithOneDifference());
        TrackingMunidiffRenderer renderer = new TrackingMunidiffRenderer();

        RamaApplication app = new RamaApplication(
                new RamaConfig(List.of(".model"), List.of(".ecore"), List.of()),
                gitService,
                comparator,
                renderer,
                new ReportCommentRenderer(new StubPlantUMLEncoderService())
        );

        app.run(11);

        assertFalse(comparator.compareCalled);
        assertFalse(renderer.renderCalled);
        assertTrue(gitService.publishedComment.body().contains("This file was deleted."));
        assertFalse(gitService.publishedComment.body().contains("<summary>Graphical Report</summary>"));
    }

    @Test
    void configurationWarningIsIncludedInPublishedComment() throws Exception {
        FakeGitService gitService = new FakeGitService(List.of());

        RamaApplication app = new RamaApplication(
                new RamaConfig(List.of(".model"), List.of(".ecore"), List.of()),
                gitService,
                new StubComparisonService(comparisonWithoutDifferences()),
                new TrackingMunidiffRenderer(),
                new ReportCommentRenderer(new StubPlantUMLEncoderService()),
                "`rama.json` is invalid or unreadable. RAMA is using the packaged default configuration."
        );

        app.run(21);

        assertTrue(gitService.publishedComment.body().contains("**Configuration warning:**"));
        assertTrue(gitService.publishedComment.body().contains("`rama.json` is invalid or unreadable."));
    }

    private static Comparison comparisonWithoutDifferences() {
        return CompareFactory.eINSTANCE.createComparison();
    }

    private static Comparison comparisonWithOneDifference() {
        Comparison comparison = CompareFactory.eINSTANCE.createComparison();
        Match match = CompareFactory.eINSTANCE.createMatch();
        Diff diff = CompareFactory.eINSTANCE.createReferenceChange();
        match.getDifferences().add(diff);
        comparison.getMatches().add(match);
        return comparison;
    }

    private static class FakeGitService implements GitService {
        private final List<ModelComparisonInput> files;
        private int publishedPullRequestNumber = -1;
        private ReportComment publishedComment;

        private FakeGitService(List<ModelComparisonInput> files) {
            this.files = files;
        }

        @Override
        public List<ModelComparisonInput> getModelFiles(int pullRequestNumber) {
            return files;
        }

        @Override
        public void publishComment(int pullRequestNumber, ReportComment comment) throws IOException {
            this.publishedPullRequestNumber = pullRequestNumber;
            this.publishedComment = comment;
        }
    }

    private static class StubComparisonService implements ComparisonService {
        private final Comparison comparison;
        private boolean compareCalled;

        private StubComparisonService(Comparison comparison) {
            this.comparison = comparison;
        }

        @Override
        public Comparison compare(ModelComparisonInput input) {
            compareCalled = true;
            return comparison;
        }
    }

    private static class TrackingMunidiffRenderer extends MunidiffRenderer {
        private boolean renderCalled;

        @Override
        public RenderedMunidiff render(Comparison comparison, boolean ecoreDiff) {
            renderCalled = true;
            return new RenderedMunidiff(null, "rendered-unified-diff", "rendered-plantuml", "<svg/>");
        }
    }

    private static class StubPlantUMLEncoderService extends es.unican.istr.rama.render.PlantUMLEncoderService {
        @Override
        public String generateURL(String umlSource) {
            return "https://plantuml.test/" + umlSource;
        }
    }
}
