package es.unican.istr.rama.render;

/**
 * Rendered provider comment for a RAMA report.
 *
 * @param marker stable marker used to find and update an existing comment
 * @param body the full comment body to publish
 */
public record ReportComment(String marker, String body) {
}
