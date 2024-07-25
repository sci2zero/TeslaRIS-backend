package rs.teslaris.core.unit;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import rs.teslaris.core.exporter.model.common.ExportEvent;
import rs.teslaris.core.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.core.exporter.model.common.ExportPerson;
import rs.teslaris.core.exporter.service.impl.CommonExportServiceImpl;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.repository.document.ConferenceRepository;
import rs.teslaris.core.repository.person.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.PersonRepository;

@SpringBootTest
public class CommonExportServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private OrganisationUnitRepository organisationUnitRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private ConferenceRepository conferenceRepository;

    @InjectMocks
    private CommonExportServiceImpl commonExportService;


    @Test
    void shouldExportOrganisationUnits() {
        // Given
        var orgUnit = new OrganisationUnit();
        orgUnit.setId(1);

        var page = new PageImpl<>(List.of(orgUnit));
        when(organisationUnitRepository.findAllModifiedInLast24Hours(any(PageRequest.class)))
            .thenReturn(page);

        // When
        commonExportService.exportOrganisationUnitsToCommonModel();

        // Then
        verify(mongoTemplate, times(1)).remove(any(Query.class), eq(ExportOrganisationUnit.class));
        verify(mongoTemplate, times(1)).save(any(ExportOrganisationUnit.class));
    }

    @Test
    void shouldExportPersons() {
        // Given
        var person = new Person();
        person.setId(1);
        person.setName(new PersonName());
        var personalInfo = new PersonalInfo();
        personalInfo.setContact(new Contact());
        person.setPersonalInfo(personalInfo);

        var page = new PageImpl<>(List.of(person));
        when(personRepository.findAllModifiedInLast24Hours(any(PageRequest.class)))
            .thenReturn(page);

        // When
        commonExportService.exportPersonsToCommonModel();

        // Then
        verify(mongoTemplate, times(1)).remove(any(Query.class), eq(ExportPerson.class));
        verify(mongoTemplate, times(1)).save(any(ExportPerson.class));
    }

    @Test
    void shouldExportConferences() {
        // Given
        var conference = new Conference();
        conference.setId(1);

        var page = new PageImpl<>(List.of(conference));
        when(conferenceRepository.findAllModifiedInLast24Hours(any(PageRequest.class)))
            .thenReturn(page);

        // When
        commonExportService.exportConferencesToCommonModel();

        // Then
        verify(mongoTemplate, times(1)).remove(any(Query.class), eq(ExportEvent.class));
        verify(mongoTemplate, times(1)).save(any(ExportEvent.class));
    }
}
