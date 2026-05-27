package es.unican.istr.rama.comparison;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import es.unican.istr.rama.config.RamaConfig;

public class EmfModelComparator implements ComparisonService {

    private final RamaConfig config;
    private final Path workspacePath;

    public EmfModelComparator(RamaConfig config, Path workspacePath) {
        this.config = config;
        this.workspacePath = workspacePath == null ? Path.of("") : workspacePath;
    }

    /**
     * Compares the source, target, and base contents of a model file using EMF Compare.
     *
     * @param input the input containing the filename and contents for source, target, and base
     * @return the EMF Compare Comparison result
     * @throws IOException if an I/O error occurs while loading the resources
     */
    @Override
    public Comparison compare(ModelComparisonInput input) throws IOException {
        Resource.Factory.Registry.INSTANCE
                .getExtensionToFactoryMap()
                .put("*", new XMIResourceFactoryImpl());

        ResourceSet resourceSet = new ResourceSetImpl();

        registerMetamodels(resourceSet);

        Resource source = loadResource(resourceSet, input.filename(), "source", input.sourceContent());
        Resource target = loadResource(resourceSet, input.filename(), "target", input.targetContent());
        Resource base = input.baseContent() == null
                ? null
                : loadResource(resourceSet, input.filename(), "base", input.baseContent());

        IComparisonScope scope = new DefaultComparisonScope(
                source,
                target,
                base
        );

        return EMFCompare.builder()
                .build()
                .compare(scope);
    }

    /**
     * Registers the metamodels specified in the configuration with the given ResourceSet.
     *
     * @param resourceSet the ResourceSet with which to register the metamodels
     */
    private void registerMetamodels(ResourceSet resourceSet) {
        for (String metamodel : config.metamodelPaths()) {
            Resource metamodelResource = resourceSet.getResource(
                    URI.createFileURI(resolveMetamodelPath(metamodel).toString()),
                    true
            );

            for (EObject root : metamodelResource.getContents()) {
                if (root instanceof EPackage ePackage) {
                    registerPackage(resourceSet, ePackage);
                }
            }
        }
    }

    /**
     * Recursively registers an EPackage and its subpackages with the given ResourceSet.
     *
     * @param resourceSet the ResourceSet with which to register the EPackage
     * @param ePackage the EPackage to register
     */
    private void registerPackage(ResourceSet resourceSet, EPackage ePackage) {
        resourceSet.getPackageRegistry().put(ePackage.getNsURI(), ePackage);

        for (EPackage subpackage : ePackage.getESubpackages()) {
            registerPackage(resourceSet, subpackage);
        }
    }

    /**
     * Resolves the path to a metamodel file.
     *
     * @param metamodel the path to the metamodel file
     * @return the resolved Path to the metamodel file
     */
    private Path resolveMetamodelPath(String metamodel) {
        Path metamodelPath = Path.of(metamodel);

        if (metamodelPath.isAbsolute()) {
            return metamodelPath;
        }

        return workspacePath.resolve(metamodelPath).normalize();
    }

    /**
     * Helper method to load a resource from a string content. If the content is null, an empty
     * resource will be created.
     *
     * @param resourceSet the resource set to which the resource will be added
     * @param filename the name of the file
     * @param side the side of the comparison (source, target, or base)
     * @param content the content of the resource
     * @return the loaded resource
     * @throws IOException if an I/O error occurs
     */
    private Resource loadResource(
            ResourceSet resourceSet,
            String filename,
            String side,
            String content
    ) throws IOException {
        Resource resource = resourceSet.createResource(resourceUri(filename, side));

        if (content != null) {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
                resource.load(inputStream, null);
            }
        }

        return resource;
    }

    /**
     * Helper method to create a URI for a resource based on the filename and side. This ensures
     * that each resource has a unique URI, which is important for EMF's resource management.
     *
     * @param filename the name of the file
     * @param side the side of the comparison (source, target, or base)
     * @return the URI for the resource
     */
    private URI resourceUri(String filename, String side) {
        return URI.createURI("rama://comparison/" + side + "/" + URI.encodeSegment(filename, false));
    }
}
