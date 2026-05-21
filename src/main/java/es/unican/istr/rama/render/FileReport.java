package es.unican.istr.rama.render;

/**
 * Renderable report data for one analyzed model file.
 *
 * @param filename the name of the analyzed file
 * @param plantuml the PlantUML source generated for the file report
 * @param diagnostic diagnostic text to show when analysis failed or needs context
 */
public record FileReport(String filename, String plantuml, String diagnostic) {

    public FileReport(String filename, String plantuml) {
        this(filename, plantuml, null);
    }

    public static FileReport failure(String filename, String diagnostic) {
        return new FileReport(filename, null, diagnostic);
    }

    public boolean hasPlantuml() {
        return plantuml != null && !plantuml.isBlank();
    }

    public boolean hasDiagnostic() {
        return diagnostic != null && !diagnostic.isBlank();
    }
}
