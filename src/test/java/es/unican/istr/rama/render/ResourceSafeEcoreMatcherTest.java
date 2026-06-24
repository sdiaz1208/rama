package es.unican.istr.rama.render;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.impl.EStringToStringMapEntryImpl;
import org.junit.jupiter.api.Test;

class ResourceSafeEcoreMatcherTest {

    private final ResourceSafeEcoreMatcher matcher = new ResourceSafeEcoreMatcher();

    @Test
    void getIdentifierReturnsNullForNullObject() {
        assertNull(matcher.getIdentifier(null));
    }

    @Test
    void matchesReturnsFalseWhenEitherSideHasNoIdentifier() {
        EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
        ePackage.setName("library");
        ePackage.setNsURI("http://example/library");

        assertFalse(matcher.matches(null, ePackage));
        assertFalse(matcher.matches(ePackage, null));
    }

    @Test
    void packageIdentifierPrefersNsUri() {
        EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
        ePackage.setName("library");
        ePackage.setNsURI("http://example/library");

        assertEquals("http://example/library", matcher.getIdentifier(ePackage));
    }

    @Test
    void classIdentifierIncludesPackageIdentityAndClassName() {
        EPackage ePackage = EcoreFactory.eINSTANCE.createEPackage();
        ePackage.setName("library");
        ePackage.setNsURI("http://example/library");

        EClass eClass = EcoreFactory.eINSTANCE.createEClass();
        eClass.setName("Book");
        ePackage.getEClassifiers().add(eClass);

        assertEquals("http://example/library::Book", matcher.getIdentifier(eClass));
    }

    @Test
    void detachedEcoreObjectsDoNotThrow() {
        EClass eClass = EcoreFactory.eINSTANCE.createEClass();
        eClass.setName("Detached");

        assertEquals("Detached", matcher.getIdentifier(eClass));
    }

    @Test
    void annotationIdentifierUsesContainerAndSource() {
        EClass eClass = EcoreFactory.eINSTANCE.createEClass();
        eClass.setName("Book");

        EAnnotation annotation = EcoreFactory.eINSTANCE.createEAnnotation();
        annotation.setSource("documentation");
        eClass.getEAnnotations().add(annotation);

        assertEquals("Book::documentation", matcher.getIdentifier(annotation));
    }

    @Test
    void mapEntryIdentifierUsesContainerAndKey() {
        EAnnotation annotation = EcoreFactory.eINSTANCE.createEAnnotation();
        annotation.setSource("documentation");
        annotation.getDetails().put("label", "Book label");

        EStringToStringMapEntryImpl entry = (EStringToStringMapEntryImpl) annotation.getDetails().get(0);

        assertEquals("documentation::label", matcher.getIdentifier(entry));
    }
}
