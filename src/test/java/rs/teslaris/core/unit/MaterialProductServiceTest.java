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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.document.MaterialProductDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.MaterialProduct;
import rs.teslaris.core.model.document.MaterialProductType;
import rs.teslaris.core.model.document.Publisher;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.MaterialProductServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.MaterialProductJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class MaterialProductServiceTest {

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private SearchService<DocumentPublicationIndex> searchService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private rs.teslaris.core.repository.document.DocumentRepository documentRepository;

    @Mock
    private DocumentFileService documentFileService;

    @Mock
    private CitationService citationService;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private PublisherService publisherService;

    @Mock
    private MaterialProductJPAServiceImpl materialProductJPAService;

    @Mock
    private CommissionRepository commissionRepository;

    @InjectMocks
    private MaterialProductServiceImpl materialProductService;

    private static Stream<Arguments> provideMaterialProductTypes() {
        return Stream.of(
            Arguments.of(MaterialProductType.INDUSTRIAL_PRODUCT),
            Arguments.of(MaterialProductType.DERIVATIVE_WORKS),
            Arguments.of(MaterialProductType.INFRASTRUCTURE_OBJECT),
            Arguments.of(MaterialProductType.PROTOTYPE),
            Arguments.of(MaterialProductType.OTHER)
        );
    }

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(materialProductService, "documentApprovedByDefault", true);
    }

    @Test
    public void shouldFindMaterialProductById() {
        // Given
        var materialProductId = 1;
        var expectedProduct = new MaterialProduct();
        expectedProduct.setId(materialProductId);

        when(materialProductJPAService.findOne(materialProductId)).thenReturn(expectedProduct);

        // When
        MaterialProduct result = materialProductService.findMaterialProductById(materialProductId);

        // Then
        verify(materialProductJPAService).findOne(eq(materialProductId));
        assertNotNull(result);
        assertEquals(materialProductId, result.getId());
    }

    @Test
    public void shouldReadMaterialProductById() {
        // Given
        Integer materialProductId = 1;
        MaterialProduct materialProduct = new MaterialProduct();
        materialProduct.setId(materialProductId);
        materialProduct.setApproveStatus(ApproveStatus.APPROVED);

        when(materialProductJPAService.findOne(materialProductId)).thenReturn(materialProduct);

        // When
        var result = materialProductService.readMaterialProductById(materialProductId);

        // Then
        verify(materialProductJPAService).findOne(eq(materialProductId));
        assertNotNull(result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenReadingNonExistentMaterialProduct() {
        // Given
        Integer materialProductId = 999;
        when(materialProductJPAService.findOne(materialProductId))
            .thenThrow(new NotFoundException("Not found"));

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            materialProductService.readMaterialProductById(materialProductId);
        });

        verify(materialProductJPAService).findOne(eq(materialProductId));
        verify(documentPublicationIndexRepository).findDocumentPublicationIndexByDatabaseIdAndType(
            eq(materialProductId), eq(DocumentPublicationType.MATERIAL_PRODUCT.name()));
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenReadingUnapprovedMaterialProductAndUserNotLoggedIn() {
        // Given
        Integer materialProductId = 1;
        MaterialProduct materialProduct = new MaterialProduct();
        materialProduct.setId(materialProductId);
        materialProduct.setApproveStatus(ApproveStatus.REQUESTED);

        when(materialProductJPAService.findOne(materialProductId)).thenReturn(materialProduct);

        SecurityContextHolder.clearContext();

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            materialProductService.readMaterialProductById(materialProductId);
        });
    }

    @Test
    public void shouldCreateMaterialProductWithIndexing() {
        // Given
        var dto = new MaterialProductDTO();
        dto.setDocumentDate("2020-03-02");
        dto.setInternalNumber("MP-001");
        dto.setNumberProduced(100L);
        dto.setMaterialProductType(MaterialProductType.INDUSTRIAL_PRODUCT);
        dto.setAuthorReprint(false);
        dto.setPublisherId(1);

        var materialProduct = new MaterialProduct();
        materialProduct.setId(1);

        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(new rs.teslaris.core.model.commontypes.MultiLingualContent()));
        when(materialProductJPAService.save(any())).thenReturn(materialProduct);
        when(publisherService.findOne(1)).thenReturn(new Publisher());

        setupSecurityContext();

        // When
        var result = materialProductService.createMaterialProduct(dto, true);

        // Then
        verify(multilingualContentService, times(6))
            .getMultilingualContent(any());
        verify(personContributionService).setPersonDocumentContributionsForDocument(
            any(), any());
        verify(materialProductJPAService).save(any(MaterialProduct.class));
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
        assertNotNull(result);
    }

    @Test
    public void shouldCreateMaterialProductWithAuthorReprint() {
        // Given
        var dto = new MaterialProductDTO();
        dto.setDocumentDate("2020-03-02");
        dto.setAuthorReprint(true);

        var materialProduct = new MaterialProduct();
        materialProduct.setId(1);

        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(new rs.teslaris.core.model.commontypes.MultiLingualContent()));
        when(materialProductJPAService.save(any())).thenReturn(materialProduct);

        setupSecurityContext();

        // When
        var result = materialProductService.createMaterialProduct(dto, true);

        // Then
        verify(materialProductJPAService).save(any(MaterialProduct.class));
        verify(publisherService, never()).findOne(any());
    }

    @Test
    public void shouldCreateMaterialProductWithoutIndexing() {
        // Given
        var dto = new MaterialProductDTO();
        dto.setDocumentDate("2020-03-02");

        var materialProduct = new MaterialProduct();
        materialProduct.setId(1);

        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(new rs.teslaris.core.model.commontypes.MultiLingualContent()));
        when(materialProductJPAService.save(any())).thenReturn(materialProduct);

        setupSecurityContext();

        // When
        var result = materialProductService.createMaterialProduct(dto, false);

        // Then
        verify(materialProductJPAService).save(any(MaterialProduct.class));
        verify(documentPublicationIndexRepository, never()).save(
            any(DocumentPublicationIndex.class));
    }

    @Test
    public void shouldEditMaterialProduct() {
        // Given
        Integer materialProductId = 1;
        var dto = new MaterialProductDTO();
        dto.setDocumentDate("2024-01-01");
        dto.setInternalNumber("MP-002");
        dto.setNumberProduced(200L);
        dto.setMaterialProductType(MaterialProductType.PROTOTYPE);
        dto.setAuthorReprint(true);

        var existingProduct = new MaterialProduct();
        existingProduct.setId(materialProductId);
        existingProduct.setApproveStatus(ApproveStatus.REQUESTED);
        existingProduct.setDocumentDate("2023");

        var index = new DocumentPublicationIndex();
        index.setDatabaseId(materialProductId);

        when(materialProductJPAService.findOne(materialProductId)).thenReturn(existingProduct);
        when(materialProductJPAService.save(any())).thenReturn(existingProduct);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            materialProductId))
            .thenReturn(Optional.of(index));

        setupSecurityContext();

        // When
        materialProductService.editMaterialProduct(materialProductId, dto);

        // Then
        verify(materialProductJPAService).findOne(eq(materialProductId));
        verify(personContributionService).setPersonDocumentContributionsForDocument(
            eq(existingProduct), eq(dto));
        verify(materialProductJPAService).save(any(MaterialProduct.class));
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }

    @Test
    public void shouldEditMaterialProductWithNewIndex() {
        // Given
        Integer materialProductId = 1;
        var dto = new MaterialProductDTO();
        dto.setDocumentDate("2024-01-01");

        var existingProduct = new MaterialProduct();
        existingProduct.setId(materialProductId);
        existingProduct.setApproveStatus(ApproveStatus.REQUESTED);

        when(materialProductJPAService.findOne(materialProductId)).thenReturn(existingProduct);
        when(materialProductJPAService.save(any())).thenReturn(existingProduct);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            materialProductId))
            .thenReturn(Optional.empty());

        setupSecurityContext();

        // When
        materialProductService.editMaterialProduct(materialProductId, dto);

        // Then
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }

    @Test
    public void shouldDeleteMaterialProduct() {
        // Given
        Integer materialProductId = 1;
        var materialProductToDelete = new MaterialProduct();
        materialProductToDelete.setId(materialProductId);

        var index = new DocumentPublicationIndex();
        index.setDatabaseId(materialProductId);

        when(materialProductJPAService.findOne(materialProductId))
            .thenReturn(materialProductToDelete);
        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
            materialProductId))
            .thenReturn(Optional.of(index));

        // When
        materialProductService.deleteMaterialProduct(materialProductId);

        // Then
        verify(materialProductJPAService).findOne(eq(materialProductId));
        verify(materialProductJPAService).delete(eq(materialProductId));
        verify(documentPublicationIndexRepository).delete(eq(index));
    }

    @Test
    public void shouldReindexMaterialProducts() {
        // Given
        var materialProduct = new MaterialProduct();
        materialProduct.setId(1);
        materialProduct.setDocumentDate("2024");
        var materialProducts = List.of(materialProduct);
        var page =
            new PageImpl<>(materialProducts, PageRequest.of(0, 100), materialProducts.size());

        when(materialProductJPAService.findAll(any(PageRequest.class))).thenReturn(page);

        // When
        materialProductService.reindexMaterialProducts();

        // Then
        verify(materialProductJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }

    @ParameterizedTest
    @MethodSource("provideMaterialProductTypes")
    public void shouldSetMaterialProductTypeWhenCreating(MaterialProductType type) {
        // Given
        var dto = new MaterialProductDTO();
        dto.setDocumentDate("2020-03-02");
        dto.setMaterialProductType(type);

        var materialProduct = new MaterialProduct();
        materialProduct.setId(1);
        materialProduct.setMaterialProductType(type);

        when(multilingualContentService.getMultilingualContent(any()))
            .thenReturn(Set.of(new rs.teslaris.core.model.commontypes.MultiLingualContent()));
        when(materialProductJPAService.save(any())).thenReturn(materialProduct);

        setupSecurityContext();

        // When
        var result = materialProductService.createMaterialProduct(dto, false);

        // Then
        verify(materialProductJPAService).save(any(MaterialProduct.class));
        assertEquals(type, result.getMaterialProductType());
    }

    @Test
    public void shouldIndexMaterialProduct() {
        // Given
        var materialProduct = new MaterialProduct();
        materialProduct.setId(1);
        materialProduct.setAuthorReprint(true);

        var index = new DocumentPublicationIndex();
        index.setDatabaseId(1);

        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(1))
            .thenReturn(Optional.of(index));
        when(citationService.craftCitationInGivenStyle(any(), any(), any()))
            .thenReturn("Test Citation");

        // When
        materialProductService.indexMaterialProduct(materialProduct);

        // Then
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
        verify(citationService).craftCitationInGivenStyle(eq("apa"), any(), eq("EN"));
    }

    @Test
    public void shouldIndexMaterialProductWithPublisher() {
        // Given
        var materialProduct = new MaterialProduct();
        materialProduct.setId(1);
        materialProduct.setAuthorReprint(false);
        var publisher = new Publisher();
        publisher.setId(5);
        materialProduct.setPublisher(publisher);

        var index = new DocumentPublicationIndex();

        when(documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(1))
            .thenReturn(Optional.of(index));
        when(citationService.craftCitationInGivenStyle(any(), any(), any()))
            .thenReturn("Test Citation");

        // When
        materialProductService.indexMaterialProduct(materialProduct);

        // Then
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }

    private void setupSecurityContext() {
        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
