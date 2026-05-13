package es.unican.istr.rama.comparison;

public record ModelComparisonInput(
        String filename,
        String sourceContent,
        String targetContent,
        String baseContent
) {
}
