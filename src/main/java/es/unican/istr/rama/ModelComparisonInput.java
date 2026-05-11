package es.unican.istr.rama;

public record ModelComparisonInput(
        String filename,
        String sourceContent,
        String targetContent,
        String baseContent
) {
}
