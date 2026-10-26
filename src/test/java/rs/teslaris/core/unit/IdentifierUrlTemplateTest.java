package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import rs.teslaris.core.util.exceptionhandling.exception.IdentifierException;
import rs.teslaris.core.util.persistence.IdentifierUtil;

public class IdentifierUrlTemplateTest {

    @ParameterizedTest
    @CsvSource({
        "https://orcid.org/{ID},                                  0000-0002-1825-0097, https://orcid.org/0000-0002-1825-0097",
        "https://www.scopus.com/authid/detail.uri?authorId={ID},   12345,               https://www.scopus.com/authid/detail.uri?authorId=12345",
        "https://bib.cobiss.net/biblioweb/biblio/sr/scr/cris/{ID}, 67890,               https://bib.cobiss.net/biblioweb/biblio/sr/scr/cris/67890",
        "https://example.org/{ID}/publications,                    A123,                https://example.org/A123/publications",
        "https://example.org/a?id={ID}&format=html,                A123,                https://example.org/a?id=A123&format=html"
    })
    void shouldSubstitutePlaceholderWhereverItSits(String urlTemplate, String value,
                                                   String expected) {
        assertEquals(expected, IdentifierUtil.buildIdentifierUrl(urlTemplate, value));
    }

    @Test
    void shouldSubstituteEveryPlaceholderOccurrence() {
        assertEquals("https://example.org/A1/profile/A1",
            IdentifierUtil.buildIdentifierUrl("https://example.org/{ID}/profile/{ID}", "A1"));
    }

    @Test
    void shouldTrimTemplateAndValueBeforeSubstituting() {
        assertEquals("https://orcid.org/0000-0002-1825-0097",
            IdentifierUtil.buildIdentifierUrl("  https://orcid.org/{ID}  ",
                "  0000-0002-1825-0097  "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"https://orcid.org/", "https://orcid.org"})
    void shouldNotBuildUrlWithoutPlaceholder(String urlTemplate) {
        assertNull(IdentifierUtil.buildIdentifierUrl(urlTemplate, "12345"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldNotBuildUrlWithoutValue(String value) {
        assertNull(IdentifierUtil.buildIdentifierUrl("https://orcid.org/{ID}", value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://orcid.org/{ID}", "https://example.org/a?id={ID}"})
    void shouldRecogniseResolvableTemplate(String urlTemplate) {
        assertTrue(IdentifierUtil.isResolvableUrlTemplate(urlTemplate));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"https://orcid.org/", "urn:issn:"})
    void shouldRecogniseUnresolvableTemplate(String urlTemplate) {
        assertFalse(IdentifierUtil.isResolvableUrlTemplate(urlTemplate));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldAcceptMissingTemplateAsNotConfigured(String urlTemplate) {
        IdentifierUtil.validateUrlTemplate(urlTemplate);
    }

    @Test
    void shouldRejectTemplateWithoutPlaceholder() {
        var exception = assertThrows(IdentifierException.class,
            () -> IdentifierUtil.validateUrlTemplate("https://orcid.org/"));

        assertTrue(exception.getMessage().contains("{ID}"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "orcid.org/{ID}",
        "ftp://orcid.org/{ID}",
        "urn:issn:{ID}",
        "https://orcid.org/{ID} extra"
    })
    void shouldRejectTemplateThatDoesNotResolveToHttpUrl(String urlTemplate) {
        assertThrows(IdentifierException.class,
            () -> IdentifierUtil.validateUrlTemplate(urlTemplate));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://orcid.org/{ID}",
        "HTTP://orcid.org/{ID}",
        "https://www.scopus.com/authid/detail.uri?authorId={ID}",
        "https://example.org/{ID}/publications"
    })
    void shouldAcceptResolvableHttpTemplate(String urlTemplate) {
        IdentifierUtil.validateUrlTemplate(urlTemplate);
    }
}
