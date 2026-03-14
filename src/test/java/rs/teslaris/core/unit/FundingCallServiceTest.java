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
import rs.teslaris.core.dto.project.FundingCallDTO;
import rs.teslaris.core.indexmodel.project.FundingCallIndex;
import rs.teslaris.core.indexrepository.project.FundingCallIndexRepository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.project.FundingCall;
import rs.teslaris.core.model.project.FundingProgram;
import rs.teslaris.core.model.project.FundingType;
import rs.teslaris.core.repository.project.FundingCallRepository;
import rs.teslaris.core.service.impl.project.FundingCallServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.project.FundingProgramService;
import rs.teslaris.core.util.exceptionhandling.exception.DateRangeException;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;

@SpringBootTest
public class FundingCallServiceTest {

    @Mock
    private FundingCallRepository fundingCallRepository;

    @Mock
    private DocumentFileService documentFileService;

    @Mock
    private SearchService<FundingCallIndex> searchService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private ResearchAreaService researchAreaService;

    @Mock
    private FundingProgramService fundingProgramService;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private FundingCallIndexRepository fundingCallIndexRepository;

    @InjectMocks
    private FundingCallServiceImpl fundingCallService;


    @Test
    public void shouldReturnEmptyPageWhenNoFundingCallsFound() {
        // given
        var tokens = List.of("test");
        var dateFrom = LocalDate.now().minusMonths(6);
        var dateTo = LocalDate.now();
        var fundingProgramId = 1;
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(Query.class), eq(pageable),
            eq(FundingCallIndex.class), eq("funding_call")))
            .thenReturn(Page.empty());

        // when
        var result = fundingCallService.searchFundingCalls(
            tokens, dateFrom, dateTo, fundingProgramId, pageable);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(searchService).runQuery(any(Query.class), eq(pageable),
            eq(FundingCallIndex.class), eq("funding_call"));
    }

    @Test
    public void shouldReturnFundingCallsPageWhenResultsExist() {
        // given
        var tokens = List.of("test");
        var dateFrom = LocalDate.now().minusMonths(6);
        var dateTo = LocalDate.now();
        var fundingProgramId = 1;
        var pageable = PageRequest.of(0, 10);

        var fundingCallIndex = new FundingCallIndex();
        fundingCallIndex.setDatabaseId(1);
        fundingCallIndex.setNameSr("Test Call");
        fundingCallIndex.setProgramId(1);

        var expectedPage = new PageImpl<>(
            List.of(fundingCallIndex), pageable, 1);

        when(searchService.runQuery(any(Query.class), eq(pageable),
            eq(FundingCallIndex.class), eq("funding_call")))
            .thenReturn(expectedPage);

        // when
        var result = fundingCallService.searchFundingCalls(
            tokens, dateFrom, dateTo, fundingProgramId, pageable);

        // then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().getFirst().getDatabaseId());
        assertEquals("Test Call", result.getContent().getFirst().getNameSr());
        assertEquals(1, result.getContent().getFirst().getProgramId());
        verify(searchService).runQuery(any(Query.class), eq(pageable),
            eq(FundingCallIndex.class), eq("funding_call"));
    }

    @Test
    public void shouldReturnFundingCallDTOWhenFundingCallExists() {
        // given
        var fundingCallId = 1;
        var fundingCall = new FundingCall();
        fundingCall.setId(fundingCallId);
        fundingCall.setFundingProgram(new FundingProgram());

        when(fundingCallRepository.findById(fundingCallId))
            .thenReturn(Optional.of(fundingCall));

        // when
        var result = fundingCallService.readFundingCall(fundingCallId);

        // then
        assertNotNull(result);
        assertEquals(fundingCallId, result.getId());
        verify(fundingCallRepository).findById(any());
    }

    @Test
    public void shouldThrowExceptionWhenFundingCallNotFound() {
        // given
        var fundingCallId = 999;

        when(fundingCallRepository.findById(fundingCallId))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
            fundingCallService.readFundingCall(fundingCallId));
        verify(fundingCallRepository).findById(fundingCallId);
    }

    @Test
    public void shouldCreateFundingCallSuccessfully() {
        // given
        var fundingCallDTO = new FundingCallDTO();
        fundingCallDTO.setName(List.of());
        fundingCallDTO.setDescription(List.of());
        fundingCallDTO.setObjectives(List.of());
        fundingCallDTO.setNameAbbreviation(List.of());
        fundingCallDTO.setKeywords(List.of());
        fundingCallDTO.setResearchAreasId(Set.of(1, 2));
        fundingCallDTO.setFundingProgramId(10);
        fundingCallDTO.setFundingTypes(Set.of(FundingType.GRANT));

        var monetaryAmountDTO = new MonetaryAmountDTO();
        monetaryAmountDTO.setCurrencyId(1);
        monetaryAmountDTO.setAmount(10000.0);
        fundingCallDTO.setMonetaryAmount(monetaryAmountDTO);

        fundingCallDTO.setDateFrom(LocalDate.now());
        fundingCallDTO.setDateTo(LocalDate.now().plusYears(1));
        fundingCallDTO.setUris(Set.of("https://example.com"));
        fundingCallDTO.setOaMandated(true);
        fundingCallDTO.setOaMandateUrl("https://example.com/mandate");

        var savedFundingCall = new FundingCall();
        savedFundingCall.setId(1);
        savedFundingCall.setFundingProgram(new FundingProgram());

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(researchAreaService.getResearchAreasByIds(anyList()))
            .thenReturn(List.of());
        when(fundingProgramService.findOne(10))
            .thenReturn(new FundingProgram());
        when(currencyService.findOne(1))
            .thenReturn(null);
        when(fundingCallRepository.save(any(FundingCall.class)))
            .thenReturn(savedFundingCall);
        when(fundingCallIndexRepository.save(any(FundingCallIndex.class)))
            .thenReturn(new FundingCallIndex());

        // when
        var result = fundingCallService.createFundingCall(fundingCallDTO);

        // then
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(multilingualContentService, times(5)).getMultilingualContent(anyList());
        verify(researchAreaService).getResearchAreasByIds(anyList());
        verify(fundingProgramService).findOne(10);
        verify(currencyService).findOne(1);
        verify(fundingCallRepository).save(any(FundingCall.class));
        verify(fundingCallIndexRepository).save(any(FundingCallIndex.class));
    }

    @Test
    public void shouldThrowWhenCreateAndDatesAreInvalid() {
        // given
        var fundingCallDTO = new FundingCallDTO();
        fundingCallDTO.setName(List.of());
        fundingCallDTO.setDescription(List.of());
        fundingCallDTO.setObjectives(List.of());
        fundingCallDTO.setNameAbbreviation(List.of());
        fundingCallDTO.setKeywords(List.of());
        fundingCallDTO.setResearchAreasId(Set.of(1, 2));
        fundingCallDTO.setFundingProgramId(10);
        fundingCallDTO.setFundingTypes(Set.of(FundingType.GRANT));

        fundingCallDTO.setDateTo(LocalDate.now());
        fundingCallDTO.setDateFrom(LocalDate.now().plusYears(1));
        fundingCallDTO.setUris(Set.of("https://example.com"));
        fundingCallDTO.setOaMandated(true);
        fundingCallDTO.setOaMandateUrl("https://example.com/mandate");

        // when & then
        assertThrows(DateRangeException.class,
            () -> fundingCallService.createFundingCall(fundingCallDTO));
    }

    @Test
    public void shouldCreateFundingCallWithoutMonetaryAmount() {
        // given
        var fundingCallDTO = new FundingCallDTO();
        fundingCallDTO.setName(List.of());
        fundingCallDTO.setDescription(List.of());
        fundingCallDTO.setObjectives(List.of());
        fundingCallDTO.setNameAbbreviation(List.of());
        fundingCallDTO.setKeywords(List.of());
        fundingCallDTO.setResearchAreasId(Set.of());
        fundingCallDTO.setFundingProgramId(10);
        fundingCallDTO.setFundingTypes(Set.of(FundingType.GRANT));
        fundingCallDTO.setMonetaryAmount(null);

        var savedFundingCall = new FundingCall();
        savedFundingCall.setId(1);
        savedFundingCall.setFundingProgram(new FundingProgram());

        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(fundingProgramService.findOne(10))
            .thenReturn(new FundingProgram());
        when(fundingCallRepository.save(any(FundingCall.class)))
            .thenReturn(savedFundingCall);
        when(fundingCallIndexRepository.save(any(FundingCallIndex.class)))
            .thenReturn(new FundingCallIndex());

        // when
        var result = fundingCallService.createFundingCall(fundingCallDTO);

        // then
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(currencyService, never()).findOne(anyInt());
        verify(fundingCallRepository).save(any(FundingCall.class));
        verify(fundingCallIndexRepository).save(any(FundingCallIndex.class));
    }

    @Test
    public void shouldUpdateFundingCallSuccessfully() {
        // given
        var fundingCallId = 1;
        var existingFundingCall = new FundingCall();
        existingFundingCall.setId(fundingCallId);
        existingFundingCall.setName(new HashSet<>());
        existingFundingCall.setDescription(new HashSet<>());
        existingFundingCall.setObjectives(new HashSet<>());
        existingFundingCall.setNameAbbreviation(new HashSet<>());
        existingFundingCall.setKeywords(new HashSet<>());
        existingFundingCall.setResearchAreas(new HashSet<>());

        var fundingCallDTO = new FundingCallDTO();
        fundingCallDTO.setName(List.of());
        fundingCallDTO.setDescription(List.of());
        fundingCallDTO.setObjectives(List.of());
        fundingCallDTO.setNameAbbreviation(List.of());
        fundingCallDTO.setKeywords(List.of());
        fundingCallDTO.setResearchAreasId(Set.of(1, 2));
        fundingCallDTO.setFundingProgramId(10);
        fundingCallDTO.setFundingTypes(Set.of(FundingType.GRANT));

        var fundingCallIndex = new FundingCallIndex();
        fundingCallIndex.setDatabaseId(fundingCallId);

        when(fundingCallRepository.findById(fundingCallId))
            .thenReturn(Optional.of(existingFundingCall));
        when(multilingualContentService.getMultilingualContent(anyList()))
            .thenReturn(Set.of(new MultiLingualContent()));
        when(researchAreaService.getResearchAreasByIds(anyList()))
            .thenReturn(List.of());
        when(fundingProgramService.findOne(10))
            .thenReturn(new FundingProgram());
        when(fundingCallIndexRepository.findFundingCallIndexByDatabaseId(fundingCallId))
            .thenReturn(Optional.of(fundingCallIndex));

        // when
        fundingCallService.updateFundingCall(fundingCallId, fundingCallDTO);

        // then
        verify(fundingCallRepository).findById(fundingCallId);
        verify(multilingualContentService, times(5)).getMultilingualContent(anyList());
        verify(researchAreaService).getResearchAreasByIds(anyList());
        verify(fundingProgramService).findOne(10);
        verify(fundingCallIndexRepository).findFundingCallIndexByDatabaseId(fundingCallId);
        verify(fundingCallIndexRepository).save(any(FundingCallIndex.class));
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingNonExistentFundingCall() {
        // given
        var fundingCallId = 999;
        var fundingCallDTO = new FundingCallDTO();

        when(fundingCallRepository.findById(fundingCallId))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
            fundingCallService.updateFundingCall(fundingCallId, fundingCallDTO));
        verify(fundingCallRepository).findById(fundingCallId);
        verify(fundingCallIndexRepository, never()).findFundingCallIndexByDatabaseId(
            anyInt());
    }

    @Test
    public void shouldDeleteFundingCallWhenNoProposalsAndFudningExist() {
        // given
        var fundingCallId = 1;

        when(fundingCallRepository.hasFundingProposals(fundingCallId))
            .thenReturn(false);
        when(fundingCallRepository.hasFunding(fundingCallId))
            .thenReturn(false);
        when(fundingCallRepository.findById(fundingCallId)).thenReturn(
            Optional.of(new FundingCall()));
        doNothing().when(fundingCallRepository).deleteById(fundingCallId);

        // when
        fundingCallService.deleteFundingCall(fundingCallId);

        // then
        verify(fundingCallRepository).hasFundingProposals(fundingCallId);
        verify(fundingCallRepository).hasFunding(fundingCallId);
        verify(fundingCallRepository).save(any());
    }

    @Test
    public void shouldThrowExceptionWhenDeletingFundingCallWithProposals() {
        // given
        var fundingCallId = 1;

        when(fundingCallRepository.hasFundingProposals(fundingCallId))
            .thenReturn(true);
        when(fundingCallRepository.hasFunding(fundingCallId))
            .thenReturn(false);

        // when & then
        assertThrows(ReferenceConstraintException.class, () ->
            fundingCallService.deleteFundingCall(fundingCallId));
        verify(fundingCallRepository).hasFundingProposals(fundingCallId);
        verify(fundingCallRepository).hasFunding(fundingCallId);
        verify(fundingCallRepository, never()).deleteById(anyInt());
    }

    @Test
    public void shouldThrowExceptionWhenDeletingFundingCallWithFunding() {
        // given
        var fundingCallId = 1;

        when(fundingCallRepository.hasFundingProposals(fundingCallId))
            .thenReturn(false);
        when(fundingCallRepository.hasFunding(fundingCallId))
            .thenReturn(true);

        // when & then
        assertThrows(ReferenceConstraintException.class, () ->
            fundingCallService.deleteFundingCall(fundingCallId));
        verify(fundingCallRepository).hasFunding(fundingCallId);
        verify(fundingCallRepository, never()).deleteById(anyInt());
    }

    @Test
    public void shouldAddDocumentToFundingCall() {
        // given
        var fundingCallId = 1;
        var documentFileDTO = new DocumentFileDTO();
        documentFileDTO.setAccessRights(AccessRights.RESTRICTED_ACCESS);

        var fundingCall = new FundingCall();
        fundingCall.setId(fundingCallId);
        fundingCall.setCallDocuments(new HashSet<>());

        var savedDocumentFile = new DocumentFile();
        savedDocumentFile.setId(100);

        var expectedResponse = new DocumentFileResponseDTO();
        expectedResponse.setId(100);

        when(fundingCallRepository.findById(fundingCallId))
            .thenReturn(Optional.of(fundingCall));
        when(documentFileService.saveNewDocument(any(DocumentFileDTO.class), eq(false)))
            .thenReturn(savedDocumentFile);
        when(fundingCallRepository.save(any(FundingCall.class)))
            .thenReturn(fundingCall);

        try (var documentFileConverterMock = mockStatic(DocumentFileConverter.class)) {
            documentFileConverterMock.when(() ->
                    DocumentFileConverter.toDTO(savedDocumentFile))
                .thenReturn(expectedResponse);

            // when
            var result = fundingCallService.addFundingCallDocument(
                fundingCallId, documentFileDTO);

            // then
            assertNotNull(result);
            assertEquals(100, result.getId());
            verify(fundingCallRepository).findById(fundingCallId);
            verify(documentFileService).saveNewDocument(any(DocumentFileDTO.class), eq(false));
            verify(fundingCallRepository).save(any(FundingCall.class));
            documentFileConverterMock.verify(() ->
                DocumentFileConverter.toDTO(savedDocumentFile), times(1));
        }
    }

    @Test
    public void shouldThrowExceptionWhenAddingDocumentToNonExistentFundingCall() {
        // given
        var fundingCallId = 999;
        var documentFileDTO = new DocumentFileDTO();

        when(fundingCallRepository.findById(fundingCallId))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
            fundingCallService.addFundingCallDocument(fundingCallId, documentFileDTO));
        verify(fundingCallRepository).findById(fundingCallId);
        verify(documentFileService, never()).saveNewDocument(any(), anyBoolean());
    }

    @Test
    public void shouldUpdateFundingCallDocument() {
        // given
        var documentFileDTO = new DocumentFileDTO();
        documentFileDTO.setAccessRights(AccessRights.RESTRICTED_ACCESS);
        documentFileDTO.setId(100);

        var expectedResponse = new DocumentFileResponseDTO();
        expectedResponse.setId(100);

        when(documentFileService.editDocumentFile(any(DocumentFileDTO.class), eq(false)))
            .thenReturn(expectedResponse);

        // when
        var result = fundingCallService.updateFundingCallDocument(documentFileDTO);

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
            fundingCallService.updateFundingCallDocument(documentFileDTO));
        verify(documentFileService).editDocumentFile(any(DocumentFileDTO.class), eq(false));
    }

    @Test
    public void shouldDeleteFundingCallDocument() {
        // given
        var callFileId = 100;
        var fundingCallId = 1;

        var documentFile = new DocumentFile();
        documentFile.setId(callFileId);

        var fundingCall = new FundingCall();
        fundingCall.setId(fundingCallId);
        fundingCall.setCallDocuments(new HashSet<>(Set.of(documentFile)));

        when(documentFileService.findOne(callFileId))
            .thenReturn(documentFile);
        when(fundingCallRepository.findById(fundingCallId))
            .thenReturn(Optional.of(fundingCall));
        doNothing().when(documentFileService).delete(callFileId);
        when(fundingCallRepository.save(any(FundingCall.class)))
            .thenReturn(fundingCall);

        // when
        fundingCallService.deleteFundingCallDocument(callFileId, fundingCallId);

        // then
        verify(documentFileService).findOne(callFileId);
        verify(fundingCallRepository).findById(fundingCallId);
        verify(documentFileService).delete(callFileId);
        verify(fundingCallRepository).save(any(FundingCall.class));
    }

    @Test
    public void shouldThrowExceptionWhenDeletingDocumentFromNonExistentFundingCall() {
        // given
        var callFileId = 100;
        var fundingCallId = 999;

        var documentFile = new DocumentFile();
        documentFile.setId(callFileId);

        when(documentFileService.findOne(callFileId))
            .thenReturn(documentFile);
        when(fundingCallRepository.findById(fundingCallId))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
            fundingCallService.deleteFundingCallDocument(callFileId, fundingCallId));
        verify(documentFileService).findOne(callFileId);
        verify(fundingCallRepository).findById(fundingCallId);
        verify(documentFileService, never()).delete(anyInt());
    }

    @Test
    public void shouldIndexSingleFundingCall() {
        // given
        var fundingCall = new FundingCall();
        fundingCall.setId(1);

        var fundingProgram = new FundingProgram();
        fundingProgram.setId(10);
        fundingCall.setFundingProgram(fundingProgram);

        var fundingCallIndex = new FundingCallIndex();

        when(fundingCallIndexRepository.save(any(FundingCallIndex.class)))
            .thenReturn(fundingCallIndex);

        // when
        fundingCallService.indexFundingCall(fundingCall, fundingCallIndex);

        // then
        verify(fundingCallIndexRepository).save(any(FundingCallIndex.class));
    }
}
