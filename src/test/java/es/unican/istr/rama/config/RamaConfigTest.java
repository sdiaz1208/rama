package es.unican.istr.rama.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class RamaConfigTest {

    @Test
    void modelExtensionsAreRelevantFiles() {
        RamaConfig config = new RamaConfig(List.of(".model"), List.of(".ecore"), List.of());

        assertTrue(config.isRelevantFile("models/example.model"));
    }

    @Test
    void metamodelExtensionsAreRelevantFiles() {
        RamaConfig config = new RamaConfig(List.of(".model"), List.of(".ecore"), List.of());

        assertTrue(config.isRelevantFile("metamodels/example.ecore"));
    }

    @Test
    void unrelatedExtensionsAreNotRelevantFiles() {
        RamaConfig config = new RamaConfig(List.of(".model"), List.of(".ecore"), List.of());

        assertFalse(config.isRelevantFile("README.md"));
    }

    @Test
    void onlyMetamodelExtensionsAreMetamodelFiles() {
        RamaConfig config = new RamaConfig(List.of(".model"), List.of(".ecore"), List.of());

        assertTrue(config.isMetamodelFile("metamodels/example.ecore"));
        assertFalse(config.isMetamodelFile("models/example.model"));
    }

    @Test
    void nullExtensionListsAreHandledSafely() {
        RamaConfig config = new RamaConfig(null, null, List.of());

        assertFalse(config.isRelevantFile("models/example.model"));
        assertFalse(config.isMetamodelFile("metamodels/example.ecore"));
    }

    @Test
    void nullMetamodelsReturnEmptyMetamodelPaths() {
        RamaConfig config = new RamaConfig(List.of(".model"), List.of(".ecore"), null);

        assertTrue(config.metamodelPaths().isEmpty());
    }

    @Test
    void configuredMetamodelsAreReturnedAsMetamodelPaths() {
        List<String> metamodels = List.of("metamodels/library.ecore", "/opt/shared/base.ecore");
        RamaConfig config = new RamaConfig(List.of(".model"), List.of(".ecore"), metamodels);

        assertSame(metamodels, config.metamodelPaths());
    }
}
