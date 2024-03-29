package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitRequestDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.GeoLocation;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.repository.person.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.OrganisationUnitsRelationRepository;
import rs.teslaris.core.service.impl.person.OrganisationUnitServiceImpl;
import rs.teslaris.core.service.impl.person.cruddelegate.OrganisationUnitsRelationJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.OrganisationUnitReferenceConstraintViolation;
import rs.teslaris.core.util.exceptionhandling.exception.SelfRelationException;
import rs.teslaris.core.util.search.SearchRequestType;

@SpringBootTest
public class OrganisationUnitServiceTest {

    @Mock
    private OrganisationUnitRepository organisationUnitRepository;

    @Mock
    private OrganisationUnitIndexRepository organisationUnitIndexRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    @Mock
    private DocumentFileService documentFileService;

    @Mock
    private ResearchAreaService researchAreaService;

    @Mock
    private OrganisationUnitsRelationJPAServiceImpl organisationUnitsRelationJPAService;

    @Mock
    private SearchService<OrganisationUnitIndex> searchService;

    @InjectMocks
    private OrganisationUnitServiceImpl organisationUnitService;


    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(organisationUnitService, "relationApprovedByDefault", true);
        ReflectionTestUtils.setField(organisationUnitService, "organisationUnitApprovedByDefault",
            true);
    }

    @Test
    public void shouldReturnOrganisationUnitWhenItExists() {
        // given
        Integer id = 21;
        OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setId(id);

        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(id))
            .thenReturn(Optional.of(organisationUnit));

        // when
        OrganisationUnit result = organisationUnitService.findOrganisationUnitById(id);

        // then
        assertEquals(organisationUnit, result);
        verify(organisationUnitRepository, times(1)).findByIdWithLangDataAndResearchArea(id);

    }

    @Test
    public void shouldThrowNotFoundExceptionWhenOrganisationUnitDoesNotExist() {
        // given
        Integer id = 1;

        // when
        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(id))
            .thenReturn(Optional.empty());

        // then (NotFoundException should be thrown)
        assertThrows(NotFoundException.class,
            () -> organisationUnitService.findOrganisationUnitById(id));
        verify(organisationUnitRepository, times(1)).findByIdWithLangDataAndResearchArea(id);

    }

    @Test
    void shouldReturnOrganisationUnitPage() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setNameAbbreviation("ID1");
        organisationUnit.setResearchAreas(new HashSet<>());
        organisationUnit.setKeyword(new HashSet<>());
        organisationUnit.setName(new HashSet<>());
        organisationUnit.setLocation(new GeoLocation());
        organisationUnit.setContact(new Contact());
        Page<OrganisationUnit>
            organisationUnitPage = new PageImpl<>(List.of(organisationUnit), pageable, 1);

        // when
        when(organisationUnitRepository.findAllWithLangData(pageable)).thenReturn(
            organisationUnitPage);

        Page<OrganisationUnitDTO> result = organisationUnitService.findOrganisationUnits(pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(organisationUnit.getNameAbbreviation(),
            result.getContent().get(0).getNameAbbreviation());
        verify(organisationUnitRepository, times(1)).findAllWithLangData(pageable);
    }

    @Test
    public void shouldReturnOrganisationUnitsRelationWhenItExists() {
        // given
        var expected = new OrganisationUnitsRelation();
        when(organisationUnitsRelationJPAService.findOne(1)).thenReturn(expected);

        // when
        var result = organisationUnitService.findOrganisationUnitsRelationById(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenOrganisationUnitsRelationDoesNotExist() {
        // given
        when(organisationUnitsRelationJPAService.findOne(1)).thenThrow(NotFoundException.class);

        // when
        assertThrows(NotFoundException.class,
            () -> organisationUnitService.findOrganisationUnitsRelationById(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldReturnOrganisationUnitsRelations() {
        // given
        Integer sourceId = 1;
        Integer targetId = 2;
        var pageable = Mockito.mock(Pageable.class);

        var ou = new OrganisationUnit();
        ou.setName(new HashSet<>());
        var relation = new OrganisationUnitsRelation();
        relation.setSourceAffiliationStatement(new HashSet<>());
        relation.setTargetAffiliationStatement(new HashSet<>());
        relation.setSourceOrganisationUnit(ou);
        relation.setTargetOrganisationUnit(ou);
        relation.setProofs(new HashSet<>());
        var relations = new ArrayList<OrganisationUnitsRelation>();
        relations.add(relation);
        var page = new PageImpl<>(relations);

        when(
            organisationUnitsRelationRepository.getRelationsForOrganisationUnits(pageable, sourceId,
                targetId)).thenReturn(page);

        // when
        var result =
            organisationUnitService.getOrganisationUnitsRelations(sourceId, targetId, pageable);

        // then
        assertEquals(relations.size(), result.getTotalElements());
    }

    @Test
    public void shouldDeleteOrganisationUnitsRelation() {
        // given
        var relationId = 1;

        // when
        organisationUnitService.deleteOrganisationUnitsRelation(relationId);

        // then
        verify(organisationUnitsRelationJPAService).delete(relationId);
    }

    @Test
    public void shouldApproveRelation() {
        // given
        var relationId = 1;
        var approve = true;

        var relationToApprove = new OrganisationUnitsRelation();
        relationToApprove.setApproveStatus(ApproveStatus.REQUESTED);

        when(organisationUnitsRelationJPAService.findOne(relationId)).thenReturn(relationToApprove);

        // when
        organisationUnitService.approveRelation(relationId, approve);

        // then
        verify(organisationUnitsRelationJPAService).save(relationToApprove);
    }

    @Test
    public void shouldCreateOrganisationUnitsRelation() {
        // given
        var relationDTO = new OrganisationUnitsRelationDTO();
        relationDTO.setSourceOrganisationUnitId(1);
        relationDTO.setTargetOrganisationUnitId(2);

        var newRelation = Mockito.mock(OrganisationUnitsRelation.class);
        when(organisationUnitsRelationJPAService.save(
            any(OrganisationUnitsRelation.class))).thenReturn(newRelation);
        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(any())).thenReturn(
            Optional.of(new OrganisationUnit()));

        // when
        var result = organisationUnitService.createOrganisationUnitsRelation(relationDTO);

        // then
        assertNotNull(result);
        assertEquals(newRelation, result);
        verify(organisationUnitsRelationJPAService).save(any(OrganisationUnitsRelation.class));
    }

    @Test
    public void shouldThrowSelfRelationExceptionWhenCreatingOrganisationUnitsRelationWithSameSourceAndTarget() {
        // given
        var relationDTO = new OrganisationUnitsRelationDTO();
        relationDTO.setSourceOrganisationUnitId(1);
        relationDTO.setTargetOrganisationUnitId(1);

        // when
        assertThrows(SelfRelationException.class,
            () -> organisationUnitService.createOrganisationUnitsRelation(relationDTO));
    }

    @Test
    public void shouldEditOrganisationUnitsRelation() {
        // given
        var relationDTO = new OrganisationUnitsRelationDTO();
        relationDTO.setSourceOrganisationUnitId(1);
        relationDTO.setSourceOrganisationUnitId(2);
        var relationId = 1;

        var relation = new OrganisationUnitsRelation();
        relation.setSourceAffiliationStatement(new HashSet<>());
        relation.setTargetAffiliationStatement(new HashSet<>());

        when(organisationUnitsRelationJPAService.findOne(relationId)).thenReturn(relation);
        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(any())).thenReturn(
            Optional.of(new OrganisationUnit()));

        // when
        organisationUnitService.editOrganisationUnitsRelation(relationDTO, relationId);

        // then
        verify(organisationUnitsRelationJPAService).save(relation);
    }

    @Test
    public void shouldAddInvolvementProofWhenInvolvementExists() {
        // given
        var relation = new OrganisationUnitsRelation();
        relation.setProofs(new HashSet<>());

        when(organisationUnitsRelationJPAService.findOne(1)).thenReturn(relation);
        when(documentFileService.saveNewDocument(any(), eq(true))).thenReturn(new DocumentFile());

        // when
        organisationUnitService.addRelationProofs(
            List.of(new DocumentFileDTO(), new DocumentFileDTO()), 1);

        //then
        verify(organisationUnitsRelationJPAService, times(2)).save(relation);
    }

    @Test
    public void shouldDeleteProofWhenDocumentExists() {
        // given
        var df = new DocumentFile();
        df.setServerFilename("UUID");
        var relation = new OrganisationUnitsRelation();
        relation.setProofs(new HashSet<>(Set.of(df)));

        when(organisationUnitsRelationRepository.findById(1)).thenReturn(Optional.of(relation));
        when(documentFileService.findDocumentFileById(1)).thenReturn(df);

        // when
        organisationUnitService.deleteRelationProof(1, 1);

        //then
//        verify(organisationUnitsRelationRepository, times(1)).save(relation);
//        verify(documentFileService, times(1)).deleteDocumentFile(df.getServerFilename());
    }

    @Test
    void shouldCreateOrganisationUnits() {
        var organisationUnitDTORequest = new OrganisationUnitRequestDTO();
        // Set properties for organisationUnitDTORequest

        MultiLingualContent name = new MultiLingualContent();
        name.setContent("A1");
        name.setLanguage(new LanguageTag());

        MultiLingualContent keyword = new MultiLingualContent();
        keyword.setContent("B1");
        keyword.setLanguage(new LanguageTag());

        ResearchArea researchArea = new ResearchArea();
        researchArea.setId(1);
        researchArea.setName(new HashSet<>());
        researchArea.setDescription(new HashSet<>());
        List<ResearchArea> researchAreas = List.of(researchArea);

        organisationUnitDTORequest.setName(new ArrayList<>());
        organisationUnitDTORequest.setKeyword(new ArrayList<>());
        organisationUnitDTORequest.setResearchAreasId(List.of(1));
        organisationUnitDTORequest.setLocation(new GeoLocationDTO(1.0, 2.0, "NOWHERE"));
        organisationUnitDTORequest.setContact(new ContactDTO("a", "b"));

        when(
            multilingualContentService.getMultilingualContent(organisationUnitDTORequest.getName()))
            .thenReturn(Set.of(name));
        when(multilingualContentService.getMultilingualContent(
            organisationUnitDTORequest.getKeyword()))
            .thenReturn(Set.of(keyword));
        when(researchAreaService.getResearchAreasByIds(
            organisationUnitDTORequest.getResearchAreasId()))
            .thenReturn(researchAreas);
        when(organisationUnitRepository.save(any(OrganisationUnit.class))).thenAnswer(
            invocation -> {
                OrganisationUnit organisationUnit = invocation.getArgument(0);
                organisationUnit.setId(1);
                return organisationUnit;
            });

        var result =
            organisationUnitService.createOrganisationUnit(organisationUnitDTORequest);

//        assertEquals(Set.of(name), result.getName());
//        assertEquals(organisationUnitDTORequest.getNameAbbreviation(),
//            result.getNameAbbreviation());
//        assertEquals(Set.of(keyword), result.getKeyword());
//        assertEquals(new HashSet<>(researchAreas), result.getResearchAreas());
//        assertEquals(location.getLatitude(), result.getLocation().getLatitude());
//        assertEquals(ApproveStatus.APPROVED, result.getApproveStatus());
//        assertEquals(contact, result.getContact());

//        verify(multilingualContentService, times(1)).getMultilingualContent(
//            organisationUnitDTORequest.getName());
//        verify(multilingualContentService, times(1)).getMultilingualContent(
//            organisationUnitDTORequest.getKeyword());
        verify(researchAreaService, times(1)).getResearchAreasByIds(
            organisationUnitDTORequest.getResearchAreasId());
        verify(organisationUnitRepository, times(1)).save(any(OrganisationUnit.class));
    }


    @Test
    void shouldEditOrganisationUnits() {
        var organisationUnitDTORequest = new OrganisationUnitRequestDTO();
        organisationUnitDTORequest.setName(List.of(new MultilingualContentDTO()));
        organisationUnitDTORequest.setKeyword(List.of(new MultilingualContentDTO()));
        organisationUnitDTORequest.setResearchAreasId(List.of(1));
        organisationUnitDTORequest.setLocation(new GeoLocationDTO(10.0, 20.0, "NOWHERE"));
        organisationUnitDTORequest.setContact(new ContactDTO("b", "b"));

        OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setName(
            Stream.of(new MultiLingualContent()).collect(Collectors.toSet()));
        organisationUnit.setKeyword(
            Stream.of(new MultiLingualContent()).collect(Collectors.toSet()));
        organisationUnit.setResearchAreas(
            Stream.of(new ResearchArea()).collect(Collectors.toSet()));
        organisationUnit.setLocation(new GeoLocation(1.0, 2.0, "NOWHERE"));
        organisationUnit.setContact(new Contact("a", "a"));

        organisationUnit.getName().clear();
        Integer organisationUnitId = 1;

//        OrganisationUnit organisationUnit = new OrganisationUnit();

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(organisationUnitRepository.getReferenceById(any())).thenReturn(organisationUnit);
        when(researchAreaService.getResearchAreasByIds(any())).thenReturn(Collections.emptyList());
        when(organisationUnitRepository.save(any(OrganisationUnit.class))).thenReturn(
            organisationUnit);

        // Act
        OrganisationUnit editedOrganisationUnit =
            organisationUnitService.editOrganisationUnit(organisationUnitDTORequest,
                organisationUnitId);

        // Assert
        assertNotNull(editedOrganisationUnit);
        assertEquals(organisationUnit, editedOrganisationUnit);
//        assertEquals(organisationUnitDTORequest.getName().stream().findFirst().get().getContent(),
//            editedOrganisationUnit.getName().stream().findFirst().get().getContent());
//        assertEquals(
//            organisationUnitDTORequest.getKeyword().stream().findFirst().get().getContent(),
//            editedOrganisationUnit.getKeyword().stream().findFirst().get().getContent());
        verify(multilingualContentService, times(2)).getMultilingualContent(any());
        verify(researchAreaService, times(1)).getResearchAreasByIds(any());
        verify(organisationUnitRepository, times(1)).save(any(OrganisationUnit.class));
    }

    @Test
    public void shouldDeleteOrganisationalUnit() {
        Integer organisationUnitId = 1;
        OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setId(organisationUnitId);
        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(
            organisationUnitId)).thenReturn(
            Optional.of(organisationUnit));

        organisationUnitService.delete(organisationUnitId);

        verify(organisationUnitRepository, times(1)).save(organisationUnit);
        verify(organisationUnitRepository, never()).delete(any());
    }

    @Test
    public void shouldIgnoreNullOrganisationalUnit() {
        Integer organisationUnitId = 1;
        var ou = new OrganisationUnit();
        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(
            organisationUnitId)).thenReturn(Optional.of(ou));

        organisationUnitService.delete(organisationUnitId);

        verify(organisationUnitRepository, times(1)).save(ou);
        verify(organisationUnitRepository, never()).delete(any());

    }

    @Test
    public void shouldEditOrganisationalUnitApproveStatus() {
        Integer organisationUnitId = 1;
        ApproveStatus approveStatus = ApproveStatus.APPROVED;
        OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setId(organisationUnitId);
        organisationUnit.setApproveStatus(ApproveStatus.REQUESTED);

        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(
            organisationUnitId)).thenReturn(Optional.of(organisationUnit));

        OrganisationUnit result =
            organisationUnitService.editOrganisationalUnitApproveStatus(approveStatus,
                organisationUnitId);

        verify(organisationUnitRepository, times(1)).save(organisationUnit);
        assertEquals(approveStatus, result.getApproveStatus());
    }

    @Test
    public void shouldThrowExceptionWhenOrganisationalUnitNotFound() {
        Integer organisationUnitId = 1;
        ApproveStatus approveStatus = ApproveStatus.APPROVED;
        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(
            organisationUnitId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            organisationUnitService.editOrganisationalUnitApproveStatus(approveStatus,
                organisationUnitId);
        });
    }

    @Test
    public void shouldFindOrganisationUnitWhenSearchingWithSimpleQuery() {
        // Given
        var tokens = Arrays.asList("Fakultet tehnickih nauka", "FTN");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(List.of(new OrganisationUnitIndex(), new OrganisationUnitIndex())));

        // When
        var result =
            organisationUnitService.searchOrganisationUnits(new ArrayList<>(tokens), pageable,
                SearchRequestType.SIMPLE);

        // Then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    public void shouldDeleteUnusedOU() {
        // Given
        int organisationUnitId = 1;
        when(organisationUnitRepository.hasEmployees(organisationUnitId)).thenReturn(false);
        when(organisationUnitRepository.hasThesis(organisationUnitId)).thenReturn(false);
        when(organisationUnitRepository.hasRelation(organisationUnitId)).thenReturn(false);
        when(organisationUnitRepository.hasInvolvement(organisationUnitId)).thenReturn(false);
        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(
            organisationUnitId)).thenReturn(Optional.of(new OrganisationUnit()));

        // When
        assertDoesNotThrow(
            () -> organisationUnitService.deleteOrganisationUnit(organisationUnitId));

        // Then
        verify(organisationUnitRepository, times(1)).save(any());
    }

    @Test
    public void shouldThrowReferenceConstraintViolationWhenDeletingOUInUse() {
        // Given
        int organisationUnitId = 1;
        when(organisationUnitRepository.hasEmployees(organisationUnitId)).thenReturn(true);

        // When
        var exception = assertThrows(
            OrganisationUnitReferenceConstraintViolation.class,
            () -> organisationUnitService.deleteOrganisationUnit(organisationUnitId)
        );

        // Then (OrganisationUnitReferenceConstraintViolation exception should be thrown)
        assertEquals("Organisation unit is already in use.", exception.getMessage());
        verify(organisationUnitRepository, never()).delete(any());
    }

    @Test
    public void shouldGetOUCount() {
        // Given
        var expectedCount = 42L;
        when(organisationUnitIndexRepository.count()).thenReturn(expectedCount);

        // When
        long actualCount = organisationUnitService.getOrganisationUnitsCount();

        // Then
        assertEquals(expectedCount, actualCount);
        verify(organisationUnitIndexRepository, times(1)).count();
    }
}
