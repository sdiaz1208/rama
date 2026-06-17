package es.unican.istr.rama.comparison;

public record ModelComparisonInput(
        String filename,
        String previousFilename,
        String sourceContent,
        String targetContent,
        String baseContent
) {
    public ModelComparisonInput(
            String filename,
            String sourceContent,
            String targetContent,
            String baseContent
    ) {
        this(filename, null, sourceContent, targetContent, baseContent);
    }

    public boolean isRename() {
        return previousFilename != null && !previousFilename.isBlank();
    }
}
