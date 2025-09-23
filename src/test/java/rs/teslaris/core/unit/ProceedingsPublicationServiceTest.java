package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
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
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.ProceedingsPublicationServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.ProceedingPublicationJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
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
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private ProceedingPublicationJPAServiceImpl proceedingPublicationJPAService;

    @Mock
    private ConferenceService conferenceService;

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private OrganisationUnitTrustConfigurationService organisationUnitTrustConfigurationService;

    @Mock
    private CitationService citationService;

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
        var conference = new Conference();
        conference.setId(1);
        document.setEvent(conference);
        document.setProceedings(new Proceedings() {{
            setEvent(conference);
        }});

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(documentRepository.save(any())).thenReturn(document);
        when(proceedingsService.findProceedingsById(publicationDTO.getProceedingsId())).thenReturn(
            new Proceedings());
        when(eventService.findOne(publicationDTO.getProceedingsId())).thenReturn(conference);
        when(proceedingPublicationJPAService.save(any())).thenReturn(document);

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        var result =
            proceedingsPublicationService.createProceedingsPublication(publicationDTO, true);

        // Then
        verify(multilingualContentService, times(5)).getMultilingualContent(any());
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

        var conference = new Conference();
        conference.setId(1);
        publicationToUpdate.setEvent(conference);

        when(proceedingPublicationJPAService.findOne(publicationId)).thenReturn(
            publicationToUpdate);
        when(proceedingsService.findProceedingsById(publicationDTO.getProceedingsId())).thenReturn(
            new Proceedings() {{
                setEvent(conference);
            }});
        when(eventService.findOne(publicationDTO.getProceedingsId())).thenReturn(conference);
        when(proceedingPublicationJPAService.save(any())).thenReturn(publicationToUpdate);

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        proceedingsPublicationService.editProceedingsPublication(publicationId, publicationDTO);

        // Then
        verify(proceedingPublicationJPAService).findOne(eq(publicationId));
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

        when(proceedingPublicationJPAService.findOne(publicationId)).thenReturn(publication);

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
        publication.setApproveStatus(ApproveStatus.APPROVED);
        publication.setEvent(new Conference());

        var contribution = new PersonDocumentContribution();
        contribution.setContributionType(type);
        contribution.setIsMainContributor(isMainAuthor);
        contribution.setIsCorrespondingContributor(isCorrespondingAuthor);
        contribution.setApproveStatus(ApproveStatus.APPROVED);
        var affiliationStatement = new AffiliationStatement();
        affiliationStatement.setContact(new Contact());
        affiliationStatement.setDisplayPersonName(new PersonName());
        affiliationStatement.setPostalAddress(
            new PostalAddress(country, new HashSet<>(), new HashSet<>()));
        contribution.setAffiliationStatement(affiliationStatement);
        publication.setContributors(Set.of(contribution));

        var proceedings = new Proceedings();
        proceedings.setId(1);
        publication.setProceedings(proceedings);

        when(proceedingPublicationJPAService.findOne(publicationId)).thenReturn(publication);

        // When
        var result = proceedingsPublicationService.readProceedingsPublicationById(publicationId);

        // Then
        verify(proceedingPublicationJPAService).findOne(eq(publicationId));
        assertNotNull(result);
        assertEquals(1, result.getContributions().size());
    }

    @Test
    public void shouldReadProceedingsPublicationForEvent() {
        // Given
        var eventId = 1;
        var authorId = 1;
        var proceedings = new Proceedings();
        var publication = new ProceedingsPublication();
        publication.setProceedings(proceedings);
        publication.setApproveStatus(ApproveStatus.APPROVED);

        when(proceedingsPublicationRepository.findProceedingsPublicationsForEventId(
            eventId, authorId)).thenReturn(List.of(publication));

        // When
        var result =
            proceedingsPublicationService.findAuthorsProceedingsForEvent(eventId, authorId);

        // Then
        verify(proceedingsPublicationRepository, times(1)).findProceedingsPublicationsForEventId(
            eventId, authorId);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    public void shouldFindDocumentPublicationsForEvent() {
        // Given
        var eventId = 1;
        var pageable = Pageable.ofSize(5);

        when(documentPublicationIndexRepository.findByTypeAndEventId(
            DocumentPublicationType.PROCEEDINGS_PUBLICATION.name(), eventId, pageable)).thenReturn(
            new PageImpl<>(
                List.of(new DocumentPublicationIndex(), new DocumentPublicationIndex())));

        // When
        var result = proceedingsPublicationService.findProceedingsForEvent(eventId, pageable);

        // Then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    public void shouldReindexProceedingsPublications() {
        // Given
        var proceedingsPublication = new ProceedingsPublication();
        proceedingsPublication.setDocumentDate("2024");
        var proceedings = new Proceedings();
        proceedings.setEvent(new Conference());
        proceedingsPublication.setProceedings(proceedings);
        var peroceedingsPublications = List.of(proceedingsPublication);
        var page1 = new PageImpl<>(peroceedingsPublications.subList(0, 1), PageRequest.of(0, 10),
            peroceedingsPublications.size());

        when(proceedingPublicationJPAService.findAll(any(PageRequest.class))).thenReturn(page1);

        // When
        proceedingsPublicationService.reindexProceedingsPublications();

        // Then
        verify(documentPublicationIndexRepository, never()).deleteAll();
        verify(proceedingPublicationJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }
}
