package es.unican.istr.rama.comparison;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.emf.compare.Comparison;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import es.unican.istr.rama.config.RamaConfig;

class EmfModelComparatorTest {

    private static final String LIBRARY_ECORE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EPackage
                xmi:version="2.0"
                xmlns:xmi="http://www.omg.org/XMI"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
                name="library"
                nsURI="http://example/library"
                nsPrefix="library">
              <eClassifiers xsi:type="ecore:EClass" name="Book"/>
            </ecore:EPackage>
            """;

    private static final String RENAMED_LIBRARY_ECORE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <ecore:EPackage
                xmi:version="2.0"
                xmlns:xmi="http://www.omg.org/XMI"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:ecore="http://www.eclipse.org/emf/2002/Ecore"
                name="catalog"
                nsURI="http://example/library"
                nsPrefix="library">
              <eClassifiers xsi:type="ecore:EClass" name="Publication"/>
            </ecore:EPackage>
            """;

    private static final String LIBRARY_MODEL = """
            <?xml version="1.0" encoding="UTF-8"?>
            <library:Book
                xmi:version="2.0"
                xmlns:xmi="http://www.omg.org/XMI"
                xmlns:library="http://example/library"/>
            """;

    @TempDir
    Path tempDir;

    @Test
    void identicalSmallEcoreInputsProduceZeroDifferences() throws Exception {
        EmfModelComparator comparator = new EmfModelComparator(configWithNoMetamodels(), tempDir);

        Comparison comparison = comparator.compare(new ModelComparisonInput(
                "library.ecore",
                LIBRARY_ECORE,
                LIBRARY_ECORE,
                null
        ));

        assertTrue(comparison.getDifferences().isEmpty());
    }

    @Test
    void changedClassAndPackageNamesProduceDifferences() throws Exception {
        EmfModelComparator comparator = new EmfModelComparator(configWithNoMetamodels(), tempDir);

        Comparison comparison = comparator.compare(new ModelComparisonInput(
                "library.ecore",
                RENAMED_LIBRARY_ECORE,
                LIBRARY_ECORE,
                null
        ));

        assertFalse(comparison.getDifferences().isEmpty());
    }

    @Test
    void nullSourceTargetAndBaseContentAreHandledAsMissingContent() {
        EmfModelComparator comparator = new EmfModelComparator(configWithNoMetamodels(), tempDir);

        assertDoesNotThrow(() -> comparator.compare(new ModelComparisonInput(
                "missing.ecore",
                null,
                null,
                null
        )));
    }

    @Test
    void configuredMetamodelPathsResolveRelativeToWorkspace() throws Exception {
        Path metamodelsDir = Files.createDirectories(tempDir.resolve("metamodels"));
        Files.writeString(metamodelsDir.resolve("library.ecore"), LIBRARY_ECORE);

        RamaConfig config = new RamaConfig(
                List.of(".model"),
                List.of(".ecore"),
                List.of("metamodels/library.ecore")
        );
        EmfModelComparator comparator = new EmfModelComparator(config, tempDir);

        Comparison comparison = comparator.compare(new ModelComparisonInput(
                "books.model",
                LIBRARY_MODEL,
                LIBRARY_MODEL,
                null
        ));

        assertTrue(comparison.getDifferences().isEmpty());
    }

    private RamaConfig configWithNoMetamodels() {
        return new RamaConfig(List.of(".model"), List.of(".ecore"), List.of());
    }
}
