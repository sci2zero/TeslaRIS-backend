package rs.teslaris.importer.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.teslaris.core.model.oaipmh.person.Person;
import rs.teslaris.core.model.oaipmh.person.PersonName;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.importer.model.converter.load.commontypes.MultilingualContentConverter;
import rs.teslaris.importer.model.converter.load.person.ImportPersonConverter;

@ExtendWith(MockitoExtension.class)
public class ImportPersonConverterTest {

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private MultilingualContentConverter multilingualContentConverter;

    @InjectMocks
    private ImportPersonConverter importPersonConverter;


    private static Person harvestedPerson(String apvnt) {
        var personName = new PersonName();
        personName.setFirstNames("Jane");
        personName.setFamilyNames("Smith");

        var person = new Person();
        person.setOldId("(BISIS)123");
        person.setPersonName(personName);
        person.setApvnt(apvnt);

        return person;
    }

    @Test
    void shouldCarryApvntUnderConfiguredIdentifierCode() {
        var dto = importPersonConverter.toDTO(harvestedPerson("12345"));

        assertEquals(Map.of("apvnt", "12345"), dto.getAdditionalIdentifiers());
    }

    @Test
    void shouldTrimHarvestedApvntValue() {
        var dto = importPersonConverter.toDTO(harvestedPerson("  12345  "));

        assertEquals("12345", dto.getAdditionalIdentifiers().get("apvnt"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldNotCarryBlankApvnt(String apvnt) {
        var dto = importPersonConverter.toDTO(harvestedPerson(apvnt));

        assertTrue(dto.getAdditionalIdentifiers().isEmpty());
    }
}
