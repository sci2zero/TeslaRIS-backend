package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.Identifier;
import rs.teslaris.core.model.identifier.PersonIdentifier;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.identifier.PersonIdentifierRepository;
import rs.teslaris.core.util.identifier.PublicPersonIdentifiers;

@ExtendWith(MockitoExtension.class)
public class PublicPersonIdentifiersTest {

    @Mock
    private PersonIdentifierRepository personIdentifierRepository;

    private static PersonIdentifier personIdentifier(Integer personId, String code, String value,
                                                     String urlTemplate) {
        var identifier = new Identifier();
        identifier.setCode(code);
        identifier.setUrlTemplate(urlTemplate);

        var person = new Person();
        person.setId(personId);

        var personIdentifier = new PersonIdentifier();
        personIdentifier.setIdentifier(identifier);
        personIdentifier.setValue(value);
        personIdentifier.setPerson(person);

        return personIdentifier;
    }

    @BeforeEach
    void setUp() {
        new PublicPersonIdentifiers(personIdentifierRepository);
    }

    @AfterEach
    void tearDown() {
        PublicPersonIdentifiers.clear();
    }

    @Test
    void shouldOnlyEverRequestOpenIdentifiers() {
        when(personIdentifierRepository.findIdentifiersForPersonAndIdentifierAccessLevel(eq(1),
            any())).thenReturn(List.of());

        PublicPersonIdentifiers.openIdentifiers(1);

        verify(personIdentifierRepository).findIdentifiersForPersonAndIdentifierAccessLevel(1,
            AccessLevel.OPEN);
    }

    @Test
    void shouldSkipIdentifiersWithoutUrlTemplate() {
        when(personIdentifierRepository.findIdentifiersForPersonAndIdentifierAccessLevel(1,
            AccessLevel.OPEN)).thenReturn(List.of(
            personIdentifier(1, "apvnt", "12345", null),
            personIdentifier(1, "ecris", "67890",
                "https://bib.cobiss.net/biblioweb/biblio/sr/scr/cris/{ID}")));

        var uris = PublicPersonIdentifiers.resolvableUris(1);

        assertEquals(List.of("https://bib.cobiss.net/biblioweb/biblio/sr/scr/cris/67890"), uris);
    }

    @Test
    void shouldReturnNullFirstUriWhenNoIdentifierResolves() {
        when(personIdentifierRepository.findIdentifiersForPersonAndIdentifierAccessLevel(1,
            AccessLevel.OPEN)).thenReturn(
            List.of(personIdentifier(1, "apvnt", "12345", null)));

        assertNull(PublicPersonIdentifiers.firstResolvableUri(1));
    }

    @Test
    void shouldSubstitutePlaceholderInsideQueryParameterTemplate() {
        when(personIdentifierRepository.findIdentifiersForPersonAndIdentifierAccessLevel(1,
            AccessLevel.OPEN)).thenReturn(List.of(
            personIdentifier(1, "scopus", "12345",
                "https://www.scopus.com/authid/detail.uri?authorId={ID}")));

        assertEquals(List.of("https://www.scopus.com/authid/detail.uri?authorId=12345"),
            PublicPersonIdentifiers.resolvableUris(1));
    }

    @Test
    void shouldSubstitutePlaceholderInTheMiddleOfTemplate() {
        when(personIdentifierRepository.findIdentifiersForPersonAndIdentifierAccessLevel(1,
            AccessLevel.OPEN)).thenReturn(List.of(
            personIdentifier(1, "portal", "A123", "https://portal.test/{ID}/publications")));

        assertEquals(List.of("https://portal.test/A123/publications"),
            PublicPersonIdentifiers.resolvableUris(1));
    }

    @Test
    void shouldPreserveRepositoryOrderingOfIdentifiers() {
        when(personIdentifierRepository.findIdentifiersForPersonAndIdentifierAccessLevel(1,
            AccessLevel.OPEN)).thenReturn(List.of(
            personIdentifier(1, "aaa", "1", "https://aaa.test/{ID}"),
            personIdentifier(1, "bbb", "2", "https://bbb.test/{ID}")));

        assertEquals("https://aaa.test/1", PublicPersonIdentifiers.firstResolvableUri(1));
    }

    @Test
    void shouldServePrimedPersonWithoutQueryingAgain() {
        when(personIdentifierRepository.findIdentifiersForPersonsAndIdentifierAccessLevel(
            Set.of(1, 2), AccessLevel.OPEN)).thenReturn(
            List.of(personIdentifier(1, "ecris", "67890", "https://cris.test/{ID}")));

        PublicPersonIdentifiers.prime(Set.of(1, 2));

        assertEquals(List.of("https://cris.test/67890"),
            PublicPersonIdentifiers.resolvableUris(1));
        assertTrue(PublicPersonIdentifiers.resolvableUris(2).isEmpty());

        verify(personIdentifierRepository, never())
            .findIdentifiersForPersonAndIdentifierAccessLevel(any(), any());
    }

    @Test
    void shouldFallBackToQueryForPersonThePrimedBatchDoesNotCover() {
        when(personIdentifierRepository.findIdentifiersForPersonsAndIdentifierAccessLevel(
            Set.of(1), AccessLevel.OPEN)).thenReturn(List.of());
        when(personIdentifierRepository.findIdentifiersForPersonAndIdentifierAccessLevel(2,
            AccessLevel.OPEN)).thenReturn(
            List.of(personIdentifier(2, "ecris", "11111", "https://cris.test/{ID}")));

        PublicPersonIdentifiers.prime(Set.of(1));

        assertEquals(List.of("https://cris.test/11111"),
            PublicPersonIdentifiers.resolvableUris(2));
    }
}
