package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.repository.document.ProceedingsRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.ProceedingsServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.ProceedingsJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class ProceedingsServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentFileService documentFileService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private ProceedingsRepository proceedingsRepository;

    @Mock
    private LanguageService languageService;

    @Mock
    private JournalService journalService;

    @Mock
    private EventService eventService;

    @Mock
    private PublisherService publisherService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private ProceedingsJPAServiceImpl proceedingsJPAService;

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private OrganisationUnitTrustConfigurationService organisationUnitTrustConfigurationService;

    @Mock
    private CitationService citationService;

    @Mock
    private ProceedingsPublicationRepository proceedingsPublicationRepository;

    @Mock
    private IndexBulkUpdateService indexBulkUpdateService;

    @InjectMocks
    private ProceedingsServiceImpl proceedingsService;


    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(proceedingsService, "documentApprovedByDefault", true);
    }

    @Test
    public void shouldReturnProceedingsWhenProceedingsExists() {
        // given
        var expectedProceedings = new Proceedings();

        when(proceedingsJPAService.findOne(1)).thenReturn(expectedProceedings);

        // when
        var actualProceedings = proceedingsService.findProceedingsById(1);

        // then
        assertEquals(expectedProceedings, actualProceedings);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenProceedingsDoesNotExist() {
        // given
        when(proceedingsJPAService.findOne(1)).thenThrow(NotFoundException.class);

        // when
        assertThrows(NotFoundException.class, () -> proceedingsService.findProceedingsById(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldReturnProceedingsForEvent() {
        // given
        var event = new Conference();
        var document = new Proceedings();
        document.setDocumentDate("MOCK DATE");
        document.setEvent(event);

        when(proceedingsRepository.findProceedingsForEventId(1)).thenReturn(List.of(document));

        // when
        var actualProceedings = proceedingsService.readProceedingsForEventId(1);

        // then
        assertEquals(1, actualProceedings.size());
    }

    @Test
    public void shouldCreateProceedings() {
        // Given
        var proceedingsDTO = new ProceedingsDTO();
        proceedingsDTO.setLanguageIds(new ArrayList<>());
        var document = new Proceedings();
        document.setDocumentDate("MOCK DATE");
        document.setEvent(new Conference());

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(proceedingsJPAService.save(any())).thenReturn(document);

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        var result = proceedingsService.createProceedings(proceedingsDTO, true);

        // Then
        verify(multilingualContentService, times(6)).getMultilingualContent(any());
        verify(personContributionService).setPersonDocumentContributionsForDocument(eq(document),
            eq(proceedingsDTO));
        verify(proceedingsJPAService).save(eq(document));
    }

    @Test
    public void shouldEditProceedings() {
        // Given
        var proceedingsId = 1;
        var proceedingsDTO = new ProceedingsDTO();
        proceedingsDTO.setLanguageIds(new ArrayList<>());
        proceedingsDTO.setDocumentDate("2025");
        var proceedingsToUpdate = new Proceedings();
        proceedingsToUpdate.setApproveStatus(ApproveStatus.REQUESTED);

        when(proceedingsJPAService.findOne(proceedingsId)).thenReturn(proceedingsToUpdate);
        when(proceedingsJPAService.save(any())).thenReturn(proceedingsToUpdate);

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        proceedingsService.updateProceedings(proceedingsId, proceedingsDTO);

        // Then
        verify(proceedingsJPAService).findOne(eq(proceedingsId));
        verify(personContributionService).setPersonDocumentContributionsForDocument(
            eq(proceedingsToUpdate), eq(proceedingsDTO));
        verify(proceedingsPublicationRepository).setDateToAggregatedPublications(any(), any());
        verify(indexBulkUpdateService).setYearForAggregatedRecord(any(), any(), any());
    }

    @Test
    public void shouldReturnProceedingsWhenOldIdExists() {
        // Given
        var proceedingsId = 123;
        var expected = new Proceedings();
        when(documentRepository.findDocumentByOldIdsContains(proceedingsId)).thenReturn(
            Optional.of(123));
        when(documentRepository.findById(123)).thenReturn(
            Optional.of(expected));

        // When
        var actual = proceedingsService.findDocumentByOldId(proceedingsId);

        // Then
        assertEquals(expected, actual);
    }

    @Test
    public void shouldReturnNullWhenProceedingsDoesNotExist() {
        // Given
        var proceedingsId = 123;
        when(documentRepository.findDocumentByOldIdsContains(proceedingsId)).thenReturn(
            Optional.empty());

        // When
        var actual = proceedingsService.findDocumentByOldId(proceedingsId);

        // Then
        assertNull(actual);
    }

    @Test
    public void shouldReindexProceedings() {
        // Given
        var proceedings1 = new Proceedings();
        var proceedings2 = new Proceedings();
        var proceedings3 = new Proceedings();
        var proceedings = Arrays.asList(proceedings1, proceedings2, proceedings3);
        var page1 =
            new PageImpl<>(proceedings.subList(0, 2), PageRequest.of(0, 10), proceedings.size());
        var page2 =
            new PageImpl<>(proceedings.subList(2, 3), PageRequest.of(1, 10), proceedings.size());

        when(proceedingsJPAService.findAll(any(PageRequest.class))).thenReturn(page1, page2);

        // When
        proceedingsService.reindexProceedings();

        // Then
        verify(proceedingsJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }

    @Test
    void shouldForceDeleteProceedings() {
        // Given
        var proceedingsId = 1;

        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            proceedingsId))
            .thenReturn(Optional.empty());

        // When
        proceedingsService.forceDeleteProceedings(proceedingsId);

        // Then
        verify(proceedingsRepository).deleteAllPublicationsInProceedings(proceedingsId);
        verify(proceedingsJPAService).delete(proceedingsId);
        verify(documentPublicationIndexRepository).deleteByProceedingsId(proceedingsId);
        verify(documentPublicationIndexRepository, never()).delete(any());
    }

    @Test
    void shouldReturnTrueWhenEISBNExists() {
        // given
        var identifier = "1234-5678";
        var publicationSeriesId = 1;
        when(proceedingsRepository.existsByeISBN(identifier, publicationSeriesId)).thenReturn(
            true);
        when(proceedingsRepository.existsByPrintISBN(identifier, publicationSeriesId)).thenReturn(
            false);

        // when
        var result = proceedingsService.isIdentifierInUse(identifier, publicationSeriesId);

        // then
        assertTrue(result);
        verify(proceedingsRepository, atMostOnce()).existsByeISBN(identifier,
            publicationSeriesId);
        verify(proceedingsRepository, atMostOnce()).existsByPrintISBN(identifier,
            publicationSeriesId);
    }

    @Test
    void shouldReturnTrueWhenPrintISBNExists() {
        // given
        var identifier = "1234-5678";
        var publicationSeriesId = 1;
        when(proceedingsRepository.existsByeISBN(identifier, publicationSeriesId)).thenReturn(
            false);
        when(proceedingsRepository.existsByPrintISBN(identifier, publicationSeriesId)).thenReturn(
            true);

        // when
        var result = proceedingsService.isIdentifierInUse(identifier, publicationSeriesId);

        // then
        assertTrue(result);
        verify(proceedingsRepository).existsByeISBN(identifier, publicationSeriesId);
        verify(proceedingsRepository).existsByPrintISBN(identifier, publicationSeriesId);
    }

    @Test
    void shouldReturnTrueWhenBothISBNsExist() {
        // given
        var identifier = "1234-5678";
        var publicationSeriesId = 1;
        when(proceedingsRepository.existsByeISBN(identifier, publicationSeriesId)).thenReturn(
            true);
        when(proceedingsRepository.existsByPrintISBN(identifier, publicationSeriesId)).thenReturn(
            true);

        // when
        var result = proceedingsService.isIdentifierInUse(identifier, publicationSeriesId);

        // then
        assertTrue(result);
        verify(proceedingsRepository, atMostOnce()).existsByeISBN(identifier,
            publicationSeriesId);
        verify(proceedingsRepository, atMostOnce()).existsByPrintISBN(identifier,
            publicationSeriesId);
    }

    @Test
    void shouldReturnFalseWhenNeitherISBNExists() {

        // given
        var identifier = "1234-5678";
        var publicationSeriesId = 1;
        when(proceedingsRepository.existsByeISBN(identifier, publicationSeriesId)).thenReturn(
            false);
        when(proceedingsRepository.existsByPrintISBN(identifier, publicationSeriesId)).thenReturn(
            false);

        // when
        var result = proceedingsService.isIdentifierInUse(identifier, publicationSeriesId);

        // then
        assertFalse(result);
        verify(proceedingsRepository).existsByeISBN(identifier, publicationSeriesId);
        verify(proceedingsRepository).existsByPrintISBN(identifier, publicationSeriesId);
    }

    @Test
    void shouldReturnRawProceedings() {
        // Given
        var proceedingsId = 123;
        var expected = new Proceedings();
        expected.setId(proceedingsId);
        when(proceedingsRepository.findRaw(proceedingsId)).thenReturn(Optional.of(expected));

        // When
        var actualProceedings = proceedingsService.findRaw(proceedingsId);

        // Then
        assertEquals(expected, actualProceedings);
        verify(proceedingsRepository).findRaw(proceedingsId);
    }

    @Test
    void shouldThrowsNotFoundExceptionWhenProceedingsDoesNotExist() {
        // Given
        var proceedingsId = 123;
        when(proceedingsRepository.findRaw(proceedingsId)).thenReturn(Optional.empty());

        // When & Then
        var exception = assertThrows(NotFoundException.class,
            () -> proceedingsService.findRaw(proceedingsId));

        assertEquals("Proceedings with given ID does not exist.", exception.getMessage());
        verify(proceedingsRepository).findRaw(proceedingsId);
    }

    @Test
    void shouldReturnTrueIfEIsbnExistsInProceedings() {
        // Given
        var identifier = "12345";
        var proceedingsId = 1;
        doReturn(true).when(proceedingsRepository).existsByeISBN(identifier, proceedingsId);

        // When
        var result = proceedingsService.isIdentifierInUse(identifier, proceedingsId);

        // Then
        assertTrue(result);
        verify(proceedingsRepository).existsByeISBN(identifier, proceedingsId);
        verify(proceedingsRepository, never()).existsByPrintISBN(any(), any());
    }

    @Test
    void shouldReturnTrueIfPrintIsbnExistsInProceedings() {
        // Given
        var identifier = "67890";
        var proceedingsId = 2;
        doReturn(false).when(proceedingsRepository).existsByeISBN(identifier, proceedingsId);
        doReturn(true).when(proceedingsRepository).existsByPrintISBN(identifier, proceedingsId);

        // When
        var result = proceedingsService.isIdentifierInUse(identifier, proceedingsId);

        // Then
        assertTrue(result);
        verify(proceedingsRepository).existsByeISBN(identifier, proceedingsId);
        verify(proceedingsRepository).existsByPrintISBN(identifier, proceedingsId);
    }

    @Test
    void shouldReturnTrueIfSuperMethodReturnsTrueInProceedings() {
        // Given
        var identifier = "abcde";
        var proceedingsId = 3;
        doReturn(false).when(proceedingsRepository).existsByeISBN(identifier, proceedingsId);
        doReturn(false).when(proceedingsRepository).existsByPrintISBN(identifier, proceedingsId);

        // mock super method
        doReturn(false).when(documentRepository).existsByDoi(identifier, proceedingsId);
        doReturn(false).when(documentRepository).existsByScopusId(identifier, proceedingsId);
        doReturn(true).when(documentRepository).existsByOpenAlexId(identifier, proceedingsId);
        doReturn(false).when(documentRepository).existsByWebOfScienceId(identifier, proceedingsId);

        // When
        var result = proceedingsService.isIdentifierInUse(identifier, proceedingsId);

        // Then
        assertTrue(result);
        verify(proceedingsRepository).existsByeISBN(identifier, proceedingsId);
        verify(proceedingsRepository).existsByPrintISBN(identifier, proceedingsId);
    }

    @Test
    void shouldReturnFalseIfNoIdentifierExistsInProceedings() {
        // Given
        var identifier = "xyz";
        var proceedingsId = 4;
        doReturn(false).when(proceedingsRepository).existsByeISBN(identifier, proceedingsId);
        doReturn(false).when(proceedingsRepository).existsByPrintISBN(identifier, proceedingsId);

        doReturn(false).when(documentRepository).existsByDoi(identifier, proceedingsId);
        doReturn(false).when(documentRepository).existsByScopusId(identifier, proceedingsId);
        doReturn(false).when(documentRepository).existsByOpenAlexId(identifier, proceedingsId);
        doReturn(false).when(documentRepository).existsByWebOfScienceId(identifier, proceedingsId);

        // When
        var result = proceedingsService.isIdentifierInUse(identifier, proceedingsId);

        // Then
        assertFalse(result);
        verify(proceedingsRepository).existsByeISBN(identifier, proceedingsId);
        verify(proceedingsRepository).existsByPrintISBN(identifier, proceedingsId);
    }

    @Test
    void shouldReturnNullWhenBothIsbnsAreBlank() {
        // Given
        var eIsbn = " ";
        String printIsbn = null;

        // When
        var result = proceedingsService.findProceedingsByIsbn(eIsbn, printIsbn);

        // Then
        assertNull(result);
    }

    @Test
    void shouldUsePrintIsbnWhenEIsbnIsBlank() {
        // Given
        var eIsbn = " ";
        var printIsbn = "12345";
        var proceedings = new Proceedings();
        when(proceedingsRepository.findByISBN(printIsbn, printIsbn)).thenReturn(
            new ArrayList<>(List.of(proceedings)));

        // When
        var result = proceedingsService.findProceedingsByIsbn(eIsbn, printIsbn);

        // Then
        assertEquals(proceedings, result);
    }

    @Test
    void shouldUseEIsbnWhenPrintIsbnIsBlank() {
        // Given
        var eIsbn = "67890";
        var printIsbn = "";
        var proceedings = new Proceedings();
        when(proceedingsRepository.findByISBN(eIsbn, eIsbn)).thenReturn(
            new ArrayList<>(List.of(proceedings)));

        // When
        var result = proceedingsService.findProceedingsByIsbn(eIsbn, printIsbn);

        // Then
        assertEquals(proceedings, result);
    }

    @Test
    void shouldReturnNullIfNoProceedingsFound() {
        // Given
        var eIsbn = "11111";
        var printIsbn = "22222";
        when(proceedingsRepository.findByISBN(eIsbn, printIsbn)).thenReturn(new ArrayList<>());

        // When
        var result = proceedingsService.findProceedingsByIsbn(eIsbn, printIsbn);

        // Then
        assertNull(result);
    }

    @Test
    void shouldAddOldIdToProceedings() {
        // Given
        var id = 1;
        var oldId = 99;
        var proceedings = new Proceedings();
        proceedings.setOldIds(new HashSet<>());
        when(documentRepository.findById(id)).thenReturn(Optional.of(proceedings));

        // When
        proceedingsService.addOldId(id, oldId);

        // Then
        assertTrue(proceedings.getOldIds().contains(oldId));
        verify(documentRepository).save(proceedings);
    }
}
