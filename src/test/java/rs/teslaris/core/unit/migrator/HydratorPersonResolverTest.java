package rs.teslaris.core.unit.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.migrator.converter.hydrator.HydratorPersonResolver;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class HydratorPersonResolverTest {

    @Mock
    private PersonService personService;

    @InjectMocks
    private HydratorPersonResolver resolver;


    private static Optional<Person> person(int id) {
        var person = new Person();
        person.setId(id);
        return Optional.of(person);
    }

    @Test
    void shouldMatchByCienciaIdFirst() {
        when(personService.findPersonByIdentifier("AAAA-BBBB-CCCC")).thenReturn(person(9));

        var match = resolver.resolve("AAAA-BBBB-CCCC", "0000-0002-1825-0097");

        assertEquals(9, match.personId());
        verify(personService, never()).findPersonByIdentifier("0000-0002-1825-0097");
    }

    @Test
    void shouldFallBackToOrcid() {
        when(personService.findPersonByIdentifier(any())).thenReturn(Optional.empty());
        when(personService.findPersonByIdentifier("0000-0002-1825-0097")).thenReturn(person(5));

        assertEquals(5, resolver.resolve("AAAA-BBBB-CCCC", "0000-0002-1825-0097").personId());
    }

    @Test
    void shouldLookUpEachIdentifierOnce() {
        when(personService.findPersonByIdentifier(any())).thenReturn(Optional.empty());

        resolver.resolve("AAAA-BBBB-CCCC", null);
        resolver.resolve("AAAA-BBBB-CCCC", null);

        verify(personService, times(1)).findPersonByIdentifier("AAAA-BBBB-CCCC");
    }

    @Test
    void shouldReportAmbiguousIdentifier() {
        when(personService.findPersonByIdentifier("AAAA-BBBB-CCCC"))
            .thenThrow(new IncorrectResultSizeDataAccessException(1, 2));

        var match = resolver.resolve("AAAA-BBBB-CCCC", null);

        assertFalse(match.found());
        assertEquals("ambiguous identifier", match.reason());
    }

    @Test
    void shouldReportMissingIdentifier() {
        assertEquals("person has no identifier", resolver.resolve(null, " ").reason());
    }
}
