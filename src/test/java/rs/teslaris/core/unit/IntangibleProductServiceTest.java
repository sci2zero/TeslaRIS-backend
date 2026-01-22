package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import rs.teslaris.core.dto.document.IntangibleProductDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.IntangibleProduct;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.IntangibleProductServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.IntangibleProductJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;

@SpringBootTest
public class IntangibleProductServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentFileService documentFileService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private EventService eventService;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private IntangibleProductJPAServiceImpl intangibleProductJPAService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private OrganisationUnitTrustConfigurationService organisationUnitTrustConfigurationService;

    @Mock
    private CitationService citationService;

    @Mock
    private ResearchAreaService researchAreaService;

    @InjectMocks
    private IntangibleProductServiceImpl intangibleProductService;


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
        ReflectionTestUtils.setField(intangibleProductService, "documentApprovedByDefault", true);
    }

    @Test
    public void shouldCreateIntangibleProduct() {
        // Given
        var dto = new IntangibleProductDTO();
        dto.setDocumentDate("2020-03-02");
        var intangibleProduct = new IntangibleProduct();
        intangibleProduct.setId(1);
        intangibleProduct.setInternalNumber("123");
        var document = new IntangibleProduct();
        document.setDocumentDate("2023");

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(intangibleProductJPAService.save(any())).thenReturn(document);

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        var result = intangibleProductService.createIntangibleProduct(dto, true);

        // Then
        verify(multilingualContentService, times(6)).getMultilingualContent(any());
        verify(personContributionService).setPersonDocumentContributionsForDocument(eq(document),
            eq(dto));
        verify(intangibleProductJPAService).save(eq(document));
    }

    @Test
    public void shouldEditIntangibleProduct() {
        // Given
        var intangibleProductId = 1;
        var intangibleProductDTO = new IntangibleProductDTO();
        intangibleProductDTO.setDocumentDate("2024");
        var intangibleProductToUpdate = new IntangibleProduct();
        intangibleProductToUpdate.setApproveStatus(ApproveStatus.REQUESTED);
        intangibleProductToUpdate.setDocumentDate("2023");

        when(intangibleProductJPAService.findOne(intangibleProductId)).thenReturn(
            intangibleProductToUpdate);
        when(intangibleProductJPAService.save(any())).thenReturn(intangibleProductToUpdate);

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        intangibleProductService.editIntangibleProduct(intangibleProductId, intangibleProductDTO);

        // Then
        verify(intangibleProductJPAService).findOne(eq(intangibleProductId));
        verify(personContributionService).setPersonDocumentContributionsForDocument(
            eq(intangibleProductToUpdate), eq(intangibleProductDTO));
    }

    @ParameterizedTest
    @MethodSource("argumentSources")
    public void shouldReadIntangibleProduct(DocumentContributionType type, Boolean isMainAuthor,
                                            Boolean isCorrespondingAuthor, Country country) {
        // Given
        var intangibleProductId = 1;
        var intangibleProduct = new IntangibleProduct();
        intangibleProduct.setApproveStatus(ApproveStatus.APPROVED);

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
        intangibleProduct.setContributors(Set.of(contribution));

        when(intangibleProductJPAService.findOne(intangibleProductId)).thenReturn(
            intangibleProduct);

        // When
        var result = intangibleProductService.readIntangibleProductById(intangibleProductId);

        // Then
        verify(intangibleProductJPAService).findOne(eq(intangibleProductId));
        assertNotNull(result);
        assertEquals(1, result.getContributions().size());
    }

    @Test
    public void shouldReindexIntangibleProducts() {
        // Given
        var intangibleProduct = new IntangibleProduct();
        intangibleProduct.setDocumentDate("2024");
        var intangibleProducts = List.of(intangibleProduct);
        var page1 = new PageImpl<>(intangibleProducts.subList(0, 1), PageRequest.of(0, 10),
            intangibleProducts.size());

        when(intangibleProductJPAService.findAll(any(PageRequest.class))).thenReturn(page1);

        // When
        intangibleProductService.reindexIntangibleProduct();

        // Then
        verify(documentPublicationIndexRepository, never()).deleteAll();
        verify(intangibleProductJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }
}
