package es.unican.istr;

public record ModelComparisonInput(
        String filename,
        String sourceContent,
        String targetContent,
        String baseContent
) {
}
