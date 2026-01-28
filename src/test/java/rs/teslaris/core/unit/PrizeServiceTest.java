package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.PrizeDTO;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexmodel.PrizeIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.indexrepository.PrizeIndexRepository;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.Prize;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.repository.person.PrizeRepository;
import rs.teslaris.core.service.impl.person.PrizeServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;

@SpringBootTest
public class PrizeServiceTest {

    @Mock
    private PrizeRepository prizeRepository;

    @Mock
    private PersonService personService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private DocumentFileService documentFileService;

    @Mock
    private PersonIndexRepository personIndexRepository;

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private PrizeIndexRepository prizeIndexRepository;

    @Mock
    private SearchService<PrizeIndex> searchService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private PrizeServiceImpl prizeService;


    @Test
    public void shouldAddPrize() {
        // Given
        var dto = new PrizeDTO();

        var person = new Person();
        person.setName(new PersonName("John", null, "Doe", null, null));

        var newPrize = new Prize();

        when(personService.findOne(1)).thenReturn(person);
        when(prizeRepository.save(any(Prize.class))).thenReturn(
            newPrize);
        when(personIndexRepository.findByDatabaseId(any())).thenReturn(
            Optional.of(new PersonIndex() {{
                setEmploymentInstitutionsIdHierarchy(List.of(1, 2, 3));
            }}));
        when(commissionRepository.findCommissionsThatAssessedPrize(any())).thenReturn(
            Collections.emptyList());
        when(commissionRepository.findAssessmentClassificationBasicInfoForPrizeAndCommissions(any(),
            any())).thenReturn(Collections.emptyList());

        // When
        var responseDTO = prizeService.addPrize(1, dto);

        // Then
        assertNotNull(responseDTO);
        verify(prizeIndexRepository, times(1)).save(any());
    }

    @Test
    public void shouldUpdatePrize() {
        // Given
        PrizeDTO dto = new PrizeDTO();

        Prize prize = new Prize();
        prize.setId(1);
        prize.setPerson(new Person() {{
            setId(1);
            setName(new PersonName("John", null, "Doe", null, null));
        }});

        when(prizeRepository.findById(1)).thenReturn(
            Optional.of(new Prize()));
        when(prizeRepository.save(any(Prize.class))).thenReturn(
            prize);
        when(personIndexRepository.findByDatabaseId(any())).thenReturn(
            Optional.of(new PersonIndex() {{
                setEmploymentInstitutionsIdHierarchy(List.of(1, 2, 3));
            }}));
        when(commissionRepository.findCommissionsThatAssessedPrize(any())).thenReturn(
            Collections.emptyList());
        when(commissionRepository.findAssessmentClassificationBasicInfoForPrizeAndCommissions(any(),
            any())).thenReturn(Collections.emptyList());

        // When
        var responseDTO = prizeService.updatePrize(1, dto);

        // Then
        assertNotNull(responseDTO);
    }

    @Test
    public void shouldDeletePrize() {
        // Given
        var person = new Person();
        var prize = new Prize();
        prize.setId(1);
        var prizes = new HashSet<Prize>();
        prizes.add(prize);
        person.setPrizes(prizes);

        when(personService.findOne(1)).thenReturn(person);
        when(prizeRepository.findById(1)).thenReturn(
            Optional.of(new Prize()));
        doNothing().when(prizeRepository).deleteById(1);

        // When
        prizeService.deletePrize(1, 1);

        // Then
        assertTrue(person.getPrizes().isEmpty());
    }

    @Test
    public void shouldAddProof() {
        // Given
        var proof = new DocumentFileDTO();
        var prize = new Prize();
        prize.setId(1);
        prize.setPerson(new Person());
        var documentFile = new DocumentFile();
        documentFile.setId(1);

        when(prizeRepository.findById(1)).thenReturn(Optional.of(prize));
        when(documentFileService.saveNewPersonalDocument(proof, false,
            prize.getPerson())).thenReturn(documentFile);

        // When
        var responseDTO = prizeService.addProof(1, proof);

        // Then
        assertNotNull(responseDTO);
        assertEquals(1, responseDTO.getId());
    }

    @Test
    public void shouldUpdateProof() {
        // Given
        var updatedProof = new DocumentFileDTO();
        updatedProof.setId(1);

        var documentFile = new DocumentFileResponseDTO();
        documentFile.setId(1);

        when(documentFileService.editDocumentFile(updatedProof, false)).thenReturn(documentFile);

        // When
        var responseDTO = prizeService.updateProof(updatedProof);

        // Then
        assertNotNull(responseDTO);
        assertEquals(1, responseDTO.getId());
    }

    @Test
    public void shouldDeleteProof() {
        // Given
        Prize prize = new Prize();
        DocumentFile documentFile = new DocumentFile();
        documentFile.setId(1);
        prize.getProofs().add(documentFile);

        when(prizeRepository.findById(1)).thenReturn(Optional.of(prize));
        when(documentFileService.findDocumentFileById(1)).thenReturn(documentFile);
        doNothing().when(documentFileService).deleteDocumentFile(anyString());

        // When
        prizeService.deleteProof(1, 1);

        // Then
        assertTrue(prize.getProofs().isEmpty());
    }

    @Test
    public void shouldFindPrizesWhenSearchingWithSimpleTokens() {
        // given
        var tokens = Arrays.asList("nobel", "prize", "physics");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(
                List.of(new PrizeIndex(), new PrizeIndex(), new PrizeIndex())
            )
        );

        // when
        var result = prizeService.searchPrizes(
            tokens, pageable, null, null, null
        );

        // then
        assertEquals(3L, result.getTotalElements());
        assertEquals(3, result.getContent().size());

        verify(searchService, times(1)).runQuery(
            any(), eq(pageable), eq(PrizeIndex.class), eq("prize")
        );
    }

    @Test
    public void shouldApplyInstitutionFilterWhenInstitutionIdProvided() {
        // given
        var tokens = List.of("award");
        var pageable = PageRequest.of(0, 5);
        var institutionId = 123;
        Integer personId = null;
        Integer commissionId = null;

        var subInstitutionIds = Arrays.asList(123, 456, 789);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(subInstitutionIds);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(List.of(new PrizeIndex()))
        );

        // when
        var result = prizeService.searchPrizes(
            tokens, pageable, personId, institutionId, commissionId
        );

        // then
        assertEquals(1L, result.getTotalElements());

        verify(organisationUnitService, times(1))
            .getOrganisationUnitIdsFromSubHierarchy(institutionId);
        verify(searchService, times(1)).runQuery(
            any(), eq(pageable), eq(PrizeIndex.class), eq("prize")
        );
    }

    @Test
    public void shouldExcludeCommissionWhenCommissionIdProvided() {
        // given
        var tokens = List.of("recognition");
        var pageable = PageRequest.of(1, 20); // Test with second page
        var commissionId = 999;
        Integer institutionId = null;
        var personId = 555;

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(List.of(new PrizeIndex(), new PrizeIndex()), pageable, 42)
        );

        // when
        var result = prizeService.searchPrizes(
            tokens, pageable, personId, institutionId, commissionId
        );

        // then
        assertEquals(42L, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals(1, result.getPageable().getPageNumber());

        verify(searchService, times(1)).runQuery(
            any(), eq(pageable), eq(PrizeIndex.class), eq("prize")
        );
        verify(organisationUnitService, never()).getOrganisationUnitIdsFromSubHierarchy(anyInt());
    }

    @Test
    public void shouldHandleSpecialTokenFormatsInSearch() {
        // given
        var tokens = Arrays.asList(
            "\\\"field medal\\\"", // quoted phrase
            "mathematics*",        // wildcard with asterisk
            "achievement.",        // wildcard with dot
            "normal"               // normal token
        );
        var pageable = PageRequest.of(0, 15);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            Page.empty()
        );

        // when
        var result = prizeService.searchPrizes(
            tokens, pageable, null, null, null
        );

        // then
        assertEquals(0L, result.getTotalElements());
        assertTrue(result.isEmpty());

        verify(searchService, times(1)).runQuery(
            any(), eq(pageable), eq(PrizeIndex.class), eq("prize")
        );
    }

    @Test
    public void shouldHandleEmptyTokensList() {
        // given
        List<String> tokens = List.of();
        var pageable = PageRequest.of(0, 10);
        var institutionId = 100;

        List<Integer> subInstitutionIds = Arrays.asList(100, 101, 102);
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId))
            .thenReturn(subInstitutionIds);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(List.of())
        );

        // when
        var result = prizeService.searchPrizes(
            tokens, pageable, null, institutionId, null
        );

        // then
        assertEquals(0L, result.getTotalElements());
        assertTrue(result.isEmpty());

        verify(searchService, times(1)).runQuery(
            any(), eq(pageable), eq(PrizeIndex.class), eq("prize")
        );
    }
}
