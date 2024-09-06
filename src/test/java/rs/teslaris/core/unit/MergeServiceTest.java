package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.ExpertiseOrSkill;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.Prize;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.service.impl.comparator.MergeServiceImpl;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DatasetService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.PatentService;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.document.SoftwareService;
import rs.teslaris.core.service.interfaces.person.ExpertiseOrSkillService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.person.PrizeService;
import rs.teslaris.core.service.interfaces.user.UserService;

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
    private SoftwareService softwareService;

    @Mock
    private DatasetService datasetService;

    @Mock
    private PatentService patentService;

    @InjectMocks
    private MergeServiceImpl mergeService;


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
            DocumentPublicationType.JOURNAL_PUBLICATION.name(), sourceId, PageRequest.of(0, 10)))
            .thenReturn(page1);
        when(documentPublicationIndexRepository.findByTypeAndJournalId(
            DocumentPublicationType.JOURNAL_PUBLICATION.name(), sourceId, PageRequest.of(1, 10)))
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

        when(documentPublicationService.findResearcherPublications(sourceId,
            PageRequest.of(0, 10))).thenReturn(page1);
        when(documentPublicationService.findResearcherPublications(sourceId,
            PageRequest.of(1, 10))).thenReturn(page2);

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

        mergeService.switchAllPublicationToOtherPerson(sourceId, targetId);

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
        verify(personService).indexPerson(person, personId);
    }

    @Test
    public void switchAllPersonsToOtherOUTest() {
        // given
        var sourceOUId = 1;
        var targetOUId = 2;
        var personId = 3;
        var person = new Person();
        var employment = new Employment();
        employment.setInvolvementType(InvolvementType.EMPLOYED_AT);
        var organisationUnit = new OrganisationUnit();
        organisationUnit.setId(1);
        employment.setOrganisationUnit(organisationUnit);
        person.setInvolvements(Set.of(employment));
        var personIndex = new PersonIndex();
        var pageRequest = PageRequest.of(0, 10);
        var page = new PageImpl<>(List.of(personIndex));
        when(personService.findPeopleForOrganisationUnit(sourceOUId, pageRequest)).thenReturn(page);
        when(personService.findOne(any())).thenReturn(person);

        // when
        mergeService.switchAllPersonsToOtherOU(sourceOUId, targetOUId);

        // then
        verify(personService, atLeastOnce()).findPeopleForOrganisationUnit(eq(sourceOUId),
            any(PageRequest.class));
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
            PageRequest.of(0, 10)))
            .thenReturn(page1);
        when(documentPublicationIndexRepository.findByTypeAndJournalId(
            DocumentPublicationType.JOURNAL_PUBLICATION.name(), sourceId, PageRequest.of(1, 10)))
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
    void testSwitchInvolvements() {
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
        mergeService.switchInvolvements(involvementIds, sourcePersonId, targetPersonId);

        // Then
        involvementIds.forEach(id -> {
            var involvement = new Involvement();
            involvement.setId(id);
            verify(involvementService, times(1)).save(any(Involvement.class));
        });
        verify(personService).save(sourcePerson);
        verify(personService).save(targetPerson);
        verify(userService).updateResearcherCurrentOrganisationUnitIfBound(sourcePersonId);
        verify(userService).updateResearcherCurrentOrganisationUnitIfBound(targetPersonId);
        verify(personService).indexPerson(sourcePerson, sourcePersonId);
        verify(personService).indexPerson(targetPerson, targetPersonId);
    }

    @Test
    void testSwitchSkills() {
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
            var skill = new ExpertiseOrSkill();
            skill.setId(id);
            verify(expertiseOrSkillService).findOne(id);
            assertFalse(sourcePerson.getExpertisesAndSkills().contains(skill));
            assertTrue(targetPerson.getExpertisesAndSkills().contains(skill));
        });
        verify(personService).save(sourcePerson);
        verify(personService).save(targetPerson);
    }

    @Test
    void testSwitchPrizes() {
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
            var prize = new Prize();
            prize.setId(id);
            verify(prizeService).findOne(id);
            assertFalse(sourcePerson.getPrizes().contains(prize));
            assertTrue(targetPerson.getPrizes().contains(prize));
        });
        verify(personService).save(sourcePerson);
        verify(personService).save(targetPerson);
    }

    @Test
    public void saveMergedProceedingsMetadataTest() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new ProceedingsDTO();
        var rightData = new ProceedingsDTO();

        // when
        mergeService.saveMergedProceedingsMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(proceedingsService, atLeastOnce()).updateProceedings(leftId, leftData);
        verify(proceedingsService).updateProceedings(rightId, rightData);
        verify(proceedingsService, times(2)).updateProceedings(leftId, leftData);
    }

    @Test
    public void saveMergedPersonsMetadataTest() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new PersonalInfoDTO();
        var rightData = new PersonalInfoDTO();

        // when
        mergeService.saveMergedPersonsMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(personService, atLeastOnce()).updatePersonalInfo(leftData, leftId);
        verify(personService).updatePersonalInfo(rightData, rightId);
        verify(personService, times(2)).updatePersonalInfo(leftData, leftId);
    }

    @Test
    public void saveMergedJournalsMetadataTest() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new JournalDTO();
        var rightData = new JournalDTO();

        // when
        mergeService.saveMergedJournalsMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(journalService, atLeastOnce()).updateJournal(leftData, leftId);
        verify(journalService).updateJournal(rightData, rightId);
        verify(journalService, times(2)).updateJournal(leftData, leftId);
    }

    @Test
    public void saveMergedConferencesMetadataTest() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new ConferenceDTO();
        var rightData = new ConferenceDTO();

        // when
        mergeService.saveMergedConferencesMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(conferenceService, atLeastOnce()).updateConference(leftData, leftId);
        verify(conferenceService).updateConference(rightData, rightId);
        verify(conferenceService, times(2)).updateConference(leftData, leftId);
    }

    @Test
    public void saveMergedSoftwareMetadataTest() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new SoftwareDTO();
        var rightData = new SoftwareDTO();

        // when
        mergeService.saveMergedSoftwareMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(softwareService, atLeastOnce()).editSoftware(leftId, leftData);
        verify(softwareService).editSoftware(rightId, rightData);
        verify(softwareService, times(2)).editSoftware(leftId, leftData);
    }

    @Test
    public void saveMergedDatasetsMetadataTest() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new DatasetDTO();
        var rightData = new DatasetDTO();

        // when
        mergeService.saveMergedDatasetsMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(datasetService, atLeastOnce()).editDataset(leftId, leftData);
        verify(datasetService).editDataset(rightId, rightData);
        verify(datasetService, times(2)).editDataset(leftId, leftData);
    }

    @Test
    public void saveMergedPatentsMetadataTest() {
        // given
        var leftId = 1;
        var rightId = 2;
        var leftData = new PatentDTO();
        var rightData = new PatentDTO();

        // when
        mergeService.saveMergedPatentsMetadata(leftId, rightId, leftData, rightData);

        // then
        verify(patentService, atLeastOnce()).editPatent(leftId, leftData);
        verify(patentService).editPatent(rightId, rightData);
        verify(patentService, times(2)).editPatent(leftId, leftData);
    }
}
