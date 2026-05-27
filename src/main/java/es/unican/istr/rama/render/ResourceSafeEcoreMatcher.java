package es.unican.istr.rama.render;

import java.util.Objects;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.EStringToStringMapEntryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.epsilon.modiff.matcher.Matcher;

/**
 * Ecore matcher that avoids MoDiff's IdMatcher null-resource assumption.
 */
public class ResourceSafeEcoreMatcher implements Matcher {

    /**
     * Matches two objects when both resolve to the same non-null identifier.
     */
    @Override
    public boolean matches(EObject left, EObject right) {
        String leftIdentifier = getIdentifier(left);
        return leftIdentifier != null && leftIdentifier.equals(getIdentifier(right));
    }

    /**
     * Returns the best available identifier for an EObject.
     *
     * @param eObject object to identify
     * @return stable identifier where possible, or {@code null} when no meaningful identifier exists
     */
    @Override
    public String getIdentifier(EObject eObject) {
        if (eObject == null) {
            return null;
        }

        String resourceIdentifier = getResourceIdentifier(eObject);
        if (resourceIdentifier != null) {
            return resourceIdentifier;
        }

        if (eObject instanceof EPackage ePackage) {
            return identifierForPackage(ePackage);
        }
        if (eObject instanceof org.eclipse.emf.ecore.ENamedElement namedElement) {
            return identifierForNamedElement(namedElement);
        }
        if (eObject instanceof EAnnotation annotation) {
            return append(identifierFor(annotation.eContainer()), annotation.getSource());
        }
        if (eObject instanceof EStringToStringMapEntryImpl entry) {
            return append(identifierFor(entry.eContainer()), entry.getKey());
        }

        return fallbackIdentifier(eObject);
    }

    /**
     * Resolves IDs that depend on the object being attached to a resource.
     */
    private String getResourceIdentifier(EObject eObject) {
        Resource resource = eObject.eResource();
        String identifier = null;

        if (resource instanceof XMLResource xmlResource) {
            identifier = xmlResource.getID(eObject);
        }
        if (identifier == null) {
            identifier = EcoreUtil.getID(eObject);
        }
        if (identifier == null && resource != null) {
            identifier = resource.getURIFragment(eObject);
        }

        return identifier;
    }

    private String identifierFor(EObject eObject) {
        return eObject == null ? null : getIdentifier(eObject);
    }

    /**
     * Uses the package namespace URI first because it is the canonical identifier for EPackages.
     */
    private String identifierForPackage(EPackage ePackage) {
        if (ePackage.getNsURI() != null && !ePackage.getNsURI().isBlank()) {
            return ePackage.getNsURI();
        }

        return identifierForNamedElement(ePackage);
    }

    /**
     * Builds an identifier from the containing Ecore path and the element name.
     */
    private String identifierForNamedElement(org.eclipse.emf.ecore.ENamedElement namedElement) {
        String name = namedElement.getName();
        String containerIdentifier = identifierFor(namedElement.eContainer());

        if (containerIdentifier == null) {
            EPackage ePackage = namedElement instanceof EClassifier classifier ? classifier.getEPackage() : null;
            if (ePackage != null && ePackage != namedElement) {
                containerIdentifier = identifierForPackage(ePackage);
            }
        }

        return append(containerIdentifier, name);
    }

    /**
     * Handles non-standard EObjects that still appear inside Ecore comparison graphs.
     */
    private String fallbackIdentifier(EObject eObject) {
        EStructuralFeature nameFeature = eObject.eClass().getEStructuralFeature("name");
        if (nameFeature != null) {
            Object name = eObject.eGet(nameFeature);
            if (name != null) {
                return append(identifierFor(eObject.eContainer()), name.toString());
            }
        }

        EClass eClass = eObject.eClass();
        String className = eClass == null ? eObject.getClass().getName() : eClass.getName();
        return append(identifierFor(eObject.eContainer()), className + "@" + System.identityHashCode(eObject));
    }

    /**
     * Joins path segments while tolerating missing parent or child values.
     */
    private String append(String parent, String child) {
        String safeChild = Objects.toString(child, "");
        if (parent == null || parent.isBlank()) {
            return safeChild.isBlank() ? null : safeChild;
        }
        return safeChild.isBlank() ? parent : parent + "::" + safeChild;
    }
}
