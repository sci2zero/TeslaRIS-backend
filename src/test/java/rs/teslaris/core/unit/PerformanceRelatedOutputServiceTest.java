package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.PerformanceRelatedOutput;
import rs.teslaris.core.model.document.PerformanceRelatedOutputType;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.PerformanceRelatedOutputRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.document.PerformanceRelatedOutputServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.PerformanceRelatedOutputJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.DocumentLookupService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitOutputConfigurationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;

@SpringBootTest
public class PerformanceRelatedOutputServiceTest {

    @Mock
    private PerformanceRelatedOutputJPAServiceImpl performanceRelatedOutputJPAService;

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private PerformanceRelatedOutputRepository performanceRelatedOutputRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private SearchService<DocumentPublicationIndex> searchService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentFileService documentFileService;

    @Mock
    private CitationService citationService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private ExpressionTransformer expressionTransformer;

    @Mock
    private EventService eventService;

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private SearchFieldsLoader searchFieldsLoader;

    @Mock
    private OrganisationUnitTrustConfigurationService organisationUnitTrustConfigurationService;

    @Mock
    private InvolvementRepository involvementRepository;

    @Mock
    private OrganisationUnitOutputConfigurationService organisationUnitOutputConfigurationService;

    @Mock
    private DocumentLookupService documentLookupService;

    @Mock
    private CountryService countryService;

    @InjectMocks
    private PerformanceRelatedOutputServiceImpl performanceRelatedOutputService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(performanceRelatedOutputService, "documentApprovedByDefault",
            true);
    }

    @Test
    public void shouldFindPerformanceRelatedOutputById() {
        // Given
        var outputId = 1;
        var output = new PerformanceRelatedOutput();
        output.setId(outputId);

        when(performanceRelatedOutputJPAService.findOne(outputId)).thenReturn(output);

        // When
        var result = performanceRelatedOutputService.findPerformanceRelatedOutputById(outputId);

        // Then
        assertNotNull(result);
        assertEquals(outputId, result.getId());
        verify(performanceRelatedOutputJPAService).findOne(eq(outputId));
    }

    @Test
    public void shouldThrowNotFoundWhenFindingNonExistentOutput() {
        // Given
        var outputId = 999;
        when(performanceRelatedOutputJPAService.findOne(outputId)).thenThrow(
            new NotFoundException("Not found"));

        // When / Then
        assertThrows(NotFoundException.class, () ->
            performanceRelatedOutputService.findPerformanceRelatedOutputById(outputId));
    }

    @Test
    public void shouldReadPerformanceRelatedOutputById() {
        // Given
        var outputId = 1;
        var output = new PerformanceRelatedOutput();
        output.setId(outputId);
        output.setApproveStatus(ApproveStatus.APPROVED);

        when(performanceRelatedOutputJPAService.findOne(outputId)).thenReturn(output);

        // When
        var result = performanceRelatedOutputService.readPerformanceRelatedOutputById(outputId);

        // Then
        assertNotNull(result);
        verify(performanceRelatedOutputJPAService).findOne(eq(outputId));
    }

    @Test
    public void shouldThrowNotFoundWhenReadingNonApprovedOutputAndUserNotLoggedIn() {
        // Given
        var outputId = 1;
        var output = new PerformanceRelatedOutput();
        output.setApproveStatus(ApproveStatus.REQUESTED);

        when(performanceRelatedOutputJPAService.findOne(outputId)).thenReturn(output);

        // When / Then
        assertThrows(NotFoundException.class, () ->
            performanceRelatedOutputService.readPerformanceRelatedOutputById(outputId));
    }

    @Test
    public void shouldCreatePerformanceRelatedOutput() {
        // Given
        var dto = new PerformanceRelatedOutputDTO();
        dto.setDocumentDate("2023-07-09");
        dto.setType(PerformanceRelatedOutputType.ART_PERFORMANCE);

        var output = new PerformanceRelatedOutput();
        output.setId(1);

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(performanceRelatedOutputJPAService.save(any())).thenReturn(output);

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        var result = performanceRelatedOutputService.createPerformanceRelatedOutput(dto, true);

        // Then
        assertNotNull(result);
        verify(multilingualContentService, times(13)).getMultilingualContent(any());
        verify(personContributionService).setPersonDocumentContributionsForDocument(any(), eq(dto));
        verify(performanceRelatedOutputJPAService).save(any());
    }

    @Test
    public void shouldCreatePerformanceRelatedOutputWithoutIndexing() {
        // Given
        var dto = new PerformanceRelatedOutputDTO();
        dto.setDocumentDate("2023-07-09");

        var output = new PerformanceRelatedOutput();
        output.setId(1);

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(performanceRelatedOutputJPAService.save(any())).thenReturn(output);

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        var result = performanceRelatedOutputService.createPerformanceRelatedOutput(dto, false);

        // Then
        assertNotNull(result);
        verify(documentPublicationIndexRepository, never()).save(any());
    }

    @Test
    public void shouldEditPerformanceRelatedOutput() {
        // Given
        var outputId = 1;
        var dto = new PerformanceRelatedOutputDTO();
        dto.setDocumentDate("2024");
        dto.setType(PerformanceRelatedOutputType.MUSICAL_PERFORMANCE);

        var outputToUpdate = new PerformanceRelatedOutput();
        outputToUpdate.setApproveStatus(ApproveStatus.REQUESTED);
        outputToUpdate.setDocumentDate("2023");
        outputToUpdate.setContributors(new HashSet<>());
        outputToUpdate.setLanguages(new HashSet<>());

        var index = new DocumentPublicationIndex();

        when(performanceRelatedOutputJPAService.findOne(outputId)).thenReturn(outputToUpdate);
        when(performanceRelatedOutputJPAService.save(any())).thenReturn(
            new PerformanceRelatedOutput());
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(outputId))
            .thenReturn(Optional.of(index));

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        performanceRelatedOutputService.editPerformanceRelatedOutput(outputId, dto);

        // Then
        verify(performanceRelatedOutputJPAService).findOne(eq(outputId));
        verify(personContributionService).setPersonDocumentContributionsForDocument(
            eq(outputToUpdate), eq(dto));
        verify(performanceRelatedOutputJPAService).save(any());
    }

    @Test
    public void shouldDeletePerformanceRelatedOutput() {
        // Given
        var outputId = 1;
        var outputToDelete = new PerformanceRelatedOutput();
        outputToDelete.setId(outputId);
        outputToDelete.setProofs(new HashSet<>());
        outputToDelete.setFileItems(new HashSet<>());

        var index = new DocumentPublicationIndex();
        index.setDatabaseId(outputId);

        when(performanceRelatedOutputJPAService.findOne(outputId)).thenReturn(outputToDelete);
        when(performanceRelatedOutputService.findOne(outputId)).thenReturn(outputToDelete);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(outputId))
            .thenReturn(Optional.of(index));

        // When
        performanceRelatedOutputService.deletePerformanceRelatedOutput(outputId);

        // Then
        verify(performanceRelatedOutputJPAService).delete(eq(outputId));
        verify(documentPublicationIndexRepository).delete(eq(index));
    }

    @Test
    public void shouldReindexPerformanceRelatedOutputs() {
        // Given
        var output = new PerformanceRelatedOutput();
        output.setDocumentDate("2024");
        var outputs = List.of(output);
        var page1 = new PageImpl<>(outputs.subList(0, 1), PageRequest.of(0, 10), outputs.size());

        when(performanceRelatedOutputJPAService.findAll(any(PageRequest.class))).thenReturn(page1);

        // When
        performanceRelatedOutputService.reindexPerformanceRelatedOutputs();

        // Then
        verify(performanceRelatedOutputJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }

    @Test
    public void shouldIndexPerformanceRelatedOutput() {
        // Given
        var output = new PerformanceRelatedOutput();
        output.setId(1);
        output.setDocumentDate("2024");

        var index = new DocumentPublicationIndex();

        when(citationService.craftCitationInGivenStyle(anyString(), any(), any()))
            .thenReturn("APA Citation");

        // When
        performanceRelatedOutputService.indexPerformanceRelatedOutput(output);

        // Then
        verify(documentPublicationIndexRepository).findDocumentPublicationIndexByDatabaseId(eq(1));
        verify(documentPublicationIndexRepository, times(2)).save(
            any(DocumentPublicationIndex.class));
    }

    @Test
    void shouldThrowNotFoundWhenPerformanceRelatedOutputDoesNotExist() {
        // Given
        var oldId = 123;
        when(performanceRelatedOutputRepository.findPerformanceRelatedOutputByOldIdsContains(oldId))
            .thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () ->
            performanceRelatedOutputService.readPerformanceRelatedOutputByOldId(oldId));
        verify(performanceRelatedOutputRepository).findPerformanceRelatedOutputByOldIdsContains(
            oldId);
    }

    @Test
    void shouldThrowNotFoundWhenPerformanceRelatedOutputIsNotApproved() {
        // Given
        var oldId = 456;
        var output = new PerformanceRelatedOutput();
        output.setApproveStatus(ApproveStatus.REQUESTED);
        when(performanceRelatedOutputRepository.findPerformanceRelatedOutputByOldIdsContains(oldId))
            .thenReturn(Optional.of(output));

        // When / Then
        assertThrows(NotFoundException.class, () ->
            performanceRelatedOutputService.readPerformanceRelatedOutputByOldId(oldId));
        verify(performanceRelatedOutputRepository).findPerformanceRelatedOutputByOldIdsContains(
            oldId);
    }

    @Test
    void shouldReturnDtoWhenPerformanceRelatedOutputIsApproved() {
        // Given
        var oldId = 789;
        var output = new PerformanceRelatedOutput();
        output.setApproveStatus(ApproveStatus.APPROVED);

        when(performanceRelatedOutputRepository.findPerformanceRelatedOutputByOldIdsContains(oldId))
            .thenReturn(Optional.of(output));

        // When
        var result = performanceRelatedOutputService.readPerformanceRelatedOutputByOldId(oldId);

        // Then
        assertNotNull(result);
        verify(performanceRelatedOutputRepository).findPerformanceRelatedOutputByOldIdsContains(
            oldId);
    }
}
