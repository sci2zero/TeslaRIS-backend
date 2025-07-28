package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
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
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisAttachmentType;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.repository.document.ThesisResearchOutputRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.ThesisServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.ThesisJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitTrustConfigurationService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.ThesisException;
import rs.teslaris.core.util.xmlutil.XMLUtil;

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
    private XMLUtil xmlUtil;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private ThesisJPAServiceImpl thesisJPAService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private PublisherService publisherService;

    @Mock
    private LanguageTagService languageService;

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private ThesisResearchOutputRepository thesisResearchOutputRepository;

    @Mock
    private ThesisRepository thesisRepository;

    @Mock
    private OrganisationUnitIndexRepository organisationUnitIndexRepository;

    @Mock
    private OrganisationUnitTrustConfigurationService organisationUnitTrustConfigurationService;

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
        var ou = new OrganisationUnit();
        ou.setId(1);
        var dto = new ThesisDTO();
        dto.setOrganisationUnitId(ou.getId());
        var document = new Thesis();
        document.setDocumentDate("2023");
        document.setOrganisationUnit(ou);
        document.setThesisType(ThesisType.PHD);

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
        verify(multilingualContentService, times(7)).getMultilingualContent(any());
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
        thesisDTO.setThesisType(ThesisType.PHD);

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
        thesis.setThesisType(ThesisType.UNDERGRADUATE_THESIS);
        thesis.setDocumentDate("2024");
        thesis.setOrganisationUnit(new OrganisationUnit());

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

    @Test
    public void shouldDeleteThesisAttachmentAndUpdateLatestFile() {
        // given
        var thesisId = 1;
        var documentFileId = 100;
        var attachmentType = ThesisAttachmentType.FILE;

        var thesis = new Thesis();
        var documentFile = new DocumentFile();

        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);
        when(documentFileService.findDocumentFileById(documentFileId)).thenReturn(documentFile);

        // when
        thesisService.deleteThesisAttachment(thesisId, documentFileId, attachmentType);

        // then
        verify(thesisJPAService).findOne(thesisId);
        verify(documentFileService).findDocumentFileById(documentFileId);
        verify(thesisJPAService).save(thesis);
        verify(documentFileService).deleteDocumentFile(documentFile.getServerFilename());
    }

    @Test
    public void shouldPutThesisOnPublicReview() {
        // given
        var thesisId = 1;
        var thesis = mock(Thesis.class);

        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);
        when(thesis.getIsOnPublicReview()).thenReturn(false);
        when(thesis.getOrganisationUnit()).thenReturn(new OrganisationUnit());
        when(thesis.getThesisType()).thenReturn(ThesisType.PHD);
        when(thesis.getPreliminaryFiles()).thenReturn(Set.of(new DocumentFile()));
        when(thesis.getPreliminarySupplements()).thenReturn(Set.of(new DocumentFile()));
        when(thesis.getCommissionReports()).thenReturn(Set.of(new DocumentFile()));
        when(thesis.getTitle()).thenReturn(new HashSet<>(List.of(mock(MultiLingualContent.class))));

        // when
        thesisService.putOnPublicReview(thesisId, false);

        // then
        verify(thesisJPAService).findOne(thesisId);
        verify(thesisJPAService).save(thesis);
    }

    @Test
    public void shouldThrowExceptionIfAlreadyOnPublicReview() {
        // given
        var thesisId = 1;
        var thesis = mock(Thesis.class);

        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);
        when(thesis.getThesisType()).thenReturn(ThesisType.PHD);
        when(thesis.getIsOnPublicReview()).thenReturn(true);

        // when / then
        assertThrows(ThesisException.class, () -> thesisService.putOnPublicReview(thesisId, false));
        verify(thesisJPAService).findOne(thesisId);
        verify(thesis, never()).setIsOnPublicReview(true);
        verify(thesisJPAService, never()).save(any());
    }

    @Test
    public void shouldThrowExceptionIfThesisIsNotPHDThesis() {
        // given
        var thesisId = 1;
        var thesis = mock(Thesis.class);

        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);
        when(thesis.getThesisType()).thenReturn(ThesisType.BACHELOR);

        // when / then
        assertThrows(ThesisException.class, () -> thesisService.putOnPublicReview(thesisId, false));
        verify(thesisJPAService).findOne(thesisId);
        verify(thesis, never()).setIsOnPublicReview(true);
        verify(thesisJPAService, never()).save(any());
    }

    @Test
    void shouldRemoveThesisFromPublicReview() {
        // Given
        var thesisId = 1;
        var thesis = new Thesis();
        thesis.setThesisType(ThesisType.BACHELOR);
        thesis.setOrganisationUnit(new OrganisationUnit());
        thesis.setIsOnPublicReview(true);
        thesis.getPublicReviewStartDates().add(LocalDate.of(2024, 1, 1));
        thesis.getPublicReviewStartDates().add(LocalDate.of(2024, 1, 10));

        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);

        // When
        thesisService.removeFromPublicReview(thesisId);

        // Then
        assertFalse(thesis.getIsOnPublicReview());
        assertEquals(2, thesis.getPublicReviewStartDates().size());
        assertTrue(thesis.getPublicReviewStartDates().contains(LocalDate.of(2024, 1, 10)));
        verify(thesisJPAService).findOne(thesisId);
    }

    @Test
    public void shouldContinueLastPublicReviewIfThesisWasPaused() {
        // given
        var thesisId = 1;
        var thesis = mock(Thesis.class);
        var lastReviewDate = LocalDate.of(2024, 1, 1);
        var reviewDates = new HashSet<>(List.of(lastReviewDate));

        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);
        when(thesis.getThesisType()).thenReturn(ThesisType.PHD);
        when(thesis.getIsOnPublicReview()).thenReturn(false);
        when(thesis.getOrganisationUnit()).thenReturn(new OrganisationUnit());
        when(thesis.getIsOnPublicReviewPause()).thenReturn(true);
        when(thesis.getPublicReviewStartDates()).thenReturn(reviewDates);
        when(thesis.getPreliminaryFiles()).thenReturn(Set.of(mock(DocumentFile.class)));
        when(thesis.getCommissionReports()).thenReturn(Set.of(mock(DocumentFile.class)));
        when(thesis.getTitle()).thenReturn(new HashSet<>(List.of(mock(MultiLingualContent.class))));

        // when
        thesisService.putOnPublicReview(thesisId, true);

        // then
        verify(thesisJPAService).findOne(thesisId);
        verify(thesis).setIsOnPublicReview(true);
        verify(thesisJPAService).save(thesis);
        assertEquals(2, thesis.getPublicReviewStartDates().size());
        assertTrue(thesis.getPublicReviewStartDates().contains(lastReviewDate));
    }

    @Test
    public void shouldNotRemoveLastPublicReviewDateIfContinuingLastReview() {
        // given
        var thesisId = 1;
        var thesis = mock(Thesis.class);
        var lastReviewDate = LocalDate.of(2024, 1, 1);
        var reviewDates = new HashSet<>(List.of(lastReviewDate));

        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);
        when(thesis.getThesisType()).thenReturn(ThesisType.PHD);
        when(thesis.getIsOnPublicReview()).thenReturn(false);
        when(thesis.getOrganisationUnit()).thenReturn(new OrganisationUnit());
        when(thesis.getIsOnPublicReviewPause()).thenReturn(true);
        when(thesis.getPublicReviewStartDates()).thenReturn(reviewDates);
        when(thesis.getPreliminaryFiles()).thenReturn(Set.of(mock(DocumentFile.class)));
        when(thesis.getCommissionReports()).thenReturn(Set.of(mock(DocumentFile.class)));
        when(thesis.getTitle()).thenReturn(new HashSet<>(List.of(mock(MultiLingualContent.class))));

        // when
        thesisService.putOnPublicReview(thesisId, true);

        // then
        verify(thesisJPAService).findOne(thesisId);
        verify(thesis).setIsOnPublicReview(true);
        verify(thesisJPAService).save(thesis);
        assertEquals(2, thesis.getPublicReviewStartDates().size());
        assertTrue(thesis.getPublicReviewStartDates().contains(lastReviewDate));
    }

    @Test
    void shouldThrowExceptionWhenThesisNotOnPublicReview() {
        // Given
        var thesisId = 1;
        var thesis = new Thesis();
        thesis.setIsOnPublicReview(false);

        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);

        // When / Then
        ThesisException exception = assertThrows(ThesisException.class,
            () -> thesisService.removeFromPublicReview(thesisId));
        assertEquals("Thesis is not on public review.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenThesisHasNeverBeenOnPublicReview() {
        // Given
        var thesisId = 1;
        var thesis = new Thesis();
        thesis.setIsOnPublicReview(true);

        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);

        // When / Then
        ThesisException exception = assertThrows(ThesisException.class,
            () -> thesisService.removeFromPublicReview(thesisId));
        assertEquals("Never been on public review.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenThesisNeverBeenOnPublicReview() {
        // Given
        var thesisId = 1;
        var thesis = new Thesis();
        thesis.setIsOnPublicReview(true);
        thesis.setPublicReviewStartDates(new HashSet<>()); // Empty set

        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);

        // When / Then
        ThesisException exception = assertThrows(ThesisException.class,
            () -> thesisService.removeFromPublicReview(thesisId));
        assertEquals("Never been on public review.", exception.getMessage());
    }

    @ParameterizedTest
    @EnumSource(ThesisAttachmentType.class)
    public void shouldAddThesisAttachmentAndSetLatest(ThesisAttachmentType attachmentType) {
        // given
        var thesisId = 1;
        var thesis = mock(Thesis.class);
        var document = new DocumentFileDTO();
        var documentFile = mock(DocumentFile.class);

        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);
        when(documentFileService.saveNewPreliminaryDocument(document)).thenReturn(documentFile);

        Set<DocumentFile> fileSet = mock(Set.class);
        when(fileSet.add(documentFile)).thenReturn(true);
        switch (attachmentType) {
            case FILE -> when(thesis.getPreliminaryFiles()).thenReturn(fileSet);
            case SUPPLEMENT -> when(thesis.getPreliminarySupplements()).thenReturn(fileSet);
            case COMMISSION_REPORT -> when(thesis.getCommissionReports()).thenReturn(fileSet);
        }

        // when
        var response = thesisService.addThesisAttachment(thesisId, document, attachmentType);

        // then
        verify(thesisJPAService).findOne(thesisId);
        verify(documentFileService).saveNewPreliminaryDocument(document);

        verify(thesisJPAService).save(thesis);
        assertNotNull(response);
    }

    @Test
    void shouldThrowExceptionWhenAttachmentsAreMissing() {
        // Given
        var thesis = new Thesis();
        thesis.setThesisType(ThesisType.PHD);
        thesis.setIsOnPublicReview(false);
        var thesisId = 1;
        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);

        // When & Then
        ThesisException exception =
            assertThrows(ThesisException.class,
                () -> thesisService.putOnPublicReview(thesisId, false));
        assertEquals("noAttachmentsMessage", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
        "1, 3",
        "2, 1"
    })
    void shouldThrowExceptionWhenAttachmentsAreUnequal(int files, int reports) {
        // Given
        var thesis = new Thesis();
        thesis.setThesisType(ThesisType.PHD);
        thesis.setIsOnPublicReview(false);
        var thesisId = 1;
        thesis.getPreliminaryFiles().addAll(createMockDocuments(files));
        thesis.getCommissionReports().addAll(createMockDocuments(reports));
        thesis.setTitle(new HashSet<>(List.of(mock(MultiLingualContent.class))));
        thesis.setOrganisationUnit(new OrganisationUnit());

        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);

        // When & Then
        ThesisException exception =
            assertThrows(ThesisException.class,
                () -> thesisService.putOnPublicReview(thesisId, false));
        assertEquals("missingAttachmentsMessage", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenMissingDataForArchiving() {
        // Given
        var thesisId = 1;
        var thesis = new Thesis();
        thesis.setTitle(new HashSet<>());
        thesis.setThesisDefenceDate(null);
        thesis.setDocumentDate(null);

        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);

        // When / Then
        ThesisException exception = assertThrows(ThesisException.class,
            () -> thesisService.archiveThesis(thesisId));
        assertEquals("missingDataToArchiveMessage", exception.getMessage());
    }

    @Test
    void shouldArchiveThesisSuccessfully() {
        // Given
        var thesisId = 1;
        var thesis = new Thesis();
        thesis.setTitle(new HashSet<>(Set.of(new MultiLingualContent())));
        thesis.setThesisDefenceDate(LocalDate.now());
        thesis.setDocumentDate(String.valueOf(thesis.getThesisDefenceDate().getYear()));

        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);

        // When
        thesisService.archiveThesis(thesisId);

        // Then
        assertTrue(thesis.getIsArchived());
        verify(thesisJPAService).save(thesis);
    }

    @Test
    void shouldUnarchiveThesisSuccessfully() {
        // Given
        var thesisId = 1;
        var thesis = new Thesis();
        thesis.setIsArchived(true);

        when(thesisJPAService.findOne(thesisId)).thenReturn(thesis);

        // When
        thesisService.unarchiveThesis(thesisId);

        // Then
        assertFalse(thesis.getIsArchived());
        verify(thesisJPAService).save(thesis);
    }

    @Test
    void shouldThrowNotFoundWhenThesisDoesNotExist() {
        // Given
        var oldId = 123;
        when(thesisRepository.findThesisByOldIdsContains(oldId)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () -> thesisService.readThesisByOldId(oldId));
        verify(thesisRepository).findThesisByOldIdsContains(oldId);
    }

    @Test
    void shouldThrowNotFoundWhenThesisIsNotApproved() {
        // Given
        var oldId = 456;
        var thesis = new Thesis();
        thesis.setApproveStatus(ApproveStatus.REQUESTED);
        when(thesisRepository.findThesisByOldIdsContains(oldId)).thenReturn(Optional.of(thesis));

        // When / Then
        assertThrows(NotFoundException.class, () -> thesisService.readThesisByOldId(oldId));
        verify(thesisRepository).findThesisByOldIdsContains(oldId);
    }

    @Test
    void shouldReturnDtoWhenThesisIsApproved() {
        // Given
        var oldId = 789;
        var thesis = new Thesis();
        thesis.setApproveStatus(ApproveStatus.APPROVED);

        when(thesisRepository.findThesisByOldIdsContains(oldId)).thenReturn(Optional.of(thesis));

        // When
        var result = thesisService.readThesisByOldId(oldId);

        // Then
        assertNotNull(result);
        verify(thesisRepository).findThesisByOldIdsContains(oldId);
    }

    @Test
    void shouldTransferPreprintToOfficialPublication() {
        // Given
        var preprintFile = new DocumentFile();
        preprintFile.setId(42);
        preprintFile.setResourceType(ResourceType.PREPRINT);

        var thesis = new Thesis();
        thesis.setId(1);
        thesis.setPreliminaryFiles(new HashSet<>(List.of(preprintFile)));
        thesis.setFileItems(new HashSet<>());

        when(thesisJPAService.findOne(1)).thenReturn(thesis);

        // When
        thesisService.transferPreprintToOfficialPublication(1, 42);

        // Then
        assertTrue(thesis.getPreliminaryFiles().isEmpty());
        assertEquals(1, thesis.getFileItems().size());
        assertEquals(ResourceType.OFFICIAL_PUBLICATION,
            thesis.getFileItems().iterator().next().getResourceType());

        verify(thesisJPAService).save(thesis);
    }

    @Test
    void shouldThrowExceptionWhenOfficialAlreadyExists() {
        // Given
        var officialFile = new DocumentFile();
        officialFile.setResourceType(ResourceType.OFFICIAL_PUBLICATION);

        var thesis = new Thesis();
        thesis.setId(1);
        thesis.setFileItems(new HashSet<>(List.of(officialFile)));

        when(thesisJPAService.findOne(1)).thenReturn(thesis);

        // When / Then
        assertThrows(ThesisException.class,
            () -> thesisService.transferPreprintToOfficialPublication(1, 123));
    }

    private Set<DocumentFile> createMockDocuments(int count) {
        return IntStream.range(0, count).mapToObj(i -> {
                var docFile = new DocumentFile();
                docFile.setId(i);
                return docFile;
            })
            .collect(Collectors.toSet());
    }
}
