package es.unican.istr.rama.render;

/**
 * Renderable report data for one analyzed model file.
 *
 * @param filename the name of the analyzed file
 * @param plantuml the PlantUML source generated for the file report
 */
public record FileReport(String filename, String plantuml) {
}
