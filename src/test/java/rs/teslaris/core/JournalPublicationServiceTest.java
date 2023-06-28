package rs.teslaris.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.DocumentFileService;
import rs.teslaris.core.service.JournalService;
import rs.teslaris.core.service.MultilingualContentService;
import rs.teslaris.core.service.PersonContributionService;
import rs.teslaris.core.service.impl.JournalPublicationServiceImpl;

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
    private PersonContributionService personContributionService;

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
        var document = new JournalPublication();

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(documentRepository.save(any())).thenReturn(document);

        // When
        var result = journalPublicationService.createJournalPublication(publicationDTO);

        // Then
        verify(multilingualContentService, times(4)).getMultilingualContent(any());
        verify(personContributionService).setPersonDocumentContributionsForDocument(eq(document),
            eq(publicationDTO));
        verify(documentRepository).save(eq(document));
    }

    @Test
    public void shouldEditJournalPublication() {
        // Given
        var publicationId = 1;
        var publicationDTO = new JournalPublicationDTO();
        var publicationToUpdate = new JournalPublication();
        publicationToUpdate.setTitle(new HashSet<>());
        publicationToUpdate.setSubTitle(new HashSet<>());
        publicationToUpdate.setDescription(new HashSet<>());
        publicationToUpdate.setKeywords(new HashSet<>());
        publicationToUpdate.setContributors(new HashSet<>());
        publicationToUpdate.setUris(new HashSet<>());

        when(documentRepository.findById(publicationId)).thenReturn(
            Optional.of(publicationToUpdate));

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
}
