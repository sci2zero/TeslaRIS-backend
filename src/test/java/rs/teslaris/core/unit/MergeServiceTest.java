package rs.teslaris.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import rs.teslaris.core.applicationevent.OrganisationUnitSignificantChangeEvent;
import rs.teslaris.core.applicationevent.PersonEmploymentOUHierarchyStructureChangedEvent;
import rs.teslaris.core.dto.document.BookSeriesDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.dto.document.GeneticMaterialDTO;
import rs.teslaris.core.dto.document.IntangibleProductDTO;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.MaterialProductDTO;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.document.BookSeries;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.IntangibleProduct;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.MaterialProduct;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.Publisher;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.ExpertiseOrSkill;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.Prize;
import rs.teslaris.core.model.user.Authority;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.document.DatasetRepository;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.IntangibleProductRepository;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.repository.document.MonographRepository;
import rs.teslaris.core.repository.document.PatentRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.repository.document.ProceedingsRepository;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.service.impl.comparator.MergeServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DatasetService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.GeneticMaterialService;
import rs.teslaris.core.service.interfaces.document.IntangibleProductService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.MaterialProductService;
import rs.teslaris.core.service.interfaces.document.MonographPublicationService;
import rs.teslaris.core.service.interfaces.document.MonographService;
import rs.teslaris.core.service.interfaces.document.PatentService;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.ExpertiseOrSkillService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.person.PrizeService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.deduplication.Mergeable;

@SpringBootTest
public class MergeServiceTest {

    @Mock
    private JournalService journalService;

    @Mock
    private JournalPublicationService journalPublicationService;

    @Mock
    private JournalPublicationRepository journalPublicationRepository;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private DocumentPublicationService documentPublicationService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private PersonService personService;

    @Mock
    private UserService userService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private ConferenceService conferenceService;

    @Mock
    private ProceedingsService proceedingsService;

    @Mock
    private ProceedingsPublicationService proceedingsPublicationService;

    @Mock
    private ProceedingsPublicationRepository proceedingsPublicationRepository;

    @Mock
    private PrizeService prizeService;

    @Mock
    private ExpertiseOrSkillService expertiseOrSkillService;

    @Mock
    private InvolvementService involvementService;

    @Mock
    private IntangibleProductService intangibleProductService;

    @Mock
    private DatasetService datasetService;

    @Mock
    private PatentService patentService;

    @Mock
    private ThesisService thesisService;

    @Mock
    private MonographService monographService;

    @Mock
    private MonographPublicationService monographPublicationService;

    @Mock
    private ProceedingsRepository proceedingsRepository;

    @Mock
    private MonographRepository monographRepository;

    @Mock
    private BookSeriesService bookSeriesService;

    @Mock
    private PublisherService publisherService;

    @Mock
    private IndexBulkUpdateService indexBulkUpdateService;

    @Mock
    private IntangibleProductRepository intangibleProductRepository;

    @Mock
    private DatasetRepository datasetRepository;

    @Mock
    private PatentRepository patentRepository;

    @Mock
    private ThesisRepository thesisRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private MaterialProductService materialProductService;

    @Mock
    private GeneticMaterialService geneticMaterialService;

    @InjectMocks
    private MergeServiceImpl mergeService;

    private static Stream<Arguments> provideDocumentPublicationsForMigration() {
        return Stream.of(
            Arguments.of(
                new IntangibleProduct() {{
                    getMergedIds().add(103);
                    getOldIds().add(203);
                }},
                new IntangibleProduct(),
                IntangibleProduct.class,
                EntityType.PUBLICATION
            ),
            Arguments.of(
                new MaterialProduct() {{
                    getMergedIds().add(103);
                    getOldIds().add(203);
                }},
                new MaterialProduct(),
                MaterialProduct.class,
                EntityType.PUBLICATION
            ),
            Arguments.of(
                new Patent() {{
                    getMergedIds().add(103);
                    getOldIds().add(203);
                }},
                new Patent(),
                Patent.class,
                EntityType.PUBLICATION
            ),
            Arguments.of(
                new Dataset() {{
                    getMergedIds().add(103);
                    getOldIds().add(203);
                }},
                new Dataset(),
                Dataset.class,
                EntityType.PUBLICATION
            ),
            Arguments.of(
                new OrganisationUnit() {{
                    getMergedIds().add(103);
                    getOldIds().add(203);
                }},
                new OrganisationUnit(),
                OrganisationUnit.class,
                EntityType.ORGANISATION_UNIT
            ),
            Arguments.of(
                new Person() {{
                    getMergedIds().add(103);
                    getOldIds().add(203);
                }},
                new Person(),
                Person.class,
                EntityType.PERSON
            ),
            Arguments.of(
                new Journal() {{
                    getMergedIds().add(103);
                    getOldIds().add(203);
                }},
                new Journal(),
                Journal.class,
                EntityType.JOURNAL
            ),
            Arguments.of(
                new Publisher() {{
                    getMergedIds().add(103);
                    getOldIds().add(203);
                }},
                new Publisher(),
                Publisher.class,
                EntityType.PUBLISHER
            )
        );
    }

    @Test
    void switchJournalPublicationToOtherJournal_shouldPerformSwitch() {
        var targetJournalId = 1;
        var publicationId = 2;

        var publication = new JournalPublication();
        when(journalPublicationRepository.findById(publicationId)).thenReturn(
            Optional.of(publication));
        var targetJournal = new Journal();
        when(journalService.findJournalById(targetJournalId)).thenReturn(targetJournal);

        mergeService.switchJournalPublicationToOtherJournal(targetJournalId, publicationId);

        verify(journalPublicationRepository).findById(publicationId);
        verify(journalService).findJournalById(targetJournalId);
        verify(journalPublicationRepository).save(publication);
        assertEquals(targetJournal, publication.getJournal());
    }

    @Test
    void switchAllPublicationsToOtherJournal_shouldPerformSwitchForAll() {
        var sourceId = 1;
        var targetId = 2;

        var publicationIndex1 = new DocumentPublicationIndex();
        publicationIndex1.setDatabaseId(1);
        var publicationIndex2 = new DocumentPublicationIndex();
        publicationIndex2.setDatabaseId(2);
        var page1 = new PageImpl<>(
            List.of(publicationIndex1, publicationIndex2));
        var page2 = new PageImpl<DocumentPublicationIndex>(List.of());

        when(documentPublicationIndexRepository.findByTypeAndJournalId(
            DocumentPublicationType.JOURNAL_PUBLICATION.name(), sourceId, PageRequest.of(0, 100)))
            .thenReturn(page1);
        when(documentPublicationIndexRepository.findByTypeAndJournalId(
            DocumentPublicationType.JOURNAL_PUBLICATION.name(), sourceId, PageRequest.of(1, 100)))
            .thenReturn(page2);

        var publication1 = new JournalPublication();
        var publication2 = new JournalPublication();
        when(journalPublicationRepository.findById(publicationIndex1.getDatabaseId())).thenReturn(
            Optional.of(publication1));
        when(journalPublicationRepository.findById(publicationIndex2.getDatabaseId())).thenReturn(
            Optional.of(publication2));

        Journal targetJournal = new Journal();
        when(journalService.findJournalById(targetId)).thenReturn(targetJournal);

        mergeService.switchAllPublicationsToOtherJournal(sourceId, targetId);

        verify(journalPublicationRepository, times(2)).save(any(JournalPublication.class));
        verify(documentPublicationIndexRepository, times(1)).findByTypeAndJournalId(
            eq(DocumentPublicationType.JOURNAL_PUBLICATION.name()), eq(sourceId),
            any(PageRequest.class));
        assertEquals(targetJournal, publication1.getJournal());
        assertEquals(targetJournal, publication2.getJournal());
    }

    @Test
    void switchPublicationToOtherBookSeries_shouldPerformSwitch() {
        var targetBookSeriesId = 1;
        var publicationId = 2;

        var publication = new Monograph();
        when(monographRepository.findById(publicationId)).thenReturn(
            Optional.of(publication));
        var targetBookSeries = new BookSeries();
        when(bookSeriesService.findBookSeriesById(targetBookSeriesId)).thenReturn(targetBookSeries);

        mergeService.switchPublicationToOtherBookSeries(targetBookSeriesId, publicationId);

        verify(monographRepository).findById(publicationId);
        verify(bookSeriesService).findBookSeriesById(targetBookSeriesId);
        verify(monographRepository).save(publication);
        assertEquals(targetBookSeries, publication.getPublicationSeries());
    }

    @Test
    void switchAllPublicationsToOtherBookSeries_shouldPerformSwitchForAll() {
        var sourceId = 1;
        var targetId = 2;

        var publicationIndex1 = new DocumentPublicationIndex();
        publicationIndex1.setDatabaseId(1);
        var publicationIndex2 = new DocumentPublicationIndex();
        publicationIndex2.setDatabaseId(2);
        var page1 = new PageImpl<>(
            List.of(publicationIndex1, publicationIndex2));
        var page2 = new PageImpl<DocumentPublicationIndex>(List.of());

        when(documentPublicationIndexRepository.findByTypeInAndPublicationSeriesId(
            List.of(DocumentPublicationType.MONOGRAPH.name(),
                DocumentPublicationType.PROCEEDINGS.name()), sourceId,
            PageRequest.of(0, 100))).thenReturn(page1);
        when(documentPublicationIndexRepository.findByTypeInAndPublicationSeriesId(
            List.of(DocumentPublicationType.MONOGRAPH.name(),
                DocumentPublicationType.PROCEEDINGS.name()), sourceId,
            PageRequest.of(1, 100))).thenReturn(page2);

        var publication1 = new Monograph();
        var publication2 = new Proceedings();
        when(monographRepository.findById(publicationIndex1.getDatabaseId())).thenReturn(
            Optional.of(publication1));
        when(proceedingsRepository.findById(publicationIndex2.getDatabaseId())).thenReturn(
            Optional.of(publication2));

        var targetBookSeries = new BookSeries();
        when(bookSeriesService.findBookSeriesById(targetId)).thenReturn(targetBookSeries);

        mergeService.switchAllPublicationsToOtherBookSeries(sourceId, targetId);

        assertEquals(targetBookSeries, publication1.getPublicationSeries());
        assertEquals(targetBookSeries, publication2.getPublicationSeries());
    }

    @Test
    void switchPublicationToOtherPerson_shouldPerformSwitch() {
        // Given
        var sourcePersonId = 1;
        var targetPersonId = 2;
        var publicationId = 2;

        var publication = new JournalPublication();
        var contribution = new PersonDocumentContribution();
        var affiliationStatement = new AffiliationStatement();
        affiliationStatement.setDisplayPersonName(new PersonName());
        contribution.setAffiliationStatement(affiliationStatement);
        var contributor = new Person();
        contributor.setName(new PersonName());
        contributor.setId(sourcePersonId);
        contribution.setPerson(contributor);

        publication.getContributors().add(contribution);
        when(documentPublicationService.findDocumentById(publicationId)).thenReturn(publication);

        var otherPerson = new Person();
        otherPerson.setName(new PersonName());
        otherPerson.setId(targetPersonId);
        when(personService.findOne(targetPersonId)).thenReturn(otherPerson);

        // When
        mergeService.switchPublicationToOtherPerson(sourcePersonId, targetPersonId, publicationId);

        // Then
        assertEquals(contribution.getPerson().getId(), targetPersonId);
        verify(documentRepository).save(publication);
    }

    @Test
    void switchAllPublicationsToOtherPerson_shouldPerformSwitchForAll() {
        var sourceId = 1;
        var targetId = 2;

        var publicationIndex1 = new DocumentPublicationIndex();
        publicationIndex1.setDatabaseId(1);
        var publicationIndex2 = new DocumentPublicationIndex();
        publicationIndex2.setDatabaseId(2);
        var page1 = new PageImpl<>(
            List.of(publicationIndex1, publicationIndex2));
        var page2 = new PageImpl<DocumentPublicationIndex>(List.of());

        when(
            documentPublicationService.findResearcherPublications(sourceId, List.of(), List.of("*"),
                Arrays.asList(DocumentPublicationType.values()), DocumentContributionType.AUTHOR,
                PageRequest.of(0, 100))).thenReturn(
            page1);
        when(
            documentPublicationService.findResearcherPublications(sourceId, List.of(), List.of("*"),
                Arrays.asList(DocumentPublicationType.values()), DocumentContributionType.AUTHOR,
                PageRequest.of(1, 100))).thenReturn(
            page2);

        var contribution = new PersonDocumentContribution();
        var affiliationStatement = new AffiliationStatement();
        affiliationStatement.setDisplayPersonName(new PersonName());
        contribution.setAffiliationStatement(affiliationStatement);
        var contributor = new Person();
        contributor.setName(new PersonName());
        contributor.setId(sourceId);
        contribution.setPerson(contributor);

        var publication1 = new JournalPublication();
        publication1.addDocumentContribution(contribution);
        var publication2 = new JournalPublication();
        publication2.addDocumentContribution(contribution);

        when(documentPublicationService.findDocumentById(
            publicationIndex1.getDatabaseId())).thenReturn(publication1);
        when(documentPublicationService.findDocumentById(
            publicationIndex2.getDatabaseId())).thenReturn(publication2);

        var otherPerson = new Person();
        otherPerson.setName(new PersonName());
        otherPerson.setId(targetId);
        when(personService.findOne(targetId)).thenReturn(otherPerson);

        mergeService.switchAllPublicationToOtherPerson(sourceId, targetId,
            DocumentContributionType.AUTHOR);

        verify(documentRepository, times(1)).save(any(JournalPublication.class));
        assertEquals(contribution.getPerson().getId(), targetId);
    }

    @Test
    public void switchPersonToOtherOUTest() {
        // given
        var sourceOUId = 1;
        var targetOUId = 2;
        var personId = 3;
        var person = new Person();
        when(personService.findOne(personId)).thenReturn(person);

        // when
        mergeService.switchPersonToOtherOU(sourceOUId, targetOUId, personId);

        // then
        verify(personService).findOne(personId);
        verify(userService).updateResearcherCurrentOrganisationUnitIfBound(personId);
        verify(personService).reindexPersonEmploymentDetails(person);
        verify(applicationEventPublisher).publishEvent(any(
            PersonEmploymentOUHierarchyStructureChangedEvent.class));
    }

    @Test
    public void switchAllPersonsToOtherOUTest() {
        // given
        var sourceOUId = 1;
        var targetOUId = 2;
        var searchTokens = List.of("*");
        var person = new Person();
        var employment = new Employment();
        employment.setInvolvementType(InvolvementType.EMPLOYED_AT);
        var organisationUnit = new OrganisationUnit();
        organisationUnit.setId(1);
        employment.setOrganisationUnit(organisationUnit);
        person.setInvolvements(Set.of(employment));
        var personIndex = new PersonIndex();
        var pageRequest = PageRequest.of(0, 100);
        var page = new PageImpl<>(List.of(personIndex));
        when(
            personService.findPeopleForOrganisationUnit(sourceOUId, searchTokens, pageRequest,
                false)).thenReturn(
            page);
        when(personService.findOne(any())).thenReturn(person);

        // when
        mergeService.switchAllPersonsToOtherOU(sourceOUId, targetOUId);

        // then
        verify(personService, atLeastOnce()).findPeopleForOrganisationUnit(eq(sourceOUId),
            eq(searchTokens), any(PageRequest.class), eq(false));
        verify(applicationEventPublisher).publishEvent(
            any(OrganisationUnitSignificantChangeEvent.class));
        verify(personService, never()).reindexPersonEmploymentDetails(person);
    }

    @Test
    public void switchProceedingsToOtherConferenceTest() {
        // given
        var targetConferenceId = 1;
        var proceedingsId = 2;
        var targetConference = new Conference();
        targetConference.setSerialEvent(false);
        var proceedings = new Proceedings();
        when(conferenceService.findConferenceById(targetConferenceId)).thenReturn(targetConference);
        when(proceedingsService.findProceedingsById(proceedingsId)).thenReturn(proceedings);
        var index = new DocumentPublicationIndex();
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            proceedingsId)).thenReturn(java.util.Optional.of(index));

        // when
        mergeService.switchProceedingsToOtherConference(targetConferenceId, proceedingsId);

        // then
        verify(conferenceService).findConferenceById(targetConferenceId);
        verify(proceedingsService).findProceedingsById(proceedingsId);
        verify(proceedingsService).indexProceedings(proceedings, index);
    }

    @Test
    public void switchAllProceedingsToOtherConferenceTest() {
        // given
        var sourceConferenceId = 1;
        var sourceConference = new Conference();
        sourceConference.setId(sourceConferenceId);
        var targetConferenceId = 2;
        var targetConference = new Conference();
        targetConference.setId(targetConferenceId);
        targetConference.setSerialEvent(false);
        var proceedings = new ProceedingsResponseDTO();
        when(proceedingsService.readProceedingsForEventId(sourceConferenceId)).thenReturn(
            List.of(proceedings));
        when(proceedingsService.findProceedingsById(any())).thenReturn(new Proceedings());
        when(conferenceService.findConferenceById(1)).thenReturn(sourceConference);
        when(conferenceService.findConferenceById(2)).thenReturn(targetConference);

        // when
        mergeService.switchAllProceedingsToOtherConference(sourceConferenceId, targetConferenceId);

        // then
        verify(proceedingsService, atLeastOnce()).readProceedingsForEventId(sourceConferenceId);
        verify(proceedingsService, atLeastOnce()).indexProceedings(any(), any());
    }

    @Test
    void switchProceedingsPublicationToOtherProceedings_shouldPerformSwitch() {
        var targetProceedingsId = 1;
        var publicationId = 2;

        var publication = new ProceedingsPublication();
        when(proceedingsPublicationRepository.findById(publicationId)).thenReturn(
            Optional.of(publication));
        var targetProceedings = new Proceedings();
        when(proceedingsService.findProceedingsById(targetProceedingsId)).thenReturn(
            targetProceedings);

        mergeService.switchProceedingsPublicationToOtherProceedings(targetProceedingsId,
            publicationId);

        verify(proceedingsPublicationRepository).findById(publicationId);
        verify(proceedingsService).findProceedingsById(targetProceedingsId);
        verify(proceedingsPublicationRepository).save(publication);
        assertEquals(targetProceedings, publication.getProceedings());
    }

    @Test
    void switchAllPublicationsToOtherProceedings_shouldPerformSwitchForAll() {
        var sourceId = 1;
        var targetId = 2;

        var publicationIndex1 = new DocumentPublicationIndex();
        publicationIndex1.setDatabaseId(1);
        var publicationIndex2 = new DocumentPublicationIndex();
        publicationIndex2.setDatabaseId(2);
        var page1 = new PageImpl<>(
            List.of(publicationIndex1, publicationIndex2));
        var page2 = new PageImpl<DocumentPublicationIndex>(List.of());

        when(documentPublicationIndexRepository.findByTypeAndProceedingsId(
            DocumentPublicationType.PROCEEDINGS_PUBLICATION.name(), sourceId,
            PageRequest.of(0, 100)))
            .thenReturn(page1);
        when(documentPublicationIndexRepository.findByTypeAndJournalId(
            DocumentPublicationType.JOURNAL_PUBLICATION.name(), sourceId, PageRequest.of(1, 100)))
            .thenReturn(page2);

        var publication1 = new ProceedingsPublication();
        var publication2 = new ProceedingsPublication();
        when(proceedingsPublicationRepository.findById(
            publicationIndex1.getDatabaseId())).thenReturn(
            Optional.of(publication1));
        when(proceedingsPublicationRepository.findById(
            publicationIndex2.getDatabaseId())).thenReturn(
            Optional.of(publication2));

        var targetProceedings = new Proceedings();
        when(proceedingsService.findProceedingsById(targetId)).thenReturn(targetProceedings);

        mergeService.switchAllPublicationsToOtherProceedings(sourceId, targetId);

        verify(proceedingsPublicationRepository, times(2)).save(any(ProceedingsPublication.class));
        assertEquals(targetProceedings, publication1.getProceedings());
        assertEquals(targetProceedings, publication2.getProceedings());
    }

    @Test
    void shouldSwitchInvolvements() {
        // Given
        var sourcePersonId = 1;
        var targetPersonId = 2;
        var involvementIds = List.of(100, 101, 102);

        var sourcePerson = new Person();
        sourcePerson.setId(sourcePersonId);
        var sourceInvolvements = new HashSet<Involvement>();
        involvementIds.forEach(id -> {
            var involvement = new Involvement();
            involvement.setId(id);
            involvement.setPersonInvolved(sourcePerson);
            sourceInvolvements.add(involvement);
        });
        sourcePerson.setInvolvements(sourceInvolvements);

        var targetPerson = new Person();
        targetPerson.setId(targetPersonId);
        var targetInvolvements = new HashSet<Involvement>();
        targetPerson.setInvolvements(targetInvolvements);

        when(personService.findOne(sourcePersonId)).thenReturn(sourcePerson);
        when(personService.findOne(targetPersonId)).thenReturn(targetPerson);
        involvementIds.forEach(id -> {
            var involvement = new Education();
            involvement.setId(id);
            when(involvementService.findOne(id)).thenReturn(involvement);
        });

        // When
        mergeService.switchInvolvements(involvementIds, sourcePersonId, targetPersonId, null);

        // Then
        involvementIds.forEach(id -> {
            verify(involvementService, times(1)).save(
                argThat(inv -> Objects.equals(inv.getId(), id)));
        });
        verify(personService).save(sourcePerson);
        verify(personService).save(targetPerson);
        verify(userService).updateResearcherCurrentOrganisationUnitIfBound(sourcePersonId);
        verify(userService).updateResearcherCurrentOrganisationUnitIfBound(targetPersonId);
        verify(personService).indexPerson(sourcePerson);
        verify(personService).indexPerson(targetPerson);
    }

    @Test
    void shouldSwitchInvolvementsWithInstitutionFilter() {
        // Given
        var sourcePersonId = 1;
        var targetPersonId = 2;
        var institutionId = 99;
        var involvementIds = List.of(100, 101, 102);

        var sourcePerson = new Person();
        sourcePerson.setId(sourcePersonId);
        var targetPerson = new Person();
        targetPerson.setId(targetPersonId);

        var ou1 = new OrganisationUnit();
        ou1.setId(999); // in subhierarchy

        var ou2 = new OrganisationUnit();
        ou2.setId(888); // not in subhierarchy

        var involvement1 = new Involvement();
        involvement1.setId(100);
        involvement1.setInvolvementType(InvolvementType.EMPLOYED_AT);
        involvement1.setOrganisationUnit(ou1);

        var involvement2 = new Involvement();
        involvement2.setId(101);
        involvement2.setInvolvementType(InvolvementType.HIRED_BY);
        involvement2.setOrganisationUnit(ou2);

        var involvement3 = new Involvement();
        involvement3.setId(102);
        involvement3.setInvolvementType(InvolvementType.MEMBER_OF);
        involvement3.setOrganisationUnit(ou1);

        sourcePerson.setInvolvements(
            new HashSet<>(List.of(involvement1, involvement2, involvement3)));
        targetPerson.setInvolvements(new HashSet<>());

        when(personService.findOne(sourcePersonId)).thenReturn(sourcePerson);
        when(personService.findOne(targetPersonId)).thenReturn(targetPerson);

        when(involvementService.findOne(100)).thenReturn(involvement1);
        when(involvementService.findOne(101)).thenReturn(involvement2);
        when(involvementService.findOne(102)).thenReturn(involvement3);

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(List.of(999));

        // When
        mergeService.switchInvolvements(involvementIds, sourcePersonId, targetPersonId,
            institutionId);

        // Then
        verify(involvementService, never()).save(argThat(inv -> inv.getId() == 100));

        // involvement2 and 3 should be switched
        verify(involvementService, times(1)).save(argThat(inv -> inv.getId() == 101));
        verify(involvementService, times(1)).save(argThat(inv -> inv.getId() == 102));

        verify(personService).save(sourcePerson);
        verify(personService).save(targetPerson);

        verify(userService).updateResearcherCurrentOrganisationUnitIfBound(sourcePersonId);
        verify(userService).updateResearcherCurrentOrganisationUnitIfBound(targetPersonId);

        verify(personService).indexPerson(sourcePerson);
        verify(personService).indexPerson(targetPerson);
    }

    @Test
    void shouldSwitchSkills() {
        // Given
        var sourcePersonId = 1;
        var targetPersonId = 2;
        var skillIds = List.of(100, 101, 102);

        var sourcePerson = new Person();
        sourcePerson.setId(sourcePersonId);
        var sourceSkills = new HashSet<ExpertiseOrSkill>();
        skillIds.forEach(id -> {
            var skill = new ExpertiseOrSkill();
            skill.setId(id);
            sourceSkills.add(skill);
        });
        sourcePerson.setExpertisesAndSkills(sourceSkills);

        var targetPerson = new Person();
        targetPerson.setId(targetPersonId);
        var targetSkills = new HashSet<ExpertiseOrSkill>();
        targetPerson.setExpertisesAndSkills(targetSkills);

        when(personService.findOne(sourcePersonId)).thenReturn(sourcePerson);
        when(personService.findOne(targetPersonId)).thenReturn(targetPerson);
        skillIds.forEach(id -> {
            var skill = new ExpertiseOrSkill();
            skill.setId(id);
            when(expertiseOrSkillService.findOne(id)).thenReturn(skill);
        });

        // When
        mergeService.switchSkills(skillIds, sourcePersonId, targetPersonId);

        // Then
        skillIds.forEach(id -> {
            verify(expertiseOrSkillService).findOne(id);
            assertTrue(sourcePerson.getExpertisesAndSkills().stream()
                .noneMatch(skill -> skill.getId().equals(id)));
            assertTrue(targetPerson.getExpertisesAndSkills().stream()
                .anyMatch(skill -> skill.getId().equals(id)));
        });
        verify(expertiseOrSkillService, times(3)).save(any());
    }

    @Test
    void shouldSwitchPrizes() {
        // Given
        var sourcePersonId = 1;
        var targetPersonId = 2;
        var prizeIds = List.of(100, 101, 102);

        var sourcePerson = new Person();
        sourcePerson.setId(sourcePersonId);
        var sourcePrizes = new HashSet<Prize>();
        prizeIds.forEach(id -> {
            var prize = new Prize();
            prize.setId(id);
            sourcePrizes.add(prize);
        });
        sourcePerson.setPrizes(sourcePrizes);

        var targetPerson = new Person();
        targetPerson.setId(targetPersonId);
        var targetPrizes = new HashSet<Prize>();
        targetPerson.setPrizes(targetPrizes);

        when(personService.findOne(sourcePersonId)).thenReturn(sourcePerson);
        when(personService.findOne(targetPersonId)).thenReturn(targetPerson);
        prizeIds.forEach(id -> {
            var prize = new Prize();
            prize.setId(id);
            when(prizeService.findOne(id)).thenReturn(prize);
        });

        // When
        mergeService.switchPrizes(prizeIds, sourcePersonId, targetPersonId);

        // Then
        prizeIds.forEach(id -> {
            verify(prizeService).findOne(id);
            assertTrue(sourcePerson.getPrizes().stream()
                .noneMatch(prize -> prize.getId().equals(id)));
            assertTrue(targetPerson.getPrizes().stream()
                .anyMatch(prize -> prize.getId().equals(id)));
        });
        verify(prizeService, times(3)).save(any());
    }

    @Test
    public void shouldSaveMergedProceedingsMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new ProceedingsDTO();
        var rightData = new ProceedingsDTO();

        // when
        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User() {{
            setAuthority(new Authority(
                UserRole.ADMIN.name(), new HashSet<>()));
        }});
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        mergeService.saveMergedProceedingsMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(proceedingsService, atLeastOnce()).updateProceedings(leftId, leftData);
        verify(proceedingsService).updateProceedings(rightId, rightData);
        verify(proceedingsService, times(2)).updateProceedings(leftId, leftData);
    }

    @Test
    public void shouldSaveMergedPersonsMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new PersonalInfoDTO();
        var rightData = new PersonalInfoDTO();

        // when
        mergeService.saveMergedPersonsMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(personService, atLeastOnce()).updatePersonalInfo(leftId, leftData);
        verify(personService).updatePersonalInfo(rightId, rightData);
        verify(personService, times(2)).updatePersonalInfo(leftId, leftData);
    }

    @Test
    public void shouldSaveMergedJournalsMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new JournalDTO();
        var rightData = new JournalDTO();

        // when
        mergeService.saveMergedJournalsMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(journalService, atLeastOnce()).updateJournal(leftId, leftData);
        verify(journalService).updateJournal(rightId, rightData);
        verify(journalService, times(2)).updateJournal(leftId, leftData);
    }

    @Test
    public void shouldSaveMergedBookSeriesMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new BookSeriesDTO();
        var rightData = new BookSeriesDTO();

        // when
        mergeService.saveMergedBookSeriesMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(bookSeriesService, atLeastOnce()).updateBookSeries(leftId, leftData);
        verify(bookSeriesService).updateBookSeries(rightId, rightData);
        verify(bookSeriesService, times(2)).updateBookSeries(leftId, leftData);
    }

    @Test
    public void shouldSaveMergedConferencesMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new ConferenceDTO();
        var rightData = new ConferenceDTO();

        // when
        mergeService.saveMergedConferencesMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(conferenceService, atLeastOnce()).updateConference(leftId, leftData);
        verify(conferenceService).updateConference(rightId, rightData);
        verify(conferenceService, times(2)).updateConference(leftId, leftData);
    }

    @Test
    public void shouldSaveMergedIntangibleProductMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new IntangibleProductDTO();
        var rightData = new IntangibleProductDTO();

        // when
        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User() {{
            setAuthority(new Authority(
                UserRole.ADMIN.name(), new HashSet<>()));
        }});
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        mergeService.saveMergedIntangibleProductMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(intangibleProductService, atLeastOnce()).editIntangibleProduct(leftId, leftData);
        verify(intangibleProductService).editIntangibleProduct(rightId, rightData);
        verify(intangibleProductService, times(2)).editIntangibleProduct(leftId, leftData);
    }

    @Test
    public void shouldSaveMergedMaterialProductMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new MaterialProductDTO();
        var rightData = new MaterialProductDTO();

        // when
        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User() {{
            setAuthority(new Authority(
                UserRole.ADMIN.name(), new HashSet<>()));
        }});
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        mergeService.saveMergedMaterialProductMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(materialProductService, atLeastOnce()).editMaterialProduct(leftId, leftData);
        verify(materialProductService).editMaterialProduct(rightId, rightData);
        verify(materialProductService, times(2)).editMaterialProduct(leftId, leftData);
    }

    @Test
    public void shouldSaveMergedGeneticMaterialMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new GeneticMaterialDTO();
        var rightData = new GeneticMaterialDTO();

        // when
        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User() {{
            setAuthority(new Authority(
                UserRole.ADMIN.name(), new HashSet<>()));
        }});
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        mergeService.saveMergedGeneticMaterialMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(geneticMaterialService, atLeastOnce()).editGeneticMaterial(leftId, leftData);
        verify(geneticMaterialService).editGeneticMaterial(rightId, rightData);
        verify(geneticMaterialService, times(2)).editGeneticMaterial(leftId, leftData);
    }

    @Test
    public void shouldSaveMergedDatasetsMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new DatasetDTO();
        var rightData = new DatasetDTO();

        // when
        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User() {{
            setAuthority(new Authority(
                UserRole.ADMIN.name(), new HashSet<>()));
        }});
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        mergeService.saveMergedDatasetsMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(datasetService, atLeastOnce()).editDataset(leftId, leftData);
        verify(datasetService).editDataset(rightId, rightData);
        verify(datasetService, times(2)).editDataset(leftId, leftData);
    }

    @Test
    public void shouldSaveMergedPatentsMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new PatentDTO();
        var rightData = new PatentDTO();

        // when
        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User() {{
            setAuthority(new Authority(
                UserRole.ADMIN.name(), new HashSet<>()));
        }});
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        mergeService.saveMergedPatentsMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(patentService, atLeastOnce()).editPatent(leftId, leftData);
        verify(patentService).editPatent(rightId, rightData);
        verify(patentService, times(2)).editPatent(leftId, leftData);
    }

    @Test
    public void shouldSaveMergedDocumentFiles() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftProofs = List.of(101, 102);
        var rightProofs = List.of(201, 202);
        var leftFileItems = new ArrayList<Integer>();
        var rightFileItems = new ArrayList<Integer>();

        var leftDocument = new Dataset();
        var rightDocument = new Dataset();

        var leftProof1 = new DocumentFile();
        leftProof1.setId(101);
        var leftProof2 = new DocumentFile();
        leftProof2.setId(102);
        var rightProof1 = new DocumentFile();
        rightProof1.setId(201);
        var rightProof2 = new DocumentFile();
        rightProof2.setId(202);

        leftDocument.setProofs(new HashSet<>(List.of(leftProof1, leftProof2)));
        rightDocument.setProofs(new HashSet<>(List.of(rightProof1, rightProof2)));

        when(documentPublicationService.findDocumentById(leftId)).thenReturn(leftDocument);
        when(documentPublicationService.findDocumentById(rightId)).thenReturn(rightDocument);

        // when
        mergeService.saveMergedDocumentFiles(leftId, rightId, leftProofs, rightProofs,
            leftFileItems, rightFileItems);

        // then
        verify(documentPublicationService).findDocumentById(leftId);
        verify(documentPublicationService).findDocumentById(rightId);
        verify(documentPublicationService, times(1)).findDocumentById(leftId);
        verify(documentPublicationService, times(1)).findDocumentById(rightId);
    }

    @Test
    public void shouldSaveMergedProceedingsPublicationMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new ProceedingsPublicationDTO();
        var rightData = new ProceedingsPublicationDTO();

        leftData.setDoi("10.1000/xyz123");
        leftData.setScopusId("");

        // when
        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User() {{
            setAuthority(new Authority(
                UserRole.ADMIN.name(), new HashSet<>()));
        }});
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        mergeService.saveMergedProceedingsPublicationMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(proceedingsPublicationService, times(2)).editProceedingsPublication(leftId,
            leftData);
        verify(proceedingsPublicationService).editProceedingsPublication(rightId, rightData);
        verify(proceedingsPublicationService, times(3)).editProceedingsPublication(anyInt(),
            any(ProceedingsPublicationDTO.class));
        assertEquals("10.1000/xyz123", leftData.getDoi());
        assertEquals("", leftData.getScopusId());
        verify(proceedingsPublicationService, times(2)).editProceedingsPublication(eq(leftId),
            any(ProceedingsPublicationDTO.class));
    }

    @Test
    public void shouldSaveMergedThesesMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new ThesisDTO();
        var rightData = new ThesisDTO();

        // when
        mergeService.saveMergedThesesMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(thesisService, atLeastOnce()).editThesis(leftId, leftData);
        verify(thesisService).editThesis(rightId, rightData);
        verify(thesisService, times(2)).editThesis(leftId, leftData);
    }

    @Test
    public void shouldSaveMergedJournalPublicationMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new JournalPublicationDTO();
        var rightData = new JournalPublicationDTO();

        leftData.setDoi("10.1000/xyz123");
        leftData.setScopusId("");

        // when
        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User() {{
            setAuthority(new Authority(
                UserRole.ADMIN.name(), new HashSet<>()));
        }});
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        mergeService.saveMergedJournalPublicationMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(journalPublicationService, times(2)).editJournalPublication(leftId,
            leftData);
        verify(journalPublicationService).editJournalPublication(rightId, rightData);
        verify(journalPublicationService, times(3)).editJournalPublication(anyInt(),
            any(JournalPublicationDTO.class));
        assertEquals("10.1000/xyz123", leftData.getDoi());
        assertEquals("", leftData.getScopusId());
        verify(journalPublicationService, times(2)).editJournalPublication(eq(leftId),
            any(JournalPublicationDTO.class));
    }

    @Test
    public void shouldSaveMergedMonographsMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new MonographDTO();
        var rightData = new MonographDTO();

        // when
        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User() {{
            setAuthority(new Authority(
                UserRole.ADMIN.name(), new HashSet<>()));
        }});
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        mergeService.saveMergedMonographsMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(monographService, atLeastOnce()).editMonograph(leftId, leftData);
        verify(monographService).editMonograph(rightId, rightData);
        verify(monographService, times(2)).editMonograph(leftId, leftData);
    }

    @Test
    public void shouldSaveMergedMonographPublicationsMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new MonographPublicationDTO();
        var rightData = new MonographPublicationDTO();

        leftData.setDoi("10.1000/xyz123");

        // when
        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User() {{
            setAuthority(new Authority(
                UserRole.ADMIN.name(), new HashSet<>()));
        }});
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        mergeService.saveMergedMonographPublicationsMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(monographPublicationService, times(2)).editMonographPublication(leftId,
            leftData);
        verify(monographPublicationService).editMonographPublication(rightId, rightData);
        verify(monographPublicationService, times(3)).editMonographPublication(anyInt(),
            any(MonographPublicationDTO.class));
        assertEquals("10.1000/xyz123", leftData.getDoi());
        assertNull(leftData.getScopusId());
        verify(monographPublicationService, times(2)).editMonographPublication(eq(leftId),
            any(MonographPublicationDTO.class));
    }

    @Test
    void shouldSwitchMonographPublicationToOtherMonograph() {
        // given
        var targetMonographId = 1;
        var publicationId = 2;

        var publication = new MonographPublication();
        when(monographPublicationService.findMonographPublicationById(publicationId)).thenReturn(
            publication);
        var targetMonograph = new Monograph();
        when(monographService.findMonographById(targetMonographId)).thenReturn(targetMonograph);

        // when
        mergeService.switchPublicationToOtherMonograph(targetMonographId, publicationId);

        // then
        verify(monographPublicationService).findMonographPublicationById(publicationId);
        verify(monographService).findMonographById(targetMonographId);
        assertEquals(targetMonograph, publication.getMonograph());
    }

    @Test
    void shouldSwitchAllPublicationsToOtherMonograph() {
        // given
        var sourceId = 1;
        var targetId = 2;

        var publicationIndex1 = new DocumentPublicationIndex();
        publicationIndex1.setDatabaseId(1);
        var publicationIndex2 = new DocumentPublicationIndex();
        publicationIndex2.setDatabaseId(2);
        var page1 = new PageImpl<>(
            List.of(publicationIndex1, publicationIndex2));
        var page2 = new PageImpl<DocumentPublicationIndex>(List.of());

        when(documentPublicationIndexRepository.findByTypeAndMonographId(
            DocumentPublicationType.MONOGRAPH_PUBLICATION.name(), sourceId,
            PageRequest.of(0, 100)))
            .thenReturn(page1);
        when(documentPublicationIndexRepository.findByTypeAndJournalId(
            DocumentPublicationType.JOURNAL_PUBLICATION.name(), sourceId, PageRequest.of(1, 100)))
            .thenReturn(page2);

        var publication1 = new MonographPublication();
        var publication2 = new MonographPublication();
        when(monographPublicationService.findMonographPublicationById(
            publicationIndex1.getDatabaseId())).thenReturn(publication1);
        when(monographPublicationService.findMonographPublicationById(
            publicationIndex2.getDatabaseId())).thenReturn(publication2);

        var targetMonograph = new Monograph();
        when(monographService.findMonographById(targetId)).thenReturn(targetMonograph);

        // when
        mergeService.switchAllPublicationsToOtherMonograph(sourceId, targetId);

        // then
        assertEquals(targetMonograph, publication1.getMonograph());
        assertEquals(targetMonograph, publication2.getMonograph());
    }

    @Test
    public void shouldSwitchPublisherPublicationToOtherPublisher() {
        // given
        var targetPublisherId = 2;
        var publicationId = 1;

        var publication = new Proceedings();
        publication.setPublisher(new Publisher());
        var targetPublisher = new Publisher();

        when(proceedingsRepository.findById(publicationId)).thenReturn(Optional.of(publication));
        when(publisherService.findOne(targetPublisherId)).thenReturn(targetPublisher);

        // when
        mergeService.switchPublisherPublicationToOtherPublisher(targetPublisherId, publicationId);

        // then
        verify(proceedingsRepository).findById(publicationId);
        verify(publisherService).findOne(targetPublisherId);
        verify(indexBulkUpdateService).setIdFieldForRecord(
            eq("document_publication"), eq("databaseId"), eq(publicationId), eq("publisher_id"),
            eq(targetPublisherId)
        );
    }

    @Test
    public void shouldSwitchAllPublicationsToOtherPublisher() {
        // given
        var sourceId = 1;
        var targetId = 2;

        var documentIndex1 = new DocumentPublicationIndex();
        documentIndex1.setDatabaseId(1);
        var documentIndex2 = new DocumentPublicationIndex();
        documentIndex2.setDatabaseId(2);

        var documentIndices = List.of(documentIndex1, documentIndex2);

        when(thesisRepository.findById(anyInt())).thenReturn(Optional.of(new Thesis()));
        when(documentPublicationIndexRepository.findByPublisherId(eq(sourceId),
            any(PageRequest.class)))
            .thenReturn(new PageImpl<>(documentIndices));

        // when
        mergeService.switchAllPublicationsToOtherPublisher(sourceId, targetId);

        // then
        verify(documentPublicationIndexRepository).findByPublisherId(eq(sourceId),
            any(PageRequest.class));
    }

    @Test
    public void shouldSaveMergedOUsMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new OrganisationUnitRequestDTO();
        leftData.setScopusAfid("60000765");
        var rightData = new OrganisationUnitRequestDTO();

        // when
        mergeService.saveMergedOUsMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(organisationUnitService, atLeastOnce()).editOrganisationUnit(leftId, leftData);
        verify(organisationUnitService).editOrganisationUnit(rightId, rightData);
        verify(organisationUnitService, times(2)).editOrganisationUnit(leftId, leftData);
    }

    @Test
    public void shouldSaveMergedPublishersMetadata() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new PublisherDTO();
        leftData.setCountryId(1);
        var rightData = new PublisherDTO();
        leftData.setCountryId(2);

        // when
        mergeService.saveMergedPublishersMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(publisherService, atLeastOnce()).editPublisher(leftId, leftData);
        verify(publisherService).editPublisher(rightId, rightData);
        verify(publisherService, times(2)).editPublisher(leftId, leftData);
    }

    @Test
    void testIdentifierMigrationForBookSeries() {
        BookSeries deletion = new BookSeries();
        deletion.getMergedIds().add(100);
        deletion.getOldIds().add(200);
        BookSeries merged = new BookSeries();

        when(bookSeriesService.findRaw(1)).thenReturn(deletion);
        when(bookSeriesService.findRaw(2)).thenReturn(merged);

        mergeService.migratePersistentIdentifiers(1, 2, EntityType.BOOK_SERIES);

        assertThat(merged.getMergedIds()).containsExactlyInAnyOrder(100, 1);
        assertThat(merged.getOldIds()).containsExactly(200);
        verify(bookSeriesService, atLeastOnce()).save(deletion);
        verify(bookSeriesService, atLeastOnce()).save(merged);
    }

    @Test
    void testIdentifierMigrationForMonograph() {
        Monograph deletion = new Monograph();
        deletion.getMergedIds().add(101);
        deletion.getOldIds().add(201);
        Monograph merged = new Monograph();

        when(monographService.findRaw(1)).thenReturn(deletion);
        when(monographService.findRaw(2)).thenReturn(merged);

        mergeService.migratePersistentIdentifiers(1, 2, EntityType.MONOGRAPH);

        assertThat(merged.getMergedIds()).containsExactlyInAnyOrder(101, 1);
        assertThat(merged.getOldIds()).containsExactly(201);
        verify(documentPublicationService, atLeastOnce()).save(deletion);
        verify(documentPublicationService, atLeastOnce()).save(merged);
    }

    @Test
    void testIdentifierMigrationForProceedings() {
        Proceedings deletion = new Proceedings();
        deletion.getMergedIds().add(102);
        deletion.getOldIds().add(202);
        Proceedings merged = new Proceedings();

        when(proceedingsService.findRaw(1)).thenReturn(deletion);
        when(proceedingsService.findRaw(2)).thenReturn(merged);

        mergeService.migratePersistentIdentifiers(1, 2, EntityType.PROCEEDINGS);

        assertThat(merged.getMergedIds()).containsExactlyInAnyOrder(102, 1);
        assertThat(merged.getOldIds()).containsExactly(202);
        verify(documentPublicationService, atLeastOnce()).save(deletion);
        verify(documentPublicationService, atLeastOnce()).save(merged);
    }

    @ParameterizedTest
    @MethodSource("provideDocumentPublicationsForMigration")
    void shouldMigrateIdentifiersForPublications(
        Mergeable deletionEntity,
        Mergeable mergedEntity,
        Class<?> entityClass,
        EntityType entityType
    ) {
        // Given
        if (deletionEntity instanceof Document) {
            when(documentPublicationService.findOne(1)).thenReturn((Document) deletionEntity);
            when(documentPublicationService.findOne(2)).thenReturn((Document) mergedEntity);
        } else if (deletionEntity instanceof OrganisationUnit) {
            when(organisationUnitService.findRaw(1)).thenReturn((OrganisationUnit) deletionEntity);
            when(organisationUnitService.findRaw(2)).thenReturn((OrganisationUnit) mergedEntity);
        } else if (deletionEntity instanceof Journal) {
            when(journalService.findRaw(1)).thenReturn((Journal) deletionEntity);
            when(journalService.findRaw(2)).thenReturn((Journal) mergedEntity);
        } else if (deletionEntity instanceof Publisher) {
            when(publisherService.findRaw(1)).thenReturn((Publisher) deletionEntity);
            when(publisherService.findRaw(2)).thenReturn((Publisher) mergedEntity);
        } else if (deletionEntity instanceof Person) {
            when(personService.findRaw(1)).thenReturn((Person) deletionEntity);
            when(personService.findRaw(2)).thenReturn((Person) mergedEntity);
        }

        // When
        mergeService.migratePersistentIdentifiers(1, 2, entityType);

        // Then
        assertThat(mergedEntity.getMergedIds()).containsExactlyInAnyOrder(103, 1);
        assertThat(mergedEntity.getOldIds()).containsExactly(203);

        if (deletionEntity instanceof Document) {
            verify(documentPublicationService, atLeastOnce()).save(
                argThat(entityClass::isInstance));
            verify(documentPublicationService, atLeastOnce()).save(
                argThat(entityClass::isInstance));
        } else if (deletionEntity instanceof OrganisationUnit) {
            verify(organisationUnitService, atLeastOnce()).save(argThat(entityClass::isInstance));
            verify(organisationUnitService, atLeastOnce()).save(argThat(entityClass::isInstance));
        } else if (deletionEntity instanceof Journal) {
            verify(journalService, atLeastOnce()).save(argThat(entityClass::isInstance));
            verify(journalService, atLeastOnce()).save(argThat(entityClass::isInstance));
        } else if (deletionEntity instanceof Publisher) {
            verify(publisherService, atLeastOnce()).save(argThat(entityClass::isInstance));
            verify(publisherService, atLeastOnce()).save(argThat(entityClass::isInstance));
        } else if (deletionEntity instanceof Person) {
            verify(personService, atLeastOnce()).save(argThat(entityClass::isInstance));
            verify(personService, atLeastOnce()).save(argThat(entityClass::isInstance));
        }
    }

    @Test
    void testIdentifierMigrationForEvent() {
        Conference deletion = new Conference();
        deletion.getMergedIds().add(104);
        deletion.getOldIds().add(204);
        Conference merged = new Conference();

        when(conferenceService.findRaw(1)).thenReturn(deletion);
        when(conferenceService.findRaw(2)).thenReturn(merged);

        mergeService.migratePersistentIdentifiers(1, 2, EntityType.EVENT);

        assertThat(merged.getMergedIds()).containsExactlyInAnyOrder(104, 1);
        assertThat(merged.getOldIds()).containsExactly(204);
        verify(conferenceService, atLeastOnce()).save(deletion);
        verify(conferenceService, atLeastOnce()).save(merged);
    }

    @Test
    void testIdentifierMigrationForJournal() {
        Journal deletion = new Journal();
        deletion.getMergedIds().add(105);
        deletion.getOldIds().add(205);
        Journal merged = new Journal();

        when(journalService.findRaw(1)).thenReturn(deletion);
        when(journalService.findRaw(2)).thenReturn(merged);

        mergeService.migratePersistentIdentifiers(1, 2, EntityType.JOURNAL);

        assertThat(merged.getMergedIds()).containsExactlyInAnyOrder(105, 1);
        assertThat(merged.getOldIds()).containsExactly(205);
        verify(journalService, atLeastOnce()).save(deletion);
        verify(journalService, atLeastOnce()).save(merged);
    }

    @Test
    void testIdentifierMigrationForOrganisationUnit() {
        OrganisationUnit deletion = new OrganisationUnit();
        deletion.setId(1);
        deletion.getMergedIds().add(106);
        deletion.getOldIds().add(206);
        OrganisationUnit merged = new OrganisationUnit();
        merged.setId(2);

        when(organisationUnitService.findRaw(1)).thenReturn(deletion);
        when(organisationUnitService.findRaw(2)).thenReturn(merged);

        mergeService.migratePersistentIdentifiers(1, 2, EntityType.ORGANISATION_UNIT);

        assertThat(merged.getMergedIds()).containsExactlyInAnyOrder(106, 1);
        assertThat(merged.getOldIds()).containsExactly(206);
        verify(organisationUnitService, atLeastOnce()).save(merged);
    }

    @Test
    void testIdentifierMigrationForPerson() {
        Person deletion = new Person();
        deletion.getMergedIds().add(107);
        deletion.getOldIds().add(207);
        Person merged = new Person();

        when(personService.findRaw(1)).thenReturn(deletion);
        when(personService.findRaw(2)).thenReturn(merged);

        mergeService.migratePersistentIdentifiers(1, 2, EntityType.PERSON);

        assertThat(merged.getMergedIds()).containsExactlyInAnyOrder(107, 1);
        assertThat(merged.getOldIds()).containsExactly(207);
        verify(personService, atLeastOnce()).save(deletion);
        verify(personService, atLeastOnce()).save(merged);
    }

    @Test
    void testIdentifierMigrationForPublisher() {
        Publisher deletion = new Publisher();
        deletion.getMergedIds().add(108);
        deletion.getOldIds().add(208);
        Publisher merged = new Publisher();

        when(publisherService.findRaw(1)).thenReturn(deletion);
        when(publisherService.findRaw(2)).thenReturn(merged);

        mergeService.migratePersistentIdentifiers(1, 2, EntityType.PUBLISHER);

        assertThat(merged.getMergedIds()).containsExactlyInAnyOrder(108, 1);
        assertThat(merged.getOldIds()).containsExactly(208);
        verify(publisherService, atLeastOnce()).save(deletion);
        verify(publisherService, atLeastOnce()).save(merged);
    }
}
