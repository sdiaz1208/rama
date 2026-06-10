package es.unican.istr.rama.render;

/**
 * Rendered provider comment for a RAMA report.
 *
 * @param marker stable marker included in RAMA report comments
 * @param body the full comment body to publish
 */
public record ReportComment(String marker, String body) {
}
