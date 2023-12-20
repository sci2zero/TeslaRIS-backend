package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
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
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.service.impl.document.ProceedingsPublicationServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.ProceedingPublicationJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class ProceedingsPublicationServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentFileService documentFileService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private ProceedingsService proceedingsService;

    @Mock
    private EventService eventService;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private ProceedingsPublicationRepository proceedingsPublicationRepository;

    @Mock
    private ProceedingPublicationJPAServiceImpl proceedingPublicationJPAService;

    @InjectMocks
    private ProceedingsPublicationServiceImpl proceedingsPublicationService;

    private static Stream<Arguments> argumentSources() {
        var country = new Country();
        country.setId(1);
        return Stream.of(Arguments.of(DocumentContributionType.AUTHOR, true, false, null),
            Arguments.of(DocumentContributionType.AUTHOR, false, true, country),
            Arguments.of(DocumentContributionType.EDITOR, false, true, country),
            Arguments.of(DocumentContributionType.REVIEWER, false, true, null),
            Arguments.of(DocumentContributionType.ADVISOR, false, false, country));
    }

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(proceedingsPublicationService, "documentApprovedByDefault",
            false);
    }

    @Test
    public void shouldCreateProceedingsPublication() {
        // Given
        var publicationDTO = new ProceedingsPublicationDTO();
        publicationDTO.setProceedingsId(1);
        publicationDTO.setEventId(1);
        var document = new ProceedingsPublication();

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(documentRepository.save(any())).thenReturn(document);
        when(proceedingsService.findProceedingsById(publicationDTO.getProceedingsId())).thenReturn(
            new Proceedings());
        when(eventService.findEventById(publicationDTO.getProceedingsId())).thenReturn(
            new Conference());

        // When
        var result = proceedingsPublicationService.createProceedingsPublication(publicationDTO);

        // Then
        verify(multilingualContentService, times(4)).getMultilingualContent(any());
        verify(personContributionService).setPersonDocumentContributionsForDocument(eq(document),
            eq(publicationDTO));
        verify(proceedingPublicationJPAService).save(eq(document));
    }

    @Test
    public void shouldEditProceedingsPublication() {
        // Given
        var publicationId = 1;
        var publicationDTO = new ProceedingsPublicationDTO();
        publicationDTO.setProceedingsId(1);
        publicationDTO.setEventId(1);
        var publicationToUpdate = new ProceedingsPublication();
        publicationToUpdate.setApproveStatus(ApproveStatus.REQUESTED);
        publicationToUpdate.setTitle(new HashSet<>());
        publicationToUpdate.setSubTitle(new HashSet<>());
        publicationToUpdate.setDescription(new HashSet<>());
        publicationToUpdate.setKeywords(new HashSet<>());
        publicationToUpdate.setContributors(new HashSet<>());
        publicationToUpdate.setUris(new HashSet<>());

        when(documentRepository.findById(publicationId)).thenReturn(
            Optional.of(publicationToUpdate));
        when(proceedingsService.findProceedingsById(publicationDTO.getProceedingsId())).thenReturn(
            new Proceedings());
        when(eventService.findEventById(publicationDTO.getProceedingsId())).thenReturn(
            new Conference());

        // When
        proceedingsPublicationService.editProceedingsPublication(publicationId, publicationDTO);

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
        var publication = new ProceedingsPublication();
        publication.setApproveStatus(status);

        when(documentRepository.findById(publicationId)).thenReturn(Optional.of(publication));

        // When
        assertThrows(NotFoundException.class,
            () -> proceedingsPublicationService.readProceedingsPublicationById(publicationId));

        // Then (NotFoundException should be thrown)
    }

    @ParameterizedTest
    @MethodSource("argumentSources")
    public void shouldReadProceedingsPublication(DocumentContributionType type,
                                                 Boolean isMainAuthor,
                                                 Boolean isCorrespondingAuthor, Country country) {
        // Given
        var publicationId = 1;
        var publication = new ProceedingsPublication();
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
        var proceedings = new Proceedings();
        proceedings.setId(1);
        publication.setProceedings(proceedings);

        when(documentRepository.findById(publicationId)).thenReturn(Optional.of(publication));

        // When
        var result = proceedingsPublicationService.readProceedingsPublicationById(publicationId);

        // Then
        verify(documentRepository).findById(eq(publicationId));
        assertNotNull(result);
        assertEquals(1, result.getContributions().size());
    }

    @Test
    public void shouldReadProceedingsPublicationForEvent() {
        // Given
        var eventId = 1;
        var authorId = 1;
        var proceedings = new Proceedings();
        proceedings.setTitle(new HashSet<>());
        var publication = new ProceedingsPublication();
        publication.setProceedings(proceedings);
        publication.setTitle(new HashSet<>());
        publication.setSubTitle(new HashSet<>());
        publication.setDescription(new HashSet<>());
        publication.setKeywords(new HashSet<>());
        publication.setApproveStatus(ApproveStatus.APPROVED);
        publication.setUris(new HashSet<>());

        when(proceedingsPublicationRepository.findProceedingsPublicationsForEventId(
            eventId, authorId)).thenReturn(List.of(publication));

        // When
        var result = proceedingsPublicationService.findProceedingsForEvent(eventId, authorId);

        // Then
        verify(proceedingsPublicationRepository, times(1)).findProceedingsPublicationsForEventId(
            eventId, authorId);
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
