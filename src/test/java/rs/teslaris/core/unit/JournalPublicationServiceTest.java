package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.service.impl.document.JournalPublicationServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.JournalPublicationJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class JournalPublicationServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentFileService documentFileService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private JournalService journalService;

    @Mock
    private EventService eventService;

    @Mock
    private JournalPublicationRepository journalPublicationRepository;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private JournalPublicationJPAServiceImpl journalPublicationJPAService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @InjectMocks
    private JournalPublicationServiceImpl journalPublicationService;

    private static Stream<Arguments> argumentSources() {
        var country = new Country();
        country.setId(1);
        return Stream.of(
            Arguments.of(DocumentContributionType.AUTHOR, true, false, null),
            Arguments.of(DocumentContributionType.AUTHOR, false, true, country),
            Arguments.of(DocumentContributionType.EDITOR, false, true, country),
            Arguments.of(DocumentContributionType.REVIEWER, false, true, null),
            Arguments.of(DocumentContributionType.ADVISOR, false, false, country)
        );
    }

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(journalPublicationService, "documentApprovedByDefault", true);
    }

    @Test
    public void shouldCreateJournalPublication() {
        // Given
        var publicationDTO = new JournalPublicationDTO();
        publicationDTO.setEventId(1);
        var journal = new Journal();
        journal.setId(1);
        var document = new JournalPublication();
        document.setJournal(journal);
        document.setContributors(new HashSet<>());
        document.setTitle(new HashSet<>());
        document.setSubTitle(new HashSet<>());
        document.setDescription(new HashSet<>());
        document.setKeywords(new HashSet<>());
        document.setFileItems(new HashSet<>());
        document.setDocumentDate("2023");

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(journalPublicationJPAService.save(any())).thenReturn(document);
        when(eventService.findEventById(1)).thenReturn(new Conference());

        // When
        var result = journalPublicationService.createJournalPublication(publicationDTO);

        // Then
        verify(multilingualContentService, times(4)).getMultilingualContent(any());
        verify(personContributionService).setPersonDocumentContributionsForDocument(eq(document),
            eq(publicationDTO));
        verify(journalPublicationJPAService).save(eq(document));
    }

    @Test
    public void shouldEditJournalPublication() {
        // Given
        var publicationId = 1;
        var publicationDTO = new JournalPublicationDTO();
        publicationDTO.setEventId(1);
        var publicationToUpdate = new JournalPublication();
        publicationToUpdate.setApproveStatus(ApproveStatus.REQUESTED);
        publicationToUpdate.setTitle(new HashSet<>());
        publicationToUpdate.setSubTitle(new HashSet<>());
        publicationToUpdate.setDescription(new HashSet<>());
        publicationToUpdate.setKeywords(new HashSet<>());
        publicationToUpdate.setContributors(new HashSet<>());
        publicationToUpdate.setUris(new HashSet<>());

        when(documentRepository.findById(publicationId)).thenReturn(
            Optional.of(publicationToUpdate));
        when(eventService.findEventById(1)).thenReturn(new Conference());

        // When
        journalPublicationService.editJournalPublication(publicationId, publicationDTO);

        // Then
        verify(documentRepository).findById(eq(publicationId));
        verify(personContributionService).setPersonDocumentContributionsForDocument(
            eq(publicationToUpdate), eq(publicationDTO));
    }

    @ParameterizedTest
    @EnumSource(value = ApproveStatus.class, names = {"REQUESTED", "DECLINED"})
    public void shouldNotReadUnapprovedPublications(ApproveStatus status) {
        // Given
        var publicationId = 1;
        var publication = new JournalPublication();
        publication.setApproveStatus(status);

        when(documentRepository.findById(publicationId)).thenReturn(
            Optional.of(publication));

        // When
        assertThrows(NotFoundException.class,
            () -> journalPublicationService.readJournalPublicationById(publicationId));

        // Then (NotFoundException should be thrown)
    }

    @ParameterizedTest
    @MethodSource("argumentSources")
    public void shouldReadJournalPublication(DocumentContributionType type, Boolean isMainAuthor,
                                             Boolean isCorrespondingAuthor, Country country) {
        // Given
        var publicationId = 1;
        var publication = new JournalPublication();
        publication.setTitle(new HashSet<>());
        publication.setSubTitle(new HashSet<>());
        publication.setDescription(new HashSet<>());
        publication.setKeywords(new HashSet<>());
        publication.setApproveStatus(ApproveStatus.APPROVED);

        var contribution = new PersonDocumentContribution();
        contribution.setContributionDescription(new HashSet<>());
        contribution.setInstitutions(new HashSet<>());
        contribution.setContributionType(type);
        contribution.setMainContributor(isMainAuthor);
        contribution.setCorrespondingContributor(isCorrespondingAuthor);
        contribution.setApproveStatus(ApproveStatus.APPROVED);
        var affiliationStatement = new AffiliationStatement();
        affiliationStatement.setDisplayAffiliationStatement(new HashSet<>());
        affiliationStatement.setContact(new Contact());
        affiliationStatement.setDisplayPersonName(new PersonName());
        affiliationStatement.setPostalAddress(
            new PostalAddress(country, new HashSet<>(), new HashSet<>()));
        contribution.setAffiliationStatement(affiliationStatement);
        publication.setContributors(Set.of(contribution));

        publication.setUris(new HashSet<>());
        var journal = new Journal();
        journal.setTitle(new HashSet<>());
        publication.setJournal(journal);

        when(documentRepository.findById(publicationId)).thenReturn(
            Optional.of(publication));

        // When
        var result = journalPublicationService.readJournalPublicationById(publicationId);

        // Then
        verify(documentRepository).findById(eq(publicationId));
        assertNotNull(result);
        assertEquals(1, result.getContributions().size());
    }

    @Test
    public void shouldFindMyPublicationsInJournal() {
        // Given
        var journalId = 123;
        var authorId = 456;

        var publication1 = new DocumentPublicationIndex();
        publication1.setId("1");
        publication1.setType("JOURNAL_PUBLICATION");
        publication1.setJournalId(journalId);
        publication1.getAuthorIds().add(authorId);

        var publication2 = new DocumentPublicationIndex();
        publication2.setId("2");
        publication2.setType("JOURNAL_PUBLICATION");
        publication2.setJournalId(journalId);
        publication2.getAuthorIds().add(authorId);

        var expectedPublications = Arrays.asList(publication1, publication2);

        when(documentPublicationIndexRepository.findByTypeAndJournalIdAndAuthorIds(
            DocumentPublicationType.JOURNAL_PUBLICATION.name(),
            journalId, authorId))
            .thenReturn(expectedPublications);

        // When
        var actualPublications =
            journalPublicationService.findMyPublicationsInJournal(journalId, authorId);

        // Then
        assertEquals(expectedPublications, actualPublications);
    }

    @Test
    public void shouldFindAllPublicationsInJournal() {
        // Given
        var journalId = 123;

        var publication1 = new DocumentPublicationIndex();
        publication1.setId("1");
        publication1.setType("JOURNAL_PUBLICATION");
        publication1.setJournalId(journalId);

        var publication2 = new DocumentPublicationIndex();
        publication2.setId("2");
        publication2.setType("JOURNAL_PUBLICATION");
        publication2.setJournalId(journalId);

        var expectedPublications = Arrays.asList(publication1, publication2);

        when(documentPublicationIndexRepository.findByTypeAndJournalId(
            DocumentPublicationType.JOURNAL_PUBLICATION.name(), journalId))
            .thenReturn(expectedPublications);

        // When
        var actualPublications =
            journalPublicationService.findPublicationsInJournal(journalId);

        // Then
        assertEquals(expectedPublications, actualPublications);
    }
}
