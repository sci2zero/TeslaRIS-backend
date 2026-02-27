package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import rs.teslaris.core.dto.project.FundingProgramDTO;
import rs.teslaris.core.indexmodel.project.FundingProgramIndex;
import rs.teslaris.core.indexrepository.project.FundingProgramIndexRepository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.project.FundingProgram;
import rs.teslaris.core.model.project.FundingType;
import rs.teslaris.core.repository.project.FundingProgramRepository;
import rs.teslaris.core.service.impl.project.FundingProgramServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.DateRangeException;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;

@SpringBootTest
public class FundingProgramServiceTest {

    @Mock
    private FundingProgramRepository fundingProgramRepository;

    @Mock
    private DocumentFileService documentFileService;

    @Mock
    private SearchService<FundingProgramIndex> searchService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private ResearchAreaService researchAreaService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private FundingProgramIndexRepository fundingProgramIndexRepository;

    @Captor
    private ArgumentCaptor<FundingProgram> fundingProgramCaptor;

    @InjectMocks
    private FundingProgramServiceImpl fundingProgramService;


    @Test
    public void shouldReturnEmptyPageWhenNoFundingProgramsFound() {
        // given
        var tokens = List.of("test");
        var dateFrom = LocalDate.now().minusMonths(6);
        var dateTo = LocalDate.now();
        var funderId = 1;
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(Query.class), eq(pageable),
            eq(FundingProgramIndex.class), eq("funding_program")))
            .thenReturn(Page.empty());

        // when
        var result = fundingProgramService.searchFundingPrograms(
            tokens, dateFrom, dateTo, funderId, pageable);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(searchService).runQuery(any(Query.class), eq(pageable),
            eq(FundingProgramIndex.class), eq("funding_program"));
    }

    @Test
    public void shouldReturnFundingProgramsPageWhenResultsExist() {
        // given
        var tokens = List.of("test");
        var dateFrom = LocalDate.now().minusMonths(6);
        var dateTo = LocalDate.now();
        var funderId = 1;
        var pageable = PageRequest.of(0, 10);

        var fundingProgramIndex = new FundingProgramIndex();
        fundingProgramIndex.setDatabaseId(1);
        fundingProgramIndex.setNameSr("Test Program");
        fundingProgramIndex.setFunderId(1);

        var expectedPage = new PageImpl<>(
            List.of(fundingProgramIndex), pageable, 1);

        when(searchService.runQuery(any(Query.class), eq(pageable),
            eq(FundingProgramIndex.class), eq("funding_program")))
            .thenReturn(expectedPage);

        // when
        var result = fundingProgramService.searchFundingPrograms(
            tokens, dateFrom, dateTo, funderId, pageable);

        // then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().get(0).getDatabaseId());
        assertEquals("Test Program", result.getContent().get(0).getNameSr());
        assertEquals(1, result.getContent().get(0).getFunderId());
        verify(searchService).runQuery(any(Query.class), eq(pageable),
            eq(FundingProgramIndex.class), eq("funding_program"));
    }

    @Test
    public void shouldReturnFundingProgramDTOWhenFundingProgramExists() {
        // given
        var fundingProgramId = 1;
        var fundingProgram = new FundingProgram();
        fundingProgram.setId(fundingProgramId);
        fundingProgram.setFunder(new OrganisationUnit());

        when(fundingProgramRepository.findById(fundingProgramId))
            .thenReturn(Optional.of(fundingProgram));

        // when
        var result = fundingProgramService.readFundingProgram(fundingProgramId);

        // then
        assertNotNull(result);
        assertEquals(fundingProgramId, result.getId());
        verify(fundingProgramRepository).findById(any());
    }

    @Test
    public void shouldThrowExceptionWhenFundingProgramNotFound() {
        // given
        var fundingProgramId = 999;

        when(fundingProgramRepository.findById(fundingProgramId))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
            fundingProgramService.readFundingProgram(fundingProgramId));
        verify(fundingProgramRepository).findById(fundingProgramId);
    }

    @Test
    public void shouldCreateFundingProgramSuccessfully() {
        // given
        var fundingProgramDTO = new FundingProgramDTO();
        fundingProgramDTO.setName(List.of());
        fundingProgramDTO.setDescription(List.of());
        fundingProgramDTO.setObjectives(List.of());
        fundingProgramDTO.setNameAbbreviation(List.of());
        fundingProgramDTO.setKeywords(List.of());
        fundingProgramDTO.setResearchAreasId(Set.of(1, 2));
        fundingProgramDTO.setFunderId(10);
        fundingProgramDTO.setFundingTypes(Set.of(FundingType.GRANT));

        var monetaryAmountDTO = new MonetaryAmountDTO();
        monetaryAmountDTO.setCurrencyId(1);
        monetaryAmountDTO.setAmount(10000.0);
        fundingProgramDTO.setMonetaryAmount(monetaryAmountDTO);

        fundingProgramDTO.setProgramOpens(LocalDate.now());
        fundingProgramDTO.setProgramCloses(LocalDate.now().plusYears(1));
        fundingProgramDTO.setUris(Set.of("https://example.com"));
        fundingProgramDTO.setOaMandated(true);
        fundingProgramDTO.setOaMandateUrl("https://example.com/mandate");

        var savedFundingProgram = new FundingProgram();
        savedFundingProgram.setId(1);
        savedFundingProgram.setFunder(new OrganisationUnit());

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(researchAreaService.getResearchAreasByIds(anyList()))
            .thenReturn(List.of());
        when(organisationUnitService.findOne(10))
            .thenReturn(new OrganisationUnit());
        when(currencyService.findOne(1))
            .thenReturn(null);
        when(fundingProgramRepository.save(any(FundingProgram.class)))
            .thenReturn(savedFundingProgram);
        when(fundingProgramIndexRepository.save(any(FundingProgramIndex.class)))
            .thenReturn(new FundingProgramIndex());

        // when
        var result = fundingProgramService.createFundingProgram(fundingProgramDTO);

        // then
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(multilingualContentService, times(5)).getMultilingualContent(anyList());
        verify(researchAreaService).getResearchAreasByIds(anyList());
        verify(organisationUnitService).findOne(10);
        verify(currencyService).findOne(1);
        verify(fundingProgramRepository).save(any(FundingProgram.class));
        verify(fundingProgramIndexRepository).save(any(FundingProgramIndex.class));
    }

    @Test
    public void shouldThrowWhenCreateAndDatesAreInvalid() {
        // given
        var fundingProgramDTO = new FundingProgramDTO();
        fundingProgramDTO.setName(List.of());
        fundingProgramDTO.setDescription(List.of());
        fundingProgramDTO.setObjectives(List.of());
        fundingProgramDTO.setNameAbbreviation(List.of());
        fundingProgramDTO.setKeywords(List.of());
        fundingProgramDTO.setResearchAreasId(Set.of(1, 2));
        fundingProgramDTO.setFunderId(10);
        fundingProgramDTO.setFundingTypes(Set.of(FundingType.GRANT));

        fundingProgramDTO.setProgramCloses(LocalDate.now());
        fundingProgramDTO.setProgramOpens(LocalDate.now().plusYears(1));
        fundingProgramDTO.setUris(Set.of("https://example.com"));
        fundingProgramDTO.setOaMandated(true);
        fundingProgramDTO.setOaMandateUrl("https://example.com/mandate");

        // when & then
        assertThrows(DateRangeException.class,
            () -> fundingProgramService.createFundingProgram(fundingProgramDTO));
    }

    @Test
    public void shouldCreateFundingProgramWithoutMonetaryAmount() {
        // given
        var fundingProgramDTO = new FundingProgramDTO();
        fundingProgramDTO.setName(List.of());
        fundingProgramDTO.setDescription(List.of());
        fundingProgramDTO.setObjectives(List.of());
        fundingProgramDTO.setNameAbbreviation(List.of());
        fundingProgramDTO.setKeywords(List.of());
        fundingProgramDTO.setResearchAreasId(Set.of());
        fundingProgramDTO.setFunderId(10);
        fundingProgramDTO.setFundingTypes(Set.of(FundingType.GRANT));
        fundingProgramDTO.setMonetaryAmount(null);

        var savedFundingProgram = new FundingProgram();
        savedFundingProgram.setId(1);
        savedFundingProgram.setFunder(new OrganisationUnit());

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(organisationUnitService.findOne(10))
            .thenReturn(new OrganisationUnit());
        when(fundingProgramRepository.save(any(FundingProgram.class)))
            .thenReturn(savedFundingProgram);
        when(fundingProgramIndexRepository.save(any(FundingProgramIndex.class)))
            .thenReturn(new FundingProgramIndex());

        // when
        var result = fundingProgramService.createFundingProgram(fundingProgramDTO);

        // then
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(currencyService, never()).findOne(anyInt());
        verify(fundingProgramRepository).save(any(FundingProgram.class));
        verify(fundingProgramIndexRepository).save(any(FundingProgramIndex.class));
    }

    @Test
    public void shouldUpdateFundingProgramSuccessfully() {
        // given
        var fundingProgramId = 1;
        var existingFundingProgram = new FundingProgram();
        existingFundingProgram.setId(fundingProgramId);
        existingFundingProgram.setName(new HashSet<>());
        existingFundingProgram.setDescription(new HashSet<>());
        existingFundingProgram.setObjectives(new HashSet<>());
        existingFundingProgram.setNameAbbreviation(new HashSet<>());
        existingFundingProgram.setKeywords(new HashSet<>());
        existingFundingProgram.setResearchAreas(new HashSet<>());

        var fundingProgramDTO = new FundingProgramDTO();
        fundingProgramDTO.setName(List.of());
        fundingProgramDTO.setDescription(List.of());
        fundingProgramDTO.setObjectives(List.of());
        fundingProgramDTO.setNameAbbreviation(List.of());
        fundingProgramDTO.setKeywords(List.of());
        fundingProgramDTO.setResearchAreasId(Set.of(1, 2));
        fundingProgramDTO.setFunderId(10);
        fundingProgramDTO.setFundingTypes(Set.of(FundingType.GRANT));

        var fundingProgramIndex = new FundingProgramIndex();
        fundingProgramIndex.setDatabaseId(fundingProgramId);

        when(fundingProgramRepository.findById(fundingProgramId))
            .thenReturn(Optional.of(existingFundingProgram));
        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(researchAreaService.getResearchAreasByIds(anyList()))
            .thenReturn(List.of());
        when(organisationUnitService.findOne(10))
            .thenReturn(new OrganisationUnit());
        when(fundingProgramIndexRepository.findFundingProgramIndexByDatabaseId(fundingProgramId))
            .thenReturn(Optional.of(fundingProgramIndex));

        // when
        fundingProgramService.updateFundingProgram(fundingProgramId, fundingProgramDTO);

        // then
        verify(fundingProgramRepository).findById(fundingProgramId);
        verify(multilingualContentService, times(5)).getMultilingualContent(anyList());
        verify(researchAreaService).getResearchAreasByIds(anyList());
        verify(organisationUnitService).findOne(10);
        verify(fundingProgramIndexRepository).findFundingProgramIndexByDatabaseId(fundingProgramId);
        verify(fundingProgramIndexRepository).save(any(FundingProgramIndex.class));
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingNonExistentFundingProgram() {
        // given
        var fundingProgramId = 999;
        var fundingProgramDTO = new FundingProgramDTO();

        when(fundingProgramRepository.findById(fundingProgramId))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
            fundingProgramService.updateFundingProgram(fundingProgramId, fundingProgramDTO));
        verify(fundingProgramRepository).findById(fundingProgramId);
        verify(fundingProgramIndexRepository, never()).findFundingProgramIndexByDatabaseId(
            anyInt());
    }

    @Test
    public void shouldDeleteFundingProgramWhenNoCallsExist() {
        // given
        var fundingProgramId = 1;

        when(fundingProgramRepository.hasFundingCalls(fundingProgramId))
            .thenReturn(false);
        when(fundingProgramRepository.findById(fundingProgramId)).thenReturn(
            Optional.of(new FundingProgram()));
        doNothing().when(fundingProgramRepository).deleteById(fundingProgramId);

        // when
        fundingProgramService.deleteFundingProgram(fundingProgramId);

        // then
        verify(fundingProgramRepository).hasFundingCalls(fundingProgramId);
        verify(fundingProgramRepository).save(any());
    }

    @Test
    public void shouldThrowExceptionWhenDeletingFundingProgramWithCalls() {
        // given
        var fundingProgramId = 1;

        when(fundingProgramRepository.hasFundingCalls(fundingProgramId))
            .thenReturn(true);

        // when & then
        assertThrows(ReferenceConstraintException.class, () ->
            fundingProgramService.deleteFundingProgram(fundingProgramId));
        verify(fundingProgramRepository).hasFundingCalls(fundingProgramId);
        verify(fundingProgramRepository, never()).deleteById(anyInt());
    }

    @Test
    public void shouldAddDocumentToFundingProgram() {
        // given
        var fundingProgramId = 1;
        var documentFileDTO = new DocumentFileDTO();
        documentFileDTO.setAccessRights(AccessRights.RESTRICTED_ACCESS);

        var fundingProgram = new FundingProgram();
        fundingProgram.setId(fundingProgramId);
        fundingProgram.setProgramDocuments(new HashSet<>());

        var savedDocumentFile = new DocumentFile();
        savedDocumentFile.setId(100);

        var expectedResponse = new DocumentFileResponseDTO();
        expectedResponse.setId(100);

        when(fundingProgramRepository.findById(fundingProgramId))
            .thenReturn(Optional.of(fundingProgram));
        when(documentFileService.saveNewDocument(any(DocumentFileDTO.class), eq(false)))
            .thenReturn(savedDocumentFile);
        when(fundingProgramRepository.save(any(FundingProgram.class)))
            .thenReturn(fundingProgram);

        try (var documentFileConverterMock = mockStatic(DocumentFileConverter.class)) {
            documentFileConverterMock.when(() ->
                    DocumentFileConverter.toDTO(savedDocumentFile))
                .thenReturn(expectedResponse);

            // when
            var result = fundingProgramService.addFundingProgramDocument(
                fundingProgramId, documentFileDTO);

            // then
            assertNotNull(result);
            assertEquals(100, result.getId());
            verify(fundingProgramRepository).findById(fundingProgramId);
            verify(documentFileService).saveNewDocument(any(DocumentFileDTO.class), eq(false));
            verify(fundingProgramRepository).save(any(FundingProgram.class));
            documentFileConverterMock.verify(() ->
                DocumentFileConverter.toDTO(savedDocumentFile), times(1));
        }
    }

    @Test
    public void shouldThrowExceptionWhenAddingDocumentToNonExistentFundingProgram() {
        // given
        var fundingProgramId = 999;
        var documentFileDTO = new DocumentFileDTO();

        when(fundingProgramRepository.findById(fundingProgramId))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
            fundingProgramService.addFundingProgramDocument(fundingProgramId, documentFileDTO));
        verify(fundingProgramRepository).findById(fundingProgramId);
        verify(documentFileService, never()).saveNewDocument(any(), anyBoolean());
    }

    @Test
    public void shouldUpdateFundingProgramDocument() {
        // given
        var documentFileDTO = new DocumentFileDTO();
        documentFileDTO.setAccessRights(AccessRights.RESTRICTED_ACCESS);
        documentFileDTO.setId(100);

        var expectedResponse = new DocumentFileResponseDTO();
        expectedResponse.setId(100);

        when(documentFileService.editDocumentFile(any(DocumentFileDTO.class), eq(false)))
            .thenReturn(expectedResponse);

        // when
        var result = fundingProgramService.updateFundingProgramDocument(documentFileDTO);

        // then
        assertNotNull(result);
        assertEquals(100, result.getId());
        verify(documentFileService).editDocumentFile(any(DocumentFileDTO.class), eq(false));
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingNonExistentDocument() {
        // given
        var documentFileDTO = new DocumentFileDTO();
        documentFileDTO.setId(999);

        when(documentFileService.editDocumentFile(any(DocumentFileDTO.class), eq(false)))
            .thenThrow(new RuntimeException("Document not found"));

        // when & then
        assertThrows(RuntimeException.class, () ->
            fundingProgramService.updateFundingProgramDocument(documentFileDTO));
        verify(documentFileService).editDocumentFile(any(DocumentFileDTO.class), eq(false));
    }

    @Test
    public void shouldDeleteFundingProgramDocument() {
        // given
        var programFileId = 100;
        var fundingProgramId = 1;

        var documentFile = new DocumentFile();
        documentFile.setId(programFileId);

        var fundingProgram = new FundingProgram();
        fundingProgram.setId(fundingProgramId);
        fundingProgram.setProgramDocuments(new HashSet<>(Set.of(documentFile)));

        when(documentFileService.findOne(programFileId))
            .thenReturn(documentFile);
        when(fundingProgramRepository.findById(fundingProgramId))
            .thenReturn(Optional.of(fundingProgram));
        doNothing().when(documentFileService).delete(programFileId);
        when(fundingProgramRepository.save(any(FundingProgram.class)))
            .thenReturn(fundingProgram);

        // when
        fundingProgramService.deleteFundingProgramDocument(programFileId, fundingProgramId);

        // then
        verify(documentFileService).findOne(programFileId);
        verify(fundingProgramRepository).findById(fundingProgramId);
        verify(documentFileService).delete(programFileId);
        verify(fundingProgramRepository).save(any(FundingProgram.class));
    }

    @Test
    public void shouldThrowExceptionWhenDeletingDocumentFromNonExistentFundingProgram() {
        // given
        var programFileId = 100;
        var fundingProgramId = 999;

        var documentFile = new DocumentFile();
        documentFile.setId(programFileId);

        when(documentFileService.findOne(programFileId))
            .thenReturn(documentFile);
        when(fundingProgramRepository.findById(fundingProgramId))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
            fundingProgramService.deleteFundingProgramDocument(programFileId, fundingProgramId));
        verify(documentFileService).findOne(programFileId);
        verify(fundingProgramRepository).findById(fundingProgramId);
        verify(documentFileService, never()).delete(anyInt());
    }

    @Test
    public void shouldIndexSingleFundingProgram() {
        // given
        var fundingProgram = new FundingProgram();
        fundingProgram.setId(1);

        var funder = new OrganisationUnit();
        funder.setId(10);
        fundingProgram.setFunder(funder);

        var fundingProgramIndex = new FundingProgramIndex();

        when(fundingProgramIndexRepository.save(any(FundingProgramIndex.class)))
            .thenReturn(fundingProgramIndex);

        // when
        fundingProgramService.indexFundingProgram(fundingProgram, fundingProgramIndex);

        // then
        verify(fundingProgramIndexRepository).save(any(FundingProgramIndex.class));
    }
}
