package es.unican.istr.rama.render;

import java.util.List;

public class ReportCommentRenderer {

    private static final String COMMENT_MARKER = "<!-- RAMA:SVG-REPORT -->";

    private final PlantUMLEncoderService plantUMLEncoderService;

    public ReportCommentRenderer(PlantUMLEncoderService plantUMLEncoderService) {
        this.plantUMLEncoderService = plantUMLEncoderService;
    }

    /**
     * Builds the provider comment containing rendered SVG images.
     *
     * @param reports the file reports to render
     * @return the provider comment to publish
     */
    public ReportComment render(List<FileReport> reports) {
        StringBuilder body = new StringBuilder();
        appendHeader(body);

        if (reports.isEmpty()) {
            body.append("No model files were analyzed.");
            return new ReportComment(COMMENT_MARKER, body.toString());
        }

        for (FileReport report : reports) {
            body.append("### <code>").append(escapeHtml(report.filename())).append("</code>\n\n");

            if (report.hasPlantuml()) {
                String imageUrl = plantUMLEncoderService.generateURL(report.plantuml());
                body.append("<details>\n");
                body.append("<summary>Rendered SVG</summary>\n\n");
                body.append("<img src=\"").append(imageUrl).append("\" alt=\"RAMA diagram for ");
                body.append(escapeHtml(report.filename())).append("\" />\n\n");
                body.append("</details>\n\n");
            }
            else {
                body.append("RAMA could not analyze this file.\n\n");
            }

            appendDiagnostic(body, report.diagnostic());
        }

        return new ReportComment(COMMENT_MARKER, body.toString());
    }

    /**
     * Builds a provider comment for failures that happen before per-file analysis can start.
     *
     * @param diagnostic diagnostic text explaining why the analysis could not run
     * @return the provider comment to publish
     */
    public ReportComment renderFailure(String diagnostic) {
        StringBuilder body = new StringBuilder();
        appendHeader(body);
        body.append("RAMA could not analyze this pull request.\n\n");
        appendDiagnostic(body, diagnostic);
        return new ReportComment(COMMENT_MARKER, body.toString());
    }

    private void appendHeader(StringBuilder body) {
        body.append(COMMENT_MARKER).append("\n");
        body.append("## RAMA Analysis Report").append("\n\n");
    }

    private void appendDiagnostic(StringBuilder body, String diagnostic) {
        if (diagnostic == null || diagnostic.isBlank()) {
            return;
        }

        body.append("<details>\n");
        body.append("<summary>Diagnostic</summary>\n\n");
        body.append("<pre><code>").append(escapeHtml(diagnostic)).append("</code></pre>\n\n");
        body.append("</details>\n\n");
    }

    /**
     * Escapes special HTML characters in a string to prevent rendering issues in provider comments.
     *
     * @param text the text to escape
     * @return the escaped text
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

}
