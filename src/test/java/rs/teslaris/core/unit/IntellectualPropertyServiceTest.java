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
import rs.teslaris.core.dto.commontypes.FlexibleDateDTO;
import rs.teslaris.core.dto.document.IntellectualPropertyDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.FlexibleDate;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.IntellectualProperty;
import rs.teslaris.core.model.document.IntellectualPropertyType;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.IntellectualPropertyRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.IntellectualPropertyServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.IntellectualPropertyJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class IntellectualPropertyServiceTest {

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
    private IntellectualPropertyJPAServiceImpl intellectualPropertyJPAService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private OrganisationUnitTrustConfigurationService organisationUnitTrustConfigurationService;

    @Mock
    private CitationService citationService;

    @Mock
    private IntellectualPropertyRepository intellectualPropertyRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private IntellectualPropertyServiceImpl intellectualPropertyService;


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
        ReflectionTestUtils.setField(intellectualPropertyService, "documentApprovedByDefault",
            true);
    }

    @Test
    public void shouldCreateIntellectualProperty() {
        // Given
        var dto = new IntellectualPropertyDTO();
        dto.setDocumentDate(new FlexibleDateDTO(2023, 7, 9, null));
        dto.setType(IntellectualPropertyType.PATENT);
        var intellectualProperty = new IntellectualProperty();
        intellectualProperty.setId(1);
        intellectualProperty.setNumber("123");
        var document = new IntellectualProperty();
        document.setDocumentDate(new FlexibleDate(2023));

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(intellectualPropertyJPAService.save(any())).thenReturn(document);

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        var result = intellectualPropertyService.createIntellectualProperty(dto, true);

        // Then
        assertNotNull(result);
        verify(multilingualContentService, times(9)).getMultilingualContent(any());
        verify(personContributionService).setPersonDocumentContributionsForDocument(eq(document),
            eq(dto));
        verify(intellectualPropertyJPAService).save(eq(document));
    }

    @Test
    public void shouldEditIntellectualProperty() {
        // Given
        var intellectualPropertyId = 1;
        var intellectualPropertyDTO = new IntellectualPropertyDTO();
        intellectualPropertyDTO.setType(IntellectualPropertyType.PATENT);
        intellectualPropertyDTO.setDocumentDate(new FlexibleDateDTO(2024, null, null, null));
        var intellectualPropertyToUpdate = new IntellectualProperty();
        intellectualPropertyToUpdate.setApproveStatus(ApproveStatus.REQUESTED);
        intellectualPropertyToUpdate.setDocumentDate(new FlexibleDate(2023));

        when(intellectualPropertyJPAService.findOne(intellectualPropertyId)).thenReturn(
            intellectualPropertyToUpdate);
        when(intellectualPropertyJPAService.save(any())).thenReturn(new IntellectualProperty());

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        intellectualPropertyService.editIntellectualProperty(intellectualPropertyId,
            intellectualPropertyDTO);

        // Then
        verify(intellectualPropertyJPAService).findOne(eq(intellectualPropertyId));
        verify(personContributionService).setPersonDocumentContributionsForDocument(
            eq(intellectualPropertyToUpdate), eq(intellectualPropertyDTO));
    }

    @ParameterizedTest
    @MethodSource("argumentSources")
    public void shouldReadIntellectualProperty(DocumentContributionType type, Boolean isMainAuthor,
                                               Boolean isCorrespondingAuthor, Country country) {
        // Given
        var intellectualPropertyId = 1;
        var intellectualProperty = new IntellectualProperty();
        intellectualProperty.setApproveStatus(ApproveStatus.APPROVED);

        var contribution = new PersonDocumentContribution();
        contribution.setContributionType(type);
        contribution.setIsMainContributor(isMainAuthor);
        contribution.setIsCorrespondingContributor(isCorrespondingAuthor);
        contribution.setApproveStatus(ApproveStatus.APPROVED);
        var affiliationStatement = new AffiliationStatement();
        affiliationStatement.setContact(new Contact());
        affiliationStatement.setDisplayPersonName(new PersonName());
        affiliationStatement.setPostalAddress(
            new PostalAddress(country, new HashSet<>(), new HashSet<>(), new HashSet<>(), null));
        contribution.setAffiliationStatement(affiliationStatement);
        intellectualProperty.setContributors(Set.of(contribution));

        when(intellectualPropertyJPAService.findOne(intellectualPropertyId)).thenReturn(
            intellectualProperty);

        // When
        var result =
            intellectualPropertyService.readIntellectualPropertyById(intellectualPropertyId);

        // Then
        verify(intellectualPropertyJPAService).findOne(eq(intellectualPropertyId));
        assertNotNull(result);
        assertEquals(1, result.getContributions().size());
    }

    @Test
    public void shouldReindexIntellectualProperties() {
        // Given
        var intellectualProperty = new IntellectualProperty();
        intellectualProperty.setDocumentDate(new FlexibleDate(2024));
        var intellectualProperties = List.of(intellectualProperty);
        var page1 = new PageImpl<>(intellectualProperties.subList(0, 1), PageRequest.of(0, 10),
            intellectualProperties.size());

        when(intellectualPropertyJPAService.findAll(any(PageRequest.class))).thenReturn(page1);

        // When
        intellectualPropertyService.reindexIntellectualProperties();

        // Then
        verify(documentPublicationIndexRepository, never()).deleteAll();
        verify(intellectualPropertyJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }

    @Test
    void shouldThrowNotFoundWhenIntellectualPropertyDoesNotExist() {
        // Given
        var oldId = 123;
        when(intellectualPropertyRepository.findIntellectualPropertyByOldIdsContains(
            oldId)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class,
            () -> intellectualPropertyService.readIntellectualPropertyByOldId(oldId));
        verify(intellectualPropertyRepository).findIntellectualPropertyByOldIdsContains(oldId);
    }

    @Test
    void shouldThrowNotFoundWhenIntellectualPropertyIsNotApproved() {
        // Given
        var oldId = 456;
        var intellectualProperty = new IntellectualProperty();
        intellectualProperty.setApproveStatus(ApproveStatus.REQUESTED);
        when(intellectualPropertyRepository.findIntellectualPropertyByOldIdsContains(
            oldId)).thenReturn(Optional.of(intellectualProperty));

        // When / Then
        assertThrows(NotFoundException.class,
            () -> intellectualPropertyService.readIntellectualPropertyByOldId(oldId));
        verify(intellectualPropertyRepository).findIntellectualPropertyByOldIdsContains(oldId);
    }

    @Test
    void shouldReturnDtoWhenIntellectualPropertyIsApproved() {
        // Given
        var oldId = 789;
        var intellectualProperty = new IntellectualProperty();
        intellectualProperty.setApproveStatus(ApproveStatus.APPROVED);

        when(intellectualPropertyRepository.findIntellectualPropertyByOldIdsContains(
            oldId)).thenReturn(Optional.of(intellectualProperty));

        // When
        var result = intellectualPropertyService.readIntellectualPropertyByOldId(oldId);

        // Then
        assertNotNull(result);
        verify(intellectualPropertyRepository).findIntellectualPropertyByOldIdsContains(oldId);
    }
}
