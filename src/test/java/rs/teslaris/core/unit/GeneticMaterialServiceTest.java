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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.document.GeneticMaterialDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.GeneticMaterial;
import rs.teslaris.core.model.document.GeneticMaterialType;
import rs.teslaris.core.model.document.Publisher;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.document.GeneticMaterialServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.GeneticMaterialJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitOutputConfigurationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchFieldsLoader;

@SpringBootTest
public class GeneticMaterialServiceTest {

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
    private PersonContributionService personContributionService;

    @Mock
    private PublisherService publisherService;

    @Mock
    private GeneticMaterialJPAServiceImpl geneticMaterialJPAService;

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
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private ExpressionTransformer expressionTransformer;

    @Mock
    private EventService eventService;

    @InjectMocks
    private GeneticMaterialServiceImpl geneticMaterialService;

    private static Stream<Arguments> provideGeneticMaterialTypes() {
        return Stream.of(
            Arguments.of(GeneticMaterialType.GENOTYPE),
            Arguments.of(GeneticMaterialType.RACE),
            Arguments.of(GeneticMaterialType.STRAIN),
            Arguments.of(GeneticMaterialType.VARIETY),
            Arguments.of(GeneticMaterialType.OTHER)
        );
    }

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(geneticMaterialService, "documentApprovedByDefault", true);
    }

    @Test
    public void shouldFindGeneticMaterialById() {
        // Given
        var geneticMaterialId = 1;
        var expectedMaterial = new GeneticMaterial();
        expectedMaterial.setId(geneticMaterialId);

        when(geneticMaterialJPAService.findOne(geneticMaterialId)).thenReturn(expectedMaterial);

        // When
        GeneticMaterial result = geneticMaterialService.findGeneticMaterialById(geneticMaterialId);

        // Then
        verify(geneticMaterialJPAService).findOne(eq(geneticMaterialId));
        assertNotNull(result);
        assertEquals(geneticMaterialId, result.getId());
    }

    @Test
    public void shouldReadGeneticMaterialById() {
        // Given
        Integer geneticMaterialId = 1;
        GeneticMaterial geneticMaterial = new GeneticMaterial();
        geneticMaterial.setId(geneticMaterialId);
        geneticMaterial.setApproveStatus(ApproveStatus.APPROVED);

        when(geneticMaterialJPAService.findOne(geneticMaterialId)).thenReturn(geneticMaterial);

        // When
        var result = geneticMaterialService.readGeneticMaterialById(geneticMaterialId);

        // Then
        verify(geneticMaterialJPAService).findOne(eq(geneticMaterialId));
        assertNotNull(result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenReadingNonExistentGeneticMaterial() {
        // Given
        Integer geneticMaterialId = 999;
        when(geneticMaterialJPAService.findOne(geneticMaterialId))
            .thenThrow(new NotFoundException("Not found"));

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            geneticMaterialService.readGeneticMaterialById(geneticMaterialId);
        });

        verify(geneticMaterialJPAService).findOne(eq(geneticMaterialId));
        verify(documentPublicationIndexRepository).findDocumentPublicationIndexByDatabaseIdAndType(
            eq(geneticMaterialId), eq(DocumentPublicationType.MATERIAL_PRODUCT.name()));
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenReadingUnapprovedGeneticMaterialAndUserNotLoggedIn() {
        // Given
        Integer geneticMaterialId = 1;
        GeneticMaterial geneticMaterial = new GeneticMaterial();
        geneticMaterial.setId(geneticMaterialId);
        geneticMaterial.setApproveStatus(ApproveStatus.REQUESTED);

        when(geneticMaterialJPAService.findOne(geneticMaterialId)).thenReturn(geneticMaterial);

        SecurityContextHolder.clearContext();

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            geneticMaterialService.readGeneticMaterialById(geneticMaterialId);
        });
    }

    @Test
    public void shouldCreateGeneticMaterialWithIndexing() {
        // Given
        var dto = new GeneticMaterialDTO();
        dto.setDocumentDate("2020-03-02");
        dto.setInternalNumber("GM-001");
        dto.setGeneticMaterialType(GeneticMaterialType.GENOTYPE);
        dto.setAuthorReprint(false);
        dto.setPublisherId(1);

        var geneticMaterial = new GeneticMaterial();
        geneticMaterial.setId(1);

        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(new rs.teslaris.core.model.commontypes.MultiLingualContent()));
        when(geneticMaterialJPAService.save(any())).thenReturn(geneticMaterial);
        when(publisherService.findOne(1)).thenReturn(new Publisher());

        setupSecurityContext();

        // When
        var result = geneticMaterialService.createGeneticMaterial(dto, true);

        // Then
        verify(multilingualContentService, times(5))
            .getMultilingualContent(any());
        verify(personContributionService).setPersonDocumentContributionsForDocument(
            any(), any());
        verify(geneticMaterialJPAService).save(any(GeneticMaterial.class));
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
        assertNotNull(result);
    }

    @Test
    public void shouldCreateGeneticMaterialWithAuthorReprint() {
        // Given
        var dto = new GeneticMaterialDTO();
        dto.setDocumentDate("2020-03-02");
        dto.setAuthorReprint(true);

        var geneticMaterial = new GeneticMaterial();
        geneticMaterial.setId(1);

        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(new rs.teslaris.core.model.commontypes.MultiLingualContent()));
        when(geneticMaterialJPAService.save(any())).thenReturn(geneticMaterial);

        setupSecurityContext();

        // When
        var result = geneticMaterialService.createGeneticMaterial(dto, true);

        // Then
        verify(geneticMaterialJPAService).save(any(GeneticMaterial.class));
        verify(publisherService, never()).findOne(any());
    }

    @Test
    public void shouldCreateGeneticMaterialWithoutIndexing() {
        // Given
        var dto = new GeneticMaterialDTO();
        dto.setDocumentDate("2020-03-02");

        var geneticMaterial = new GeneticMaterial();
        geneticMaterial.setId(1);

        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(new rs.teslaris.core.model.commontypes.MultiLingualContent()));
        when(geneticMaterialJPAService.save(any())).thenReturn(geneticMaterial);

        setupSecurityContext();

        // When
        var result = geneticMaterialService.createGeneticMaterial(dto, false);

        // Then
        verify(geneticMaterialJPAService).save(any(GeneticMaterial.class));
        verify(documentPublicationIndexRepository, never()).save(
            any(DocumentPublicationIndex.class));
    }

    @Test
    public void shouldEditGeneticMaterial() {
        // Given
        Integer geneticMaterialId = 1;
        var dto = new GeneticMaterialDTO();
        dto.setDocumentDate("2024-01-01");
        dto.setInternalNumber("GM-002");
        dto.setGeneticMaterialType(GeneticMaterialType.GENOTYPE);
        dto.setAuthorReprint(true);

        var existingMaterial = new GeneticMaterial();
        existingMaterial.setId(geneticMaterialId);
        existingMaterial.setApproveStatus(ApproveStatus.REQUESTED);
        existingMaterial.setDocumentDate("2023");

        var index = new DocumentPublicationIndex();
        index.setDatabaseId(geneticMaterialId);

        when(geneticMaterialJPAService.findOne(geneticMaterialId)).thenReturn(existingMaterial);
        when(geneticMaterialJPAService.save(any())).thenReturn(existingMaterial);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            geneticMaterialId))
            .thenReturn(Optional.of(index));

        setupSecurityContext();

        // When
        geneticMaterialService.editGeneticMaterial(geneticMaterialId, dto);

        // Then
        verify(geneticMaterialJPAService).findOne(eq(geneticMaterialId));
        verify(personContributionService).setPersonDocumentContributionsForDocument(
            eq(existingMaterial), eq(dto));
        verify(geneticMaterialJPAService).save(any(GeneticMaterial.class));
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }

    @Test
    public void shouldEditGeneticMaterialWithNewIndex() {
        // Given
        Integer geneticMaterialId = 1;
        var dto = new GeneticMaterialDTO();
        dto.setDocumentDate("2024-01-01");

        var existingMaterial = new GeneticMaterial();
        existingMaterial.setId(geneticMaterialId);
        existingMaterial.setApproveStatus(ApproveStatus.REQUESTED);

        when(geneticMaterialJPAService.findOne(geneticMaterialId)).thenReturn(existingMaterial);
        when(geneticMaterialJPAService.save(any())).thenReturn(existingMaterial);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            geneticMaterialId))
            .thenReturn(Optional.empty());

        setupSecurityContext();

        // When
        geneticMaterialService.editGeneticMaterial(geneticMaterialId, dto);

        // Then
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }

    @Test
    public void shouldDeleteGeneticMaterial() {
        // Given
        Integer geneticMaterialId = 1;
        var geneticMaterialToDelete = new GeneticMaterial();
        geneticMaterialToDelete.setId(geneticMaterialId);

        var index = new DocumentPublicationIndex();
        index.setDatabaseId(geneticMaterialId);

        when(geneticMaterialJPAService.findOne(geneticMaterialId))
            .thenReturn(geneticMaterialToDelete);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            geneticMaterialId))
            .thenReturn(Optional.of(index));

        // When
        geneticMaterialService.deleteGeneticMaterial(geneticMaterialId);

        // Then
        verify(geneticMaterialJPAService).findOne(eq(geneticMaterialId));
        verify(geneticMaterialJPAService).delete(eq(geneticMaterialId));
        verify(documentPublicationIndexRepository).delete(eq(index));
    }

    @Test
    public void shouldReindexGeneticMaterials() {
        // Given
        var geneticMaterial = new GeneticMaterial();
        geneticMaterial.setId(1);
        geneticMaterial.setDocumentDate("2024");
        var geneticMaterials = List.of(geneticMaterial);
        var page =
            new PageImpl<>(geneticMaterials, PageRequest.of(0, 100), geneticMaterials.size());

        when(geneticMaterialJPAService.findAll(any(PageRequest.class))).thenReturn(page);

        // When
        geneticMaterialService.reindexGeneticMaterials();

        // Then
        verify(geneticMaterialJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }

    @ParameterizedTest
    @MethodSource("provideGeneticMaterialTypes")
    public void shouldSetGeneticMaterialTypeWhenCreating(GeneticMaterialType type) {
        // Given
        var dto = new GeneticMaterialDTO();
        dto.setDocumentDate("2020-03-02");
        dto.setGeneticMaterialType(type);

        var geneticMaterial = new GeneticMaterial();
        geneticMaterial.setId(1);
        geneticMaterial.setGeneticMaterialType(type);

        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(new rs.teslaris.core.model.commontypes.MultiLingualContent()));
        when(geneticMaterialJPAService.save(any())).thenReturn(geneticMaterial);

        setupSecurityContext();

        // When
        var result = geneticMaterialService.createGeneticMaterial(dto, false);

        // Then
        verify(geneticMaterialJPAService).save(any(GeneticMaterial.class));
        assertEquals(type, result.getGeneticMaterialType());
    }

    @Test
    public void shouldIndexGeneticMaterial() {
        // Given
        var geneticMaterial = new GeneticMaterial();
        geneticMaterial.setId(1);
        geneticMaterial.setAuthorReprint(true);

        var index = new DocumentPublicationIndex();
        index.setDatabaseId(1);

        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(1))
            .thenReturn(Optional.of(index));
        when(citationService.craftCitationInGivenStyle(any(), any(), any()))
            .thenReturn("Test Citation");

        // When
        geneticMaterialService.indexGeneticMaterial(geneticMaterial);

        // Then
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
        verify(citationService).craftCitationInGivenStyle(eq("apa"), any(), eq("EN"));
    }

    @Test
    public void shouldIndexGeneticMaterialWithPublisher() {
        // Given
        var geneticMaterial = new GeneticMaterial();
        geneticMaterial.setId(1);
        geneticMaterial.setAuthorReprint(false);
        var publisher = new Publisher();
        publisher.setId(5);
        geneticMaterial.setPublisher(publisher);

        var index = new DocumentPublicationIndex();

        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(1))
            .thenReturn(Optional.of(index));
        when(citationService.craftCitationInGivenStyle(any(), any(), any()))
            .thenReturn("Test Citation");

        // When
        geneticMaterialService.indexGeneticMaterial(geneticMaterial);

        // Then
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }

    @Test
    public void shouldCallSuperServiceMethodWhenDeleting() {
        // Given
        Integer geneticMaterialId = 1;
        var geneticMaterialToDelete = new GeneticMaterial();
        geneticMaterialToDelete.setId(geneticMaterialId);

        var index = new DocumentPublicationIndex();
        index.setDatabaseId(geneticMaterialId);

        when(geneticMaterialJPAService.findOne(geneticMaterialId))
            .thenReturn(geneticMaterialToDelete);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            geneticMaterialId))
            .thenReturn(Optional.of(index));

        // When
        geneticMaterialService.deleteGeneticMaterial(geneticMaterialId);

        // Then
        verify(geneticMaterialJPAService).delete(eq(geneticMaterialId));
        verify(documentRepository).save(any()); // super.save() call
    }

    @Test
    public void shouldHandleNullPublisherId() {
        // Given
        var dto = new GeneticMaterialDTO();
        dto.setDocumentDate("2020-03-02");
        dto.setAuthorReprint(false);
        // No publisherId set

        var geneticMaterial = new GeneticMaterial();
        geneticMaterial.setId(1);

        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(new rs.teslaris.core.model.commontypes.MultiLingualContent()));
        when(geneticMaterialJPAService.save(any())).thenReturn(geneticMaterial);

        setupSecurityContext();

        // When
        var result = geneticMaterialService.createGeneticMaterial(dto, false);

        // Then
        verify(geneticMaterialJPAService).save(any(GeneticMaterial.class));
        verify(publisherService, never()).findOne(any());
        assertNotNull(result);
    }

    @Test
    public void shouldSetInternalNumberWhenCreating() {
        // Given
        var dto = new GeneticMaterialDTO();
        dto.setDocumentDate("2020-03-02");
        dto.setInternalNumber("GM-12345");

        var geneticMaterial = new GeneticMaterial();
        geneticMaterial.setId(1);
        geneticMaterial.setInternalNumber(dto.getInternalNumber());

        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(new rs.teslaris.core.model.commontypes.MultiLingualContent()));
        when(geneticMaterialJPAService.save(any())).thenReturn(geneticMaterial);

        setupSecurityContext();

        // When
        var result = geneticMaterialService.createGeneticMaterial(dto, false);

        // Then
        verify(geneticMaterialJPAService).save(any(GeneticMaterial.class));
        assertEquals("GM-12345", result.getInternalNumber());
    }

    private void setupSecurityContext() {
        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
