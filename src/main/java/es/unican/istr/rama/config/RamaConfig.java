package es.unican.istr.rama.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

// JSON shape of rama.json.
public record RamaConfig(
        @JsonProperty("model_extensions") List<String> modelExtensions,
        @JsonProperty("metamodel_extensions") List<String> metamodelExtensions,
        @JsonProperty("metamodels") List<String> metamodels
) {
    public boolean isRelevantFile(String filename) {
        if (filename == null) {
            return false;
        }

        return relevantFileExtensions().stream().anyMatch(filename::endsWith);
    }

    public boolean isMetamodelFile(String filename) {
        if (filename == null) {
            return false;
        }

        return metamodelFileExtensions().stream().anyMatch(filename::endsWith);
    }

    public List<String> metamodelPaths() {
        return metamodels == null ? List.of() : metamodels;
    }

    private List<String> relevantFileExtensions() {
        List<String> extensions = new ArrayList<>();
        extensions.addAll(modelExtensions == null ? List.of() : modelExtensions);
        extensions.addAll(metamodelFileExtensions());
        return extensions;
    }

    private List<String> metamodelFileExtensions() {
        return metamodelExtensions == null ? List.of() : metamodelExtensions;
    }
}
