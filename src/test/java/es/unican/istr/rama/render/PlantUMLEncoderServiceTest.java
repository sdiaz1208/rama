package es.unican.istr.rama.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.sourceforge.plantuml.code.NoPlantumlCompressionException;
import net.sourceforge.plantuml.code.TranscoderUtil;

class PlantUMLEncoderServiceTest {

    private final PlantUMLEncoderService service = new PlantUMLEncoderService();

    @Test
    void encodeRejectsNullSource() {
        assertThrows(IllegalArgumentException.class, () -> service.encode(null));
    }

    @Test
    void encodeRejectsEmptySource() {
        assertThrows(IllegalArgumentException.class, () -> service.encode(""));
    }

    @Test
    void encodeRejectsBlankSource() {
        assertThrows(IllegalArgumentException.class, () -> service.encode(" \n\t "));
    }

    @Test
    void encodedSourceCanBeDecodedByPlantumlTranscoder() throws NoPlantumlCompressionException {
        String umlSource = "@startuml\nAlice -> Bob: hello\n@enduml";

        String encoded = service.encode(umlSource);

        assertEquals(umlSource, TranscoderUtil.getDefaultTranscoder().decode(encoded));
    }

    @Test
    void generateUrlUsesDefaultPlantumlServer() {
        String url = service.generateURL("@startuml\nAlice -> Bob: hello\n@enduml");

        assertTrue(url.startsWith(PlantUMLEncoderService.DEFAULT_SERVER));
    }

    @Test
    void generateUrlAddsSlashWhenServerUrlHasNoTrailingSlash() {
        String umlSource = "@startuml\nAlice -> Bob: hello\n@enduml";

        String url = service.generateURL(umlSource, "https://plantuml.example/svg");

        assertEquals("https://plantuml.example/svg/" + service.encode(umlSource), url);
    }

    @Test
    void generateUrlDoesNotAddExtraSlashWhenServerUrlHasTrailingSlash() {
        String umlSource = "@startuml\nAlice -> Bob: hello\n@enduml";

        String url = service.generateURL(umlSource, "https://plantuml.example/svg/");

        assertEquals("https://plantuml.example/svg/" + service.encode(umlSource), url);
    }
}
