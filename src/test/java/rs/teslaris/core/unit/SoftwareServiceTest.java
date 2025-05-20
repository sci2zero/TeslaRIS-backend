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
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.SoftwareServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.SoftwareJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;

@SpringBootTest
public class SoftwareServiceTest {

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
    private SoftwareJPAServiceImpl softwareJPAService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private CommissionRepository commissionRepository;

    @InjectMocks
    private SoftwareServiceImpl softwareService;


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
        ReflectionTestUtils.setField(softwareService, "documentApprovedByDefault", true);
    }

    @Test
    public void shouldCreateSoftware() {
        // Given
        var dto = new SoftwareDTO();
        var software = new Software();
        software.setId(1);
        software.setInternalNumber("123");
        var document = new Software();
        document.setDocumentDate("2023");

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(softwareJPAService.save(any())).thenReturn(document);

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        var result = softwareService.createSoftware(dto, true);

        // Then
        verify(multilingualContentService, times(4)).getMultilingualContent(any());
        verify(personContributionService).setPersonDocumentContributionsForDocument(eq(document),
            eq(dto));
        verify(softwareJPAService).save(eq(document));
    }

    @Test
    public void shouldEditSoftware() {
        // Given
        var softwareId = 1;
        var softwareDTO = new SoftwareDTO();
        softwareDTO.setDocumentDate("2024");
        var softwareToUpdate = new Software();
        softwareToUpdate.setApproveStatus(ApproveStatus.REQUESTED);
        softwareToUpdate.setDocumentDate("2023");

        when(softwareJPAService.findOne(softwareId)).thenReturn(softwareToUpdate);
        when(softwareJPAService.save(any())).thenReturn(softwareToUpdate);

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        softwareService.editSoftware(softwareId, softwareDTO);

        // Then
        verify(softwareJPAService).findOne(eq(softwareId));
        verify(personContributionService).setPersonDocumentContributionsForDocument(
            eq(softwareToUpdate), eq(softwareDTO));
    }

    @ParameterizedTest
    @MethodSource("argumentSources")
    public void shouldReadSoftware(DocumentContributionType type, Boolean isMainAuthor,
                                   Boolean isCorrespondingAuthor, Country country) {
        // Given
        var softwareId = 1;
        var software = new Software();
        software.setApproveStatus(ApproveStatus.APPROVED);

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
        software.setContributors(Set.of(contribution));

        when(softwareJPAService.findOne(softwareId)).thenReturn(software);

        // When
        var result = softwareService.readSoftwareById(softwareId);

        // Then
        verify(softwareJPAService).findOne(eq(softwareId));
        assertNotNull(result);
        assertEquals(1, result.getContributions().size());
    }

    @Test
    public void shouldReindexSoftwares() {
        // Given
        var software = new Software();
        software.setDocumentDate("2024");
        var softwares = List.of(software);
        var page1 = new PageImpl<>(softwares.subList(0, 1), PageRequest.of(0, 10),
            softwares.size());

        when(softwareJPAService.findAll(any(PageRequest.class))).thenReturn(page1);

        // When
        softwareService.reindexSoftware();

        // Then
        verify(documentPublicationIndexRepository, never()).deleteAll();
        verify(softwareJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }
}
