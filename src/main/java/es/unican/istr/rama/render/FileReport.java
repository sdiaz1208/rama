package es.unican.istr.rama.render;

/**
 * Renderable report data for one analyzed model file.
 *
 * @param filename the name of the analyzed file
 * @param plantuml the PlantUML source generated for the file report
 * @param textualReport the textual Munidiff report generated for the file
 * @param metamodel whether the analyzed file is a metamodel
 * @param message informational text to show when the file was analyzed but there is nothing to render
 * @param diagnostic diagnostic text to show when analysis failed or needs context
 * @param conflictReport side-by-side conflict report data, when conflict groups were detected
 */
public record FileReport(
        String filename,
        String plantuml,
        String textualReport,
        boolean metamodel,
        String message,
        String diagnostic,
        ConflictReport conflictReport
) {

    public FileReport(String filename, String plantuml) {
        this(filename, plantuml, null, false, null, null, null);
    }

    public FileReport(String filename, String plantuml, String textualReport) {
        this(filename, plantuml, textualReport, false, null, null, null);
    }

    public FileReport(String filename, String plantuml, String textualReport, boolean metamodel) {
        this(filename, plantuml, textualReport, metamodel, null, null, null);
    }

    public static FileReport message(String filename, String message) {
        return new FileReport(filename, null, null, false, message, null, null);
    }

    public static FileReport failure(String filename, String diagnostic) {
        return new FileReport(filename, null, null, false, null, diagnostic, null);
    }

    public static FileReport conflict(String filename, ConflictReport conflictReport) {
        return new FileReport(filename, null, null, false, null, null, conflictReport);
    }

    public boolean hasPlantuml() {
        return plantuml != null && !plantuml.isBlank();
    }

    public boolean hasTextualReport() {
        return !metamodel && textualReport != null && !textualReport.isBlank();
    }

    public boolean hasMessage() {
        return message != null && !message.isBlank();
    }

    public boolean hasDiagnostic() {
        return diagnostic != null && !diagnostic.isBlank();
    }

    public boolean hasConflictReport() {
        return conflictReport != null;
    }
}
