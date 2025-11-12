package rs.teslaris.core.unit;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.repository.document.ConferenceRepository;
import rs.teslaris.core.repository.document.DatasetRepository;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.repository.document.MonographPublicationRepository;
import rs.teslaris.core.repository.document.MonographRepository;
import rs.teslaris.core.repository.document.PatentRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.repository.document.ProceedingsRepository;
import rs.teslaris.core.repository.document.SoftwareRepository;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.repository.institution.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.exporter.model.common.ExportEvent;
import rs.teslaris.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.common.ExportPublicationType;
import rs.teslaris.exporter.service.impl.CommonExportServiceImpl;

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

    @Mock
    private DatasetRepository datasetRepository;

    @Mock
    private SoftwareRepository softwareRepository;

    @Mock
    private PatentRepository patentRepository;

    @Mock
    private JournalRepository journalRepository;

    @Mock
    private JournalPublicationRepository journalPublicationRepository;

    @Mock
    private MonographRepository monographRepository;

    @Mock
    private MonographPublicationRepository monographPublicationRepository;

    @Mock
    private ProceedingsRepository proceedingsRepository;

    @Mock
    private ProceedingsPublicationRepository proceedingsPublicationRepository;

    @Mock
    private ThesisRepository thesisRepository;

    @InjectMocks
    private CommonExportServiceImpl commonExportService;


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldExportOrganisationUnits(boolean allTime) {
        // Given
        var orgUnit = new OrganisationUnit();
        orgUnit.setId(1);

        var page = new PageImpl<>(List.of(orgUnit));
        when(organisationUnitRepository.findAllModified(any(PageRequest.class),
            anyBoolean()))
            .thenReturn(page);

        // When
        commonExportService.exportOrganisationUnitsToCommonModel(allTime);

        // Then
        verify(mongoTemplate, times(1)).remove(any(Query.class), eq(ExportOrganisationUnit.class));
        verify(mongoTemplate, times(1)).save(any(ExportOrganisationUnit.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldExportPersons(boolean allTime) {
        // Given
        var person = new Person();
        person.setId(1);
        person.setName(new PersonName());
        var personalInfo = new PersonalInfo();
        personalInfo.setContact(new Contact());
        person.setPersonalInfo(personalInfo);

        var page = new PageImpl<>(List.of(person));
        when(personRepository.findAllModified(any(PageRequest.class), anyBoolean()))
            .thenReturn(page);

        // When
        commonExportService.exportPersonsToCommonModel(allTime);

        // Then
        verify(mongoTemplate, times(1)).remove(any(Query.class), eq(ExportPerson.class));
        verify(mongoTemplate, times(1)).save(any(ExportPerson.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldExportConferences(boolean allTime) {
        // Given
        var conference = new Conference();
        conference.setId(1);

        var page = new PageImpl<>(List.of(conference));
        when(
            conferenceRepository.findAllModified(any(PageRequest.class), anyBoolean()))
            .thenReturn(page);

        // When
        commonExportService.exportConferencesToCommonModel(allTime);

        // Then
        verify(mongoTemplate, times(1)).remove(any(Query.class), eq(ExportEvent.class));
        verify(mongoTemplate, times(1)).save(any(ExportEvent.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldExportDocumentsToCommonModel(boolean allTime) {
        // Given
        var emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(datasetRepository.findAllModified(any(Pageable.class),
            anyBoolean())).thenReturn(
            (Page) emptyPage);
        when(softwareRepository.findAllModified(any(Pageable.class),
            anyBoolean())).thenReturn(
            (Page) emptyPage);
        when(patentRepository.findAllModified(any(Pageable.class),
            anyBoolean())).thenReturn(
            (Page) emptyPage);
        when(journalRepository.findAllModified(
            any(Pageable.class), anyBoolean())).thenReturn((Page) emptyPage);
        when(journalPublicationRepository.findAllModified(
            any(Pageable.class), anyBoolean())).thenReturn((Page) emptyPage);
        when(proceedingsRepository.findAllModified(any(Pageable.class),
            anyBoolean())).thenReturn(
            (Page) emptyPage);
        when(proceedingsPublicationRepository.findAllModified(
            any(Pageable.class), anyBoolean())).thenReturn((Page) emptyPage);
        when(monographRepository.findAllModified(any(Pageable.class),
            anyBoolean())).thenReturn(
            (Page) emptyPage);
        when(monographPublicationRepository.findAllModified(
            any(Pageable.class), anyBoolean())).thenReturn((Page) emptyPage);
        when(thesisRepository.findAllModified(any(Pageable.class),
            anyBoolean())).thenReturn(
            (Page) emptyPage);

        // When
        commonExportService.exportDocumentsToCommonModel(allTime, Collections.emptyList());

        // Then
        verify(datasetRepository, times(1)).findAllModified(any(Pageable.class),
            anyBoolean());
        verify(softwareRepository, times(1)).findAllModified(any(Pageable.class),
            anyBoolean());
        verify(patentRepository, times(1)).findAllModified(any(Pageable.class),
            anyBoolean());
        verify(journalRepository, times(1)).findAllModified(any(Pageable.class),
            anyBoolean());
        verify(journalPublicationRepository, times(1)).findAllModified(
            any(Pageable.class), anyBoolean());
        verify(proceedingsRepository, times(1)).findAllModified(any(Pageable.class),
            anyBoolean());
        verify(proceedingsPublicationRepository, times(1)).findAllModified(
            any(Pageable.class), anyBoolean());
        verify(monographRepository, times(1)).findAllModified(any(Pageable.class),
            anyBoolean());
        verify(monographPublicationRepository, times(1)).findAllModified(
            any(Pageable.class), anyBoolean());
    }

    @ParameterizedTest
    @EnumSource(ExportPublicationType.class)
    public void shouldExportDocumentsToCommonModel(ExportPublicationType exportPublicationType) {
        // Given
        var emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(datasetRepository.findAllModified(any(Pageable.class),
            anyBoolean())).thenReturn(
            (Page) emptyPage);
        when(softwareRepository.findAllModified(any(Pageable.class),
            anyBoolean())).thenReturn(
            (Page) emptyPage);
        when(patentRepository.findAllModified(any(Pageable.class),
            anyBoolean())).thenReturn(
            (Page) emptyPage);
        when(journalRepository.findAllModified(
            any(Pageable.class), anyBoolean())).thenReturn((Page) emptyPage);
        when(journalPublicationRepository.findAllModified(
            any(Pageable.class), anyBoolean())).thenReturn((Page) emptyPage);
        when(proceedingsRepository.findAllModified(any(Pageable.class),
            anyBoolean())).thenReturn(
            (Page) emptyPage);
        when(proceedingsPublicationRepository.findAllModified(
            any(Pageable.class), anyBoolean())).thenReturn((Page) emptyPage);
        when(monographRepository.findAllModified(any(Pageable.class),
            anyBoolean())).thenReturn(
            (Page) emptyPage);
        when(monographPublicationRepository.findAllModified(
            any(Pageable.class), anyBoolean())).thenReturn((Page) emptyPage);
        when(thesisRepository.findAllModified(any(Pageable.class),
            anyBoolean())).thenReturn(
            (Page) emptyPage);

        // When
        commonExportService.exportDocumentsToCommonModel(true, List.of(exportPublicationType));

        // Then
        switch (exportPublicationType) {
            case JOURNAL_PUBLICATION ->
                verify(journalPublicationRepository, times(1)).findAllModified(any(Pageable.class),
                    anyBoolean());
            case PROCEEDINGS ->
                verify(proceedingsRepository, times(1)).findAllModified(any(Pageable.class),
                    anyBoolean());
            case PROCEEDINGS_PUBLICATION ->
                verify(proceedingsPublicationRepository, times(1)).findAllModified(
                    any(Pageable.class), anyBoolean());
            case MONOGRAPH ->
                verify(monographRepository, times(1)).findAllModified(any(Pageable.class),
                    anyBoolean());
            case PATENT -> verify(patentRepository, times(1)).findAllModified(any(Pageable.class),
                anyBoolean());
            case SOFTWARE ->
                verify(softwareRepository, times(1)).findAllModified(any(Pageable.class),
                    anyBoolean());
            case DATASET -> verify(datasetRepository, times(1)).findAllModified(any(Pageable.class),
                anyBoolean());
            case JOURNAL -> verify(journalRepository, times(1)).findAllModified(any(Pageable.class),
                anyBoolean());
            case MONOGRAPH_PUBLICATION ->
                verify(monographPublicationRepository, times(1)).findAllModified(
                    any(Pageable.class), anyBoolean());
            case THESIS -> verify(thesisRepository, times(1)).findAllModified(any(Pageable.class),
                anyBoolean());
        }
    }
}
