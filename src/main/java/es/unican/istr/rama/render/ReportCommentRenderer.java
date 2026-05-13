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
        body.append(COMMENT_MARKER).append("\n");
        body.append("## RAMA Analysis Report").append("\n\n");

        if (reports.isEmpty()) {
            body.append("No model files were analyzed.");
            return new ReportComment(COMMENT_MARKER, body.toString());
        }

        for (FileReport report : reports) {
            String imageUrl = plantUMLEncoderService.generateURL(report.plantuml());
            body.append("### <code>").append(escapeHtml(report.filename())).append("</code>\n\n");
            body.append("<details>\n");
            body.append("<summary>Rendered SVG</summary>\n\n");
            body.append("<img src=\"").append(imageUrl).append("\" alt=\"RAMA diagram for ");
            body.append(escapeHtml(report.filename())).append("\" />\n\n");
            body.append("</details>\n\n");
        }

        return new ReportComment(COMMENT_MARKER, body.toString());
    }

    /**
     * Escapes special HTML characters in a string to prevent rendering issues in provider comments.
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

}
