package rs.teslaris.core.unit.project;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.DateRangeException;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;
import rs.teslaris.project.dto.funding.FundingDTO;
import rs.teslaris.project.indexmodel.funding.FundingIndex;
import rs.teslaris.project.indexrepository.funding.FundingIndexRepository;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.model.funding.FundingCall;
import rs.teslaris.project.model.funding.FundingType;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.repository.funding.FundingRepository;
import rs.teslaris.project.service.impl.funding.FundingServiceImpl;
import rs.teslaris.project.service.interfaces.funding.FundingCallService;
import rs.teslaris.project.service.interfaces.project.ProjectService;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
public class FundingServiceTest extends BaseTest {

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private SearchService<FundingIndex> searchService;

    @Mock
    private ResearchAreaService researchAreaService;

    @Mock
    private ProjectService projectService;

    @Mock
    private FundingCallService fundingCallService;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private FundingIndexRepository fundingIndexRepository;

    @Mock
    private DocumentFileService documentFileService;

    @InjectMocks
    private FundingServiceImpl fundingService;


    @Test
    public void shouldReturnEmptyPageWhenNoFundingFound() {
        // given
        var tokens = List.of("test");
        var dateFrom = LocalDate.now().minusMonths(6);
        var dateTo = LocalDate.now();
        var projectId = 1;
        var fundingCallId = 1;
        var funderId = 1;
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(Query.class), eq(pageable),
                eq(FundingIndex.class), eq("funding")))
                .thenReturn(Page.empty());

        // when
        var result = fundingService.searchFunding(
                tokens, dateFrom, dateTo, projectId, fundingCallId, funderId, pageable);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(searchService).runQuery(any(Query.class), eq(pageable),
                eq(FundingIndex.class), eq("funding"));
    }

    @Test
    public void shouldReturnFundingCallsPageWhenResultsExist() {
        // given
        var tokens = List.of("test");
        var dateFrom = LocalDate.of(2022, 7, 5);
        var dateTo = LocalDate.of(2023, 3, 18);
        var projectId = 1;
        var fundingCallId = 1;
        var funderId = 1;
        var pageable = PageRequest.of(0, 10);

        var fundingIndex = new FundingIndex();
        fundingIndex.setDatabaseId(1);
        fundingIndex.setNameSr("Test Funding");
        fundingIndex.setProjectId(1);
        fundingIndex.setFundingCallId(1);
        fundingIndex.setFunderId(1);

        var expectedPage = new PageImpl<>(
                List.of(fundingIndex), pageable, 1);

        when(searchService.runQuery(any(Query.class), eq(pageable),
                eq(FundingIndex.class), eq("funding")))
                .thenReturn(expectedPage);

        // when
        var result = fundingService.searchFunding(
                tokens, dateFrom, dateTo, projectId, fundingCallId, funderId, pageable);

        // then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().getFirst().getDatabaseId());
        assertEquals("Test Funding", result.getContent().getFirst().getNameSr());
        assertEquals(1, result.getContent().getFirst().getProjectId());
        assertEquals(1, result.getContent().getFirst().getFundingCallId());
        assertEquals(1, result.getContent().getFirst().getFunderId());
        verify(searchService).runQuery(any(Query.class), eq(pageable),
                eq(FundingIndex.class), eq("funding"));
    }

    @Test
    public void shouldReturnFundingDTOWhenFundingExists() {
        // given
        var fundingId = 1;
        var funding = new Funding();
        funding.setId(fundingId);
        funding.setProject(new Project());

        when(fundingRepository.findById(fundingId)).thenReturn(Optional.of(funding));

        // when
        var result = fundingService.readFunding(fundingId);

        // then
        assertNotNull(result);
        assertEquals(fundingId, result.getId());
        verify(fundingRepository).findById(any());
    }

    @Test
    public void shouldThrowExceptionWhenFundingNotFound() {
        // given
        var fundingId = 999;

        when(fundingRepository.findById(fundingId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () -> fundingService.readFunding(fundingId));
        verify(fundingRepository).findById(fundingId);
    }

    @Test
    public void shouldCreateFundingSuccessfully() {
        // given
        var fundingDTO = new FundingDTO();
        fundingDTO.setName(List.of());
        fundingDTO.setDescription(List.of());
        fundingDTO.setNameAbbreviation(List.of());
        fundingDTO.setKeywords(List.of());
        fundingDTO.setDisplayCall(List.of());
        fundingDTO.setDisplayProgram(List.of());
        fundingDTO.setDisplayFunder(List.of());
        fundingDTO.setResearchAreasId(Set.of());
        fundingDTO.setProjectId(1);
        fundingDTO.setFundingCallId(1);
        fundingDTO.setFundingTypes(Set.of(FundingType.GRANT));
        fundingDTO.setDateFrom(LocalDate.now());
        fundingDTO.setDateTo(LocalDate.now().plusYears(1));
        fundingDTO.setUris(Set.of("https://example.com"));
        fundingDTO.setOaMandated(true);
        fundingDTO.setOaMandateUrl("https://example.com/mandate");
        fundingDTO.setCompetitive(true);
        fundingDTO.setRenewable(false);

        var monetaryAmountDTO = new MonetaryAmountDTO();
        monetaryAmountDTO.setCurrencyId(1);
        monetaryAmountDTO.setAmount(250000.0);
        fundingDTO.setAmount(monetaryAmountDTO);

        var savedFunding = new Funding();
        savedFunding.setId(1);
        savedFunding.setProject(new Project());

        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(Set.of(new MultiLingualContent()));
        when(researchAreaService.getResearchAreasByIds(anyList())).thenReturn(List.of());
        when(projectService.findOne(1)).thenReturn(new Project());
        when(fundingCallService.findOne(1)).thenReturn(new FundingCall());
        when(currencyService.findOne(1)).thenReturn(null);
        when(fundingRepository.save(any(Funding.class))).thenReturn(savedFunding);

        // when
        var result = fundingService.createFunding(fundingDTO);

        // then
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(multilingualContentService, times(7)).getMultilingualContent(anyList());
        verify(researchAreaService).getResearchAreasByIds(anyList());
        verify(projectService).findOne(1);
        verify(fundingCallService).findOne(1);
        verify(currencyService).findOne(1);
        verify(fundingRepository).save(any(Funding.class));
    }

    @Test
    public void shouldCreateFundingWithoutFundingCall() {
        // given
        var fundingDTO = new FundingDTO();
        fundingDTO.setName(List.of());
        fundingDTO.setDescription(List.of());
        fundingDTO.setNameAbbreviation(List.of());
        fundingDTO.setKeywords(List.of());
        fundingDTO.setDisplayCall(List.of());
        fundingDTO.setDisplayProgram(List.of());
        fundingDTO.setDisplayFunder(List.of());
        fundingDTO.setResearchAreasId(Set.of());
        fundingDTO.setProjectId(1);
        fundingDTO.setFunderId(5);
        fundingDTO.setFundingTypes(Set.of(FundingType.GRANT));
        fundingDTO.setDateFrom(LocalDate.now());
        fundingDTO.setDateTo(LocalDate.now().plusYears(1));

        var monetaryAmountDTO = new MonetaryAmountDTO();
        monetaryAmountDTO.setCurrencyId(1);
        monetaryAmountDTO.setAmount(250000.0);
        fundingDTO.setAmount(monetaryAmountDTO);

        var savedFunding = new Funding();
        savedFunding.setId(2);
        savedFunding.setProject(new Project());

        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(Set.of(new MultiLingualContent()));
        when(researchAreaService.getResearchAreasByIds(anyList())).thenReturn(List.of());
        when(projectService.findOne(1)).thenReturn(new Project());
        when(organisationUnitService.findOne(5)).thenReturn(new OrganisationUnit());
        when(currencyService.findOne(1)).thenReturn(null);
        when(fundingRepository.save(any(Funding.class))).thenReturn(savedFunding);

        // when
        var result = fundingService.createFunding(fundingDTO);

        // then
        assertNotNull(result);
        assertEquals(2, result.getId());
        verify(organisationUnitService).findOne(5);
        verify(fundingCallService, never()).findOne(any());
    }

    @Test
    public void shouldThrowWhenProjectIdIsNull() {
        // given
        var fundingDTO = new FundingDTO();
        fundingDTO.setName(List.of());
        fundingDTO.setDescription(List.of());
        fundingDTO.setNameAbbreviation(List.of());
        fundingDTO.setKeywords(List.of());
        fundingDTO.setDisplayCall(List.of());
        fundingDTO.setDisplayProgram(List.of());
        fundingDTO.setDisplayFunder(List.of());
        fundingDTO.setResearchAreasId(Set.of());
        fundingDTO.setFundingTypes(Set.of(FundingType.GRANT));
        fundingDTO.setDateFrom(LocalDate.now());
        fundingDTO.setDateTo(LocalDate.now().plusYears(1));

        // when & then
        assertThrows(ReferenceConstraintException.class, () -> fundingService.createFunding(fundingDTO));

        verify(projectService, never()).findOne(any());
        verify(fundingRepository, never()).save(any());
    }

    @Test
    public void shouldThrowWhenDatesAreInvalid() {
        // given
        var fundingDTO = new FundingDTO();
        fundingDTO.setName(List.of());
        fundingDTO.setDescription(List.of());
        fundingDTO.setNameAbbreviation(List.of());
        fundingDTO.setKeywords(List.of());
        fundingDTO.setDisplayCall(List.of());
        fundingDTO.setDisplayProgram(List.of());
        fundingDTO.setDisplayFunder(List.of());
        fundingDTO.setResearchAreasId(Set.of());
        fundingDTO.setProjectId(1);
        fundingDTO.setFundingTypes(Set.of(FundingType.GRANT));
        fundingDTO.setDateTo(LocalDate.now());
        fundingDTO.setDateFrom(LocalDate.now().plusYears(1));

        // when & then
        assertThrows(DateRangeException.class, () -> fundingService.createFunding(fundingDTO));

        verify(fundingRepository, never()).save(any());
    }

    @Test
    public void shouldCreateFundingWithoutMonetaryAmount() {
        // given
        var fundingDTO = new FundingDTO();
        fundingDTO.setName(List.of());
        fundingDTO.setDescription(List.of());
        fundingDTO.setNameAbbreviation(List.of());
        fundingDTO.setKeywords(List.of());
        fundingDTO.setDisplayCall(List.of());
        fundingDTO.setDisplayProgram(List.of());
        fundingDTO.setDisplayFunder(List.of());
        fundingDTO.setResearchAreasId(Set.of());
        fundingDTO.setProjectId(1);
        fundingDTO.setFundingTypes(Set.of(FundingType.GRANT));
        fundingDTO.setAmount(null);
        fundingDTO.setDateFrom(LocalDate.now());
        fundingDTO.setDateTo(LocalDate.now().plusYears(1));

        var savedFunding = new Funding();
        savedFunding.setId(1);
        savedFunding.setProject(new Project());

        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(Set.of(new MultiLingualContent()));
        when(projectService.findOne(1)).thenReturn(new Project());
        when(fundingRepository.save(any(Funding.class))).thenReturn(savedFunding);

        // when
        var result = fundingService.createFunding(fundingDTO);

        // then
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(currencyService, never()).findOne(anyInt());
        verify(fundingRepository).save(any(Funding.class));
    }

    @Test
    public void shouldUpdateFundingSuccessfully() {
        // given
        var fundingId = 1;
        var existingFunding = new Funding();
        existingFunding.setId(fundingId);
        existingFunding.setName(new HashSet<>());
        existingFunding.setDescription(new HashSet<>());
        existingFunding.setNameAbbreviation(new HashSet<>());
        existingFunding.setKeywords(new HashSet<>());
        existingFunding.setDisplayCall(new HashSet<>());
        existingFunding.setDisplayProgram(new HashSet<>());
        existingFunding.setDisplayFunder(new HashSet<>());
        existingFunding.setResearchAreas(new HashSet<>());

        var fundingDTO = new FundingDTO();
        fundingDTO.setName(List.of());
        fundingDTO.setDescription(List.of());
        fundingDTO.setNameAbbreviation(List.of());
        fundingDTO.setKeywords(List.of());
        fundingDTO.setDisplayCall(List.of());
        fundingDTO.setDisplayProgram(List.of());
        fundingDTO.setDisplayFunder(List.of());
        fundingDTO.setResearchAreasId(Set.of(1, 2));
        fundingDTO.setProjectId(1);
        fundingDTO.setFundingTypes(Set.of(FundingType.GRANT));
        fundingDTO.setDateFrom(LocalDate.now());
        fundingDTO.setDateTo(LocalDate.now().plusYears(1));

        var fundingIndex = new FundingIndex();
        fundingIndex.setDatabaseId(fundingId);

        when(fundingRepository.findById(fundingId))
                .thenReturn(Optional.of(existingFunding));
        when(multilingualContentService.getMultilingualContent(anyList()))
                .thenReturn(Set.of(new MultiLingualContent()));
        when(researchAreaService.getResearchAreasByIds(anyList()))
                .thenReturn(List.of());
        when(projectService.findOne(1))
                .thenReturn(new Project());
        when(fundingIndexRepository.findFundingIndexByDatabaseId(fundingId))
                .thenReturn(Optional.of(fundingIndex));

        // when
        fundingService.updateFunding(fundingId, fundingDTO);

        // then
        verify(fundingRepository).findById(fundingId);
        verify(multilingualContentService, times(7)).getMultilingualContent(anyList());
        verify(researchAreaService).getResearchAreasByIds(anyList());
        verify(projectService).findOne(1);
        verify(fundingIndexRepository).findFundingIndexByDatabaseId(fundingId);
        verify(fundingIndexRepository).save(any(FundingIndex.class));
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingNonExistentFunding() {
        // given
        var fundingId = 999;
        var fundingDTO = new FundingDTO();

        when(fundingRepository.findById(fundingId))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
                fundingService.updateFunding(fundingId, fundingDTO));
        verify(fundingRepository).findById(fundingId);
        verify(fundingIndexRepository, never()).findFundingIndexByDatabaseId(anyInt());
    }

    @Test
    public void shouldAddDocumentToFunding() {
        // given
        var fundingId = 1;
        var documentFileDTO = new DocumentFileDTO();
        documentFileDTO.setAccessRights(AccessRights.RESTRICTED_ACCESS);

        var funding = new Funding();
        funding.setId(fundingId);
        funding.setAgreements(new HashSet<>());

        var savedDocumentFile = new DocumentFile();
        savedDocumentFile.setId(100);

        var expectedResponse = new DocumentFileResponseDTO();
        expectedResponse.setId(100);

        when(fundingRepository.findById(fundingId))
                .thenReturn(Optional.of(funding));
        when(documentFileService.saveNewDocument(any(DocumentFileDTO.class), eq(false)))
                .thenReturn(savedDocumentFile);
        when(fundingRepository.save(any(Funding.class)))
                .thenReturn(funding);

        try (var documentFileConverterMock = mockStatic(DocumentFileConverter.class)) {
            documentFileConverterMock.when(() ->
                            DocumentFileConverter.toDTO(savedDocumentFile))
                    .thenReturn(expectedResponse);

            // when
            var result = fundingService.addAgreementDocument(
                    fundingId, documentFileDTO);

            // then
            assertNotNull(result);
            assertEquals(100, result.getId());
            verify(fundingRepository).findById(fundingId);
            verify(documentFileService).saveNewDocument(any(DocumentFileDTO.class), eq(false));
            verify(fundingRepository).save(any(Funding.class));
            documentFileConverterMock.verify(() ->
                    DocumentFileConverter.toDTO(savedDocumentFile), times(1));
        }
    }

    @Test
    public void shouldThrowExceptionWhenAddingDocumentToNonExistentFunding() {
        // given
        var fundingId = 999;
        var documentFileDTO = new DocumentFileDTO();

        when(fundingRepository.findById(fundingId))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
                fundingService.addAgreementDocument(fundingId, documentFileDTO));
        verify(fundingRepository).findById(fundingId);
        verify(documentFileService, never()).saveNewDocument(any(), anyBoolean());
    }

    @Test
    public void shouldDeleteFundingDocument() {
        // given
        var agreementFileId = 100;
        var fundingId = 1;

        var documentFile = new DocumentFile();
        documentFile.setId(agreementFileId);

        var funding = new Funding();
        funding.setId(fundingId);
        funding.setAgreements(new HashSet<>(Set.of(documentFile)));

        when(documentFileService.findOne(agreementFileId))
                .thenReturn(documentFile);
        when(fundingRepository.findById(fundingId))
                .thenReturn(Optional.of(funding));
        doNothing().when(documentFileService).delete(agreementFileId);
        when(fundingRepository.save(any(Funding.class)))
                .thenReturn(funding);

        // when
        fundingService.deleteAgreementDocument(agreementFileId, fundingId);

        // then
        verify(documentFileService).findOne(agreementFileId);
        verify(fundingRepository).findById(fundingId);
        verify(documentFileService).delete(agreementFileId);
        verify(fundingRepository).save(any(Funding.class));
    }

    @Test
    public void shouldThrowExceptionWhenDeletingDocumentFromNonExistentFunding() {
        // given
        var agreementFileId = 100;
        var fundingId = 999;

        var documentFile = new DocumentFile();
        documentFile.setId(agreementFileId);

        when(documentFileService.findOne(agreementFileId))
                .thenReturn(documentFile);
        when(fundingRepository.findById(fundingId))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
                fundingService.deleteAgreementDocument(agreementFileId, fundingId));
        verify(documentFileService).findOne(agreementFileId);
        verify(fundingRepository).findById(fundingId);
        verify(documentFileService, never()).delete(anyInt());
    }
}
