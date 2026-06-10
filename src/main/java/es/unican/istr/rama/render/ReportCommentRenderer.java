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

            if (report.hasConflictReport()) {
                appendConflictReport(body, report);
            }
            else if (report.hasPlantuml() || report.hasTextualReport()) {
                appendGraphicalReport(body, report);
                appendTextualReport(body, report);
            }
            else {
                body.append("RAMA could not analyze this file.\n\n");
            }

            appendDiagnostic(body, report.diagnostic());
        }

        return new ReportComment(COMMENT_MARKER, body.toString());
    }

    private void appendGraphicalReport(StringBuilder body, FileReport report) {
        if (!report.hasPlantuml()) {
            return;
        }

        body.append("<details>\n");
        body.append("<summary>Graphical Report</summary>\n\n");
        appendPlantumlImage(body, report.plantuml(), "RAMA diagram for " + report.filename());
        body.append("\n");
        body.append("</details>\n\n");
    }

    private void appendTextualReport(StringBuilder body, FileReport report) {
        if (!report.hasTextualReport()) {
            return;
        }

        body.append("<details>\n");
        body.append("<summary>Textual Report</summary>\n\n");
        body.append("```diff\n");
        body.append(report.textualReport()).append("\n");
        body.append("```\n\n");
        body.append("</details>\n\n");
    }

    private void appendConflictReport(StringBuilder body, FileReport report) {
        ConflictReport conflictReport = report.conflictReport();

        body.append("RAMA detected ");
        body.append(conflictReport.conflictCount());
        body.append(" EMF Compare conflict group");
        if (conflictReport.conflictCount() != 1) {
            body.append("s");
        }
        body.append(" (");
        body.append(conflictReport.realConflictCount()).append(" real, ");
        body.append(conflictReport.pseudoConflictCount()).append(" pseudo");
        body.append(").\n\n");

        body.append("<details open>\n");
        body.append("<summary>Branch changes against merge-base</summary>\n\n");
        body.append("<table>\n");
        body.append("<thead>\n");
        body.append("<tr>");
        body.append("<th>Left/source changes vs merge-base</th>");
        body.append("<th>Right/target changes vs merge-base</th>");
        body.append("</tr>\n");
        body.append("</thead>\n");
        body.append("<tbody>\n");
        body.append("<tr>\n");
        body.append("<td valign=\"top\">");
        appendConflictImageOrFallback(
                body,
                conflictReport.leftPlantuml(),
                "RAMA left/source changes against merge-base for " + report.filename()
        );
        body.append("</td>\n");
        body.append("<td valign=\"top\">");
        appendConflictImageOrFallback(
                body,
                conflictReport.rightPlantuml(),
                "RAMA right/target changes against merge-base for " + report.filename()
        );
        body.append("</td>\n");
        body.append("</tr>\n");
        body.append("</tbody>\n");
        body.append("</table>\n\n");
        body.append("</details>\n\n");
    }

    private void appendConflictImageOrFallback(StringBuilder body, String plantuml, String altText) {
        if (plantuml == null || plantuml.isBlank()) {
            body.append("No renderable branch changes.");
            return;
        }

        appendPlantumlImage(body, plantuml, altText);
    }

    private void appendPlantumlImage(StringBuilder body, String plantuml, String altText) {
        String imageUrl = plantUMLEncoderService.generateURL(plantuml);
        body.append("<img src=\"").append(escapeHtml(imageUrl)).append("\" alt=\"");
        body.append(escapeHtml(altText)).append("\" />\n\n");
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
