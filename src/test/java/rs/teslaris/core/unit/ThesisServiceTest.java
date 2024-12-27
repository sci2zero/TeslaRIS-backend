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
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.impl.document.ThesisServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.ThesisJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;

@SpringBootTest
public class ThesisServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private DocumentFileService documentFileService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private EventService eventService;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private ThesisJPAServiceImpl thesisJPAService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private PublisherService publisherService;

    @Mock
    private ResearchAreaService researchAreaService;

    @Mock
    private LanguageTagService languageService;

    @InjectMocks
    private ThesisServiceImpl thesisService;


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
        ReflectionTestUtils.setField(thesisService, "documentApprovedByDefault", true);
    }

    @Test
    public void shouldCreateThesis() {
        // Given
        var dto = new ThesisDTO();
        dto.setOrganisationUnitId(1);
        var thesis = new Thesis();
        thesis.setId(1);
        var document = new Thesis();
        document.setDocumentDate("2023");

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(thesisJPAService.save(any())).thenReturn(document);
        when(organisationUnitService.findOrganisationUnitById(1)).thenReturn(
            new OrganisationUnit());

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        var result = thesisService.createThesis(dto, true);

        // Then
        verify(multilingualContentService, times(4)).getMultilingualContent(any());
        verify(personContributionService).setPersonDocumentContributionsForDocument(eq(document),
            eq(dto));
        verify(thesisJPAService).save(eq(document));
    }

    @Test
    public void shouldEditThesis() {
        // Given
        var thesisId = 1;
        var thesisDTO = new ThesisDTO();
        thesisDTO.setDocumentDate("2024");
        thesisDTO.setOrganisationUnitId(1);

        var thesisToUpdate = new Thesis();
        thesisToUpdate.setApproveStatus(ApproveStatus.REQUESTED);
        thesisToUpdate.setDocumentDate("2023");

        when(organisationUnitService.findOne(1)).thenReturn(new OrganisationUnit());
        when(thesisJPAService.findOne(thesisId)).thenReturn(thesisToUpdate);
        when(thesisJPAService.save(any())).thenReturn(thesisToUpdate);

        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new User());
        var securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // When
        thesisService.editThesis(thesisId, thesisDTO);

        // Then
        verify(thesisJPAService).findOne(eq(thesisId));
        verify(personContributionService).setPersonDocumentContributionsForDocument(
            eq(thesisToUpdate), eq(thesisDTO));
    }

    @ParameterizedTest
    @MethodSource("argumentSources")
    public void shouldReadThesis(DocumentContributionType type, Boolean isMainAuthor,
                                 Boolean isCorrespondingAuthor, Country country) {
        // Given
        var thesisId = 1;
        var thesis = new Thesis();
        thesis.setApproveStatus(ApproveStatus.APPROVED);

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
        thesis.setContributors(Set.of(contribution));

        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);

        // When
        var result = thesisService.readThesisById(thesisId);

        // Then
        verify(thesisJPAService).findOne(eq(thesisId));
        assertNotNull(result);
        assertEquals(1, result.getContributions().size());
    }

    @Test
    public void shouldReindexThesiss() {
        // Given
        var thesis = new Thesis();
        thesis.setDocumentDate("2024");
        var theses = List.of(thesis);
        var page1 = new PageImpl<>(theses.subList(0, 1), PageRequest.of(0, 10),
            theses.size());

        when(thesisJPAService.findAll(any(PageRequest.class))).thenReturn(page1);

        // When
        thesisService.reindexTheses();

        // Then
        verify(documentPublicationIndexRepository, never()).deleteAll();
        verify(thesisJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(documentPublicationIndexRepository, atLeastOnce()).save(
            any(DocumentPublicationIndex.class));
    }
}
