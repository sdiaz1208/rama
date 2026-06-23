package es.unican.istr.rama.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ReportCommentRendererTest {

    private final ReportCommentRenderer renderer = new ReportCommentRenderer(new StubPlantUMLEncoderService());

    @Test
    void emptyReportListProducesMarkerAndEmptyMessage() {
        ReportComment comment = renderer.render(List.of());

        assertEquals("<!-- RAMA:SVG-REPORT -->", comment.marker());
        assertTrue(comment.body().contains("<!-- RAMA:SVG-REPORT -->"));
        assertTrue(comment.body().contains("No changes to any model/metamodel files were detected."));
    }

    @Test
    void configurationWarningIsShownBeforeTheReport() {
        ReportComment comment = renderer.render(
                List.of(),
                "`rama.json` is invalid or unreadable. RAMA is using the packaged default configuration."
        );

        assertTrue(comment.body().contains("**Configuration warning:**"));
        assertTrue(comment.body().contains("`rama.json` is invalid or unreadable."));
    }

    @Test
    void oneRenderedReportProducesGraphicalAndTextualDropdowns() {
        ReportComment comment = renderer.render(List.of(new FileReport(
                "models/example.model",
                "plantuml-source",
                "diff --model <example>&\""
        )));

        assertTrue(comment.body().contains("### <code>models/example.model</code>"));
        assertTrue(comment.body().contains("<details>"));
        assertTrue(comment.body().contains("<summary>Graphical Report</summary>"));
        assertTrue(comment.body().contains("<img src=\"https://plantuml.test/plantuml-source\""));
        assertTrue(comment.body().contains("<summary>Textual Report</summary>"));
        assertTrue(comment.body().contains("```diff\ndiff --model <example>&\"\n```"));
    }

    @Test
    void metamodelReportDoesNotProduceTextualDropdown() {
        ReportComment comment = renderer.render(List.of(new FileReport(
                "metamodels/example.ecore",
                "plantuml-source",
                "diff --metamodel",
                true
        )));

        assertTrue(comment.body().contains("<summary>Graphical Report</summary>"));
        assertTrue(comment.body().contains("<img src=\"https://plantuml.test/plantuml-source\""));
        assertFalse(comment.body().contains("<summary>Textual Report</summary>"));
        assertFalse(comment.body().contains("```diff\ndiff --metamodel\n```"));
    }

    @Test
    void multipleReportsPreserveOrder() {
        ReportComment comment = renderer.render(List.of(
                new FileReport("models/first.model", "first"),
                new FileReport("models/second.model", "second")
        ));

        assertTrue(comment.body().indexOf("models/first.model") < comment.body().indexOf("models/second.model"));
    }

    @Test
    void filenamesAreEscapedInSectionAndAltText() {
        ReportComment comment = renderer.render(List.of(new FileReport("models/a<&>\".model", "plantuml-source")));

        assertTrue(comment.body().contains("models/a&lt;&amp;&gt;&quot;.model"));
        assertTrue(comment.body().contains("alt=\"RAMA diagram for models/a&lt;&amp;&gt;&quot;.model\""));
    }

    @Test
    void failureReportIncludesEscapedDiagnostic() {
        ReportComment comment = renderer.render(List.of(FileReport.failure(
                "models/broken.model",
                "Could not load <bad>&\"model\""
        )));

        assertTrue(comment.body().contains("RAMA could not analyze this file."));
        assertTrue(comment.body().contains("Could not load &lt;bad&gt;&amp;&quot;model&quot;"));
    }

    @Test
    void informationalReportShowsMessageInsteadOfRenderedSvg() {
        ReportComment comment = renderer.render(List.of(FileReport.message(
                "models/serialization-only.model",
                "No model-level changes were detected in this file."
        )));

        assertTrue(comment.body().contains("### <code>models/serialization-only.model</code>"));
        assertTrue(comment.body().contains("No model-level changes were detected in this file."));
        assertTrue(!comment.body().contains("<summary>Rendered SVG</summary>"));
    }

    private static class StubPlantUMLEncoderService extends PlantUMLEncoderService {
        @Override
        public String generateURL(String umlSource) {
            return "https://plantuml.test/" + umlSource;
        }
    }
}
