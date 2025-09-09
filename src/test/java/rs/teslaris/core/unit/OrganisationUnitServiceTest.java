package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.commontypes.ProfilePhotoOrLogoDTO;
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
import rs.teslaris.core.model.commontypes.ProfilePhotoOrLogo;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitRelationType;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.repository.institution.OrganisationUnitRepository;
import rs.teslaris.core.repository.institution.OrganisationUnitsRelationRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.service.impl.institution.OrganisationUnitServiceImpl;
import rs.teslaris.core.service.impl.person.cruddelegate.OrganisationUnitsRelationJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.util.ImageUtil;
import rs.teslaris.core.util.Triple;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.OrganisationUnitReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.SelfRelationException;
import rs.teslaris.core.util.search.SearchFieldsLoader;
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

    @Mock
    private IndexBulkUpdateService indexBulkUpdateService;

    @Mock
    private InvolvementRepository involvementRepository;

    @Mock
    private SearchFieldsLoader searchFieldsLoader;

    @Mock
    private FileService fileService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private OrganisationUnitServiceImpl organisationUnitService;


    private static Stream<Arguments> argumentSources() {
        return Stream.of(
            Arguments.of(null, null, null),
            Arguments.of(null, 2, true),
            Arguments.of(1, null, false),
            Arguments.of(1, 2, true)
        );
    }

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
    public void shouldThrowNotFoundExceptionWhenOrganisationUnitDoesNotExistForOldId() {
        // given
        Integer id = 1;

        // when
        when(organisationUnitRepository.findOrganisationUnitByOldIdsContains(id)).thenReturn(
            Optional.empty());

        // then (NotFoundException should be thrown)
        assertThrows(NotFoundException.class,
            () -> organisationUnitService.readOrganisationUnitForOldId(id));
    }

    @Test
    public void shouldReturnOrganisationUnitForOldIdWhenItExists() {
        // given
        var organisationUnit = new OrganisationUnit();
        organisationUnit.setId(21);

        when(organisationUnitRepository.findOrganisationUnitByOldIdsContains(12))
            .thenReturn(Optional.of(organisationUnit));

        // when
        var result = organisationUnitService.readOrganisationUnitForOldId(12);

        // then
        assertEquals(organisationUnit.getId(), result.getId());
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
        var relation = new OrganisationUnitsRelation();
        relation.setSourceOrganisationUnit(ou);
        relation.setTargetOrganisationUnit(ou);
        var relations = new ArrayList<OrganisationUnitsRelation>();
        relations.add(relation);

        when(organisationUnitsRelationRepository.getRelationsForOrganisationUnits(
            sourceId)).thenReturn(relations);

        // when
        var result =
            organisationUnitService.getOrganisationUnitsRelations(sourceId);

        // then
        assertEquals(relations.size(), result.size());
    }

    @Test
    public void shouldDeleteOrganisationUnitsRelation() {
        // given
        var relationId = 1;
        when(organisationUnitsRelationJPAService.findOne(relationId)).thenReturn(
            new OrganisationUnitsRelation() {{
                setSourceOrganisationUnit(new OrganisationUnit());
            }});
        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(any())).thenReturn(
            Optional.of(new OrganisationUnit()));

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
        relationDTO.setRelationType(OrganisationUnitRelationType.BELONGS_TO);

        var organisationUnitsRelation = new OrganisationUnitsRelation();
        organisationUnitsRelation.setSourceOrganisationUnit(new OrganisationUnit());

        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(any())).thenReturn(
            Optional.of(new OrganisationUnit()));
        when(organisationUnitsRelationRepository.getSuperOU(any())).thenReturn(Optional.empty());
        when(organisationUnitsRelationJPAService.save(any())).thenReturn(organisationUnitsRelation);
        when(organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
            any())).thenReturn(Optional.empty());

        // when
        var result = organisationUnitService.createOrganisationUnitsRelation(relationDTO);

        // then
        assertNotNull(result);
        assertEquals(organisationUnitsRelation, result);
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
            organisationUnitService.createOrganisationUnit(organisationUnitDTORequest, true);

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
        // given
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
        organisationUnit.setApproveStatus(ApproveStatus.APPROVED);
        var organisationUnitId = 1;

//        OrganisationUnit organisationUnit = new OrganisationUnit();

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));
        when(organisationUnitRepository.getReferenceById(any())).thenReturn(organisationUnit);
        when(researchAreaService.getResearchAreasByIds(any())).thenReturn(Collections.emptyList());
        when(organisationUnitRepository.save(any(OrganisationUnit.class))).thenReturn(
            organisationUnit);

        // when
        OrganisationUnit editedOrganisationUnit =
            organisationUnitService.editOrganisationUnit(organisationUnitId,
                organisationUnitDTORequest);

        // then
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

    @ParameterizedTest
    @MethodSource("argumentSources")
    public void shouldFindOrganisationUnitWhenSearchingWithSimpleQuery(Integer personId,
                                                                       Integer topLevelInstitutionId,
                                                                       Boolean onlyReturnOnesWhichCanHarvest) {
        // Given
        var tokens = Arrays.asList("Fakultet tehnickih nauka", "FTN");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(List.of(new OrganisationUnitIndex(), new OrganisationUnitIndex())));
        when(involvementRepository.findActiveEmploymentInstitutionIds(any())).thenReturn(
            List.of(1, 2));

        // When
        var result =
            organisationUnitService.searchOrganisationUnits(new ArrayList<>(tokens), pageable,
                SearchRequestType.SIMPLE, personId, topLevelInstitutionId,
                onlyReturnOnesWhichCanHarvest, null, null, null);

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
            OrganisationUnitReferenceConstraintViolationException.class,
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

    @Test
    public void shouldReindexOrganisationUnits() {
        // Given
        var ou1 = new OrganisationUnit();
        var ou2 = new OrganisationUnit();
        var ou3 = new OrganisationUnit();
        var organisationUnits = Arrays.asList(ou1, ou2, ou3);
        var page1 = new PageImpl<>(organisationUnits.subList(0, 2), PageRequest.of(0, 10),
            organisationUnits.size());
        var page2 = new PageImpl<>(organisationUnits.subList(2, 3), PageRequest.of(1, 10),
            organisationUnits.size());

        when(organisationUnitRepository.findAll(any(PageRequest.class))).thenReturn(page1, page2);

        // When
        organisationUnitService.reindexOrganisationUnits();

        // Then
        verify(organisationUnitIndexRepository, times(1)).deleteAll();
        verify(organisationUnitRepository, atLeastOnce()).findAll(any(PageRequest.class));
        verify(organisationUnitIndexRepository, atLeastOnce()).save(
            any(OrganisationUnitIndex.class));
    }

    @Test
    void shouldFindOrganisationUnitByOldId() {
        // Given
        var oldId = 123;
        var expectedUnit = new OrganisationUnit();
        when(organisationUnitRepository.findOrganisationUnitByOldIdsContains(oldId)).thenReturn(
            Optional.of(expectedUnit));

        // When
        var actualUnit = organisationUnitService.findOrganisationUnitByOldId(oldId);

        // Then
        assertEquals(expectedUnit, actualUnit);
        verify(organisationUnitRepository, times(1)).findOrganisationUnitByOldIdsContains(oldId);
    }

    @Test
    void shouldReturnNullWhenOldIdDoesNotExist() {
        // Given
        Integer oldId = 123;
        when(organisationUnitRepository.findOrganisationUnitByOldIdsContains(oldId)).thenReturn(
            Optional.empty());

        // When
        var actualUnit = organisationUnitService.findOrganisationUnitByOldId(oldId);

        // Then
        assertNull(actualUnit);
        verify(organisationUnitRepository, times(1)).findOrganisationUnitByOldIdsContains(oldId);
    }

    @Test
    void testGetOrganisationUnitsRelationsChain() {
        // Given
        var leafUnit = new OrganisationUnit();
        leafUnit.setId(1);
        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(
            leafUnit.getId())).thenReturn(Optional.of(leafUnit));

        var middleUnit = new OrganisationUnit();
        middleUnit.setId(2);
        when(organisationUnitsRelationRepository.getSuperOU(1)).thenReturn(Optional.of(
            new OrganisationUnitsRelation(new HashSet<>(), new HashSet<>(),
                OrganisationUnitRelationType.BELONGS_TO, null, null, ApproveStatus.APPROVED,
                new HashSet<>(), leafUnit, middleUnit)));

        var rootUnit = new OrganisationUnit();
        rootUnit.setId(3);
        when(organisationUnitsRelationRepository.getSuperOU(2)).thenReturn(Optional.of(
            new OrganisationUnitsRelation(new HashSet<>(), new HashSet<>(),
                OrganisationUnitRelationType.BELONGS_TO, null, null, ApproveStatus.APPROVED,
                new HashSet<>(), middleUnit, rootUnit)));

        // When
        var result = organisationUnitService.getOrganisationUnitsRelationsChain(leafUnit.getId());

        // Then
        assertEquals(3, result.getNodes().size());
        verify(organisationUnitsRelationRepository, times(3)).getSuperOU(anyInt());
    }

    @Test
    public void shouldGetOrganisationUnitIdsFromSubHierarchy() {
        // given
        var unit1 = new OrganisationUnit();
        unit1.setId(1);

        when(organisationUnitsRelationRepository.getSubOUsRecursive(1)).thenReturn(
            new ArrayList<>(List.of(2, 3)));

        // when
        var result = organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(unit1.getId());

        // then
        assertEquals(List.of(2, 3, 1), result);
    }

    @Test
    public void shouldFindOUByImportId() {
        // Given
        var ou = new OrganisationUnitIndex();
        ou.setScopusAfid("12345");

        when(organisationUnitIndexRepository.findByScopusAfidOrOpenAlexId(
            "12345")).thenReturn(Optional.of(ou));

        // When
        var foundOu = organisationUnitService.findOrganisationUnitByImportId("12345");

        // Then
        assertEquals(ou, foundOu);
    }

    @Test
    public void shouldNotFindOUByImportIdWhenOUDoesNotExist() {
        // Given
        when(organisationUnitIndexRepository.findByScopusAfidOrOpenAlexId(
            "12345")).thenReturn(Optional.empty());

        // When
        var foundOu = organisationUnitService.findOrganisationUnitByImportId("12345");

        // Then
        assertNull(foundOu);
    }

    @Test
    public void testGetOUSubUnits() {
        // Given
        var organisationUnitId = 1;
        var ouIndex1 = new OrganisationUnitIndex();
        var ouIndex2 = new OrganisationUnitIndex();
        var expectedSubUnits = new PageImpl<>(List.of(ouIndex1, ouIndex2));
        var pageRequest = PageRequest.of(0, 10);

        when(organisationUnitIndexRepository.findOrganisationUnitIndexesBySuperOUId(
            organisationUnitId, pageRequest))
            .thenReturn(expectedSubUnits);

        // When
        var actualSubUnits = organisationUnitService.getOUSubUnits(organisationUnitId, pageRequest);

        // Then
        assertEquals(expectedSubUnits, actualSubUnits);
        verify(organisationUnitIndexRepository, times(1))
            .findOrganisationUnitIndexesBySuperOUId(organisationUnitId, pageRequest);
    }

    @Test
    void shouldForceDeleteOrganisationUnit() {
        // Given
        var organisationUnitId = 1;

        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(organisationUnitId))
            .thenReturn(Optional.of(new OrganisationUnit()));
        when(organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
            organisationUnitId))
            .thenReturn(Optional.of(new OrganisationUnitIndex()));
        when(organisationUnitRepository.fetchAllThesesForOU(any(), any())).thenReturn(Page.empty());

        // When
        organisationUnitService.forceDeleteOrganisationUnit(organisationUnitId);

        // Then
        verify(organisationUnitRepository, times(1))
            .deleteInvolvementsForOrganisationUnit(organisationUnitId);
        verify(organisationUnitRepository, times(1))
            .deleteRelationsForOrganisationUnit(organisationUnitId);
    }

    @Test
    void shouldReturnTrueIfInstitutionalAdminsExist() {
        // Given
        Integer organisationUnitId = 1;
        when(organisationUnitRepository.checkIfInstitutionalAdminsExist(
            organisationUnitId)).thenReturn(true);

        // When
        boolean result =
            organisationUnitService.checkIfInstitutionalAdminsExist(organisationUnitId);

        // Then
        assertTrue(result, "Expected institutional admins to exist");
        verify(organisationUnitRepository, times(1)).checkIfInstitutionalAdminsExist(
            organisationUnitId);
    }

    @Test
    void shouldReturnFalseIfInstitutionalAdminsDoNotExist() {
        // Given
        Integer organisationUnitId = 1;
        when(organisationUnitRepository.checkIfInstitutionalAdminsExist(
            organisationUnitId)).thenReturn(false);

        // When
        boolean result =
            organisationUnitService.checkIfInstitutionalAdminsExist(organisationUnitId);

        // Then
        assertFalse(result, "Expected institutional admins to not exist");
        verify(organisationUnitRepository, times(1)).checkIfInstitutionalAdminsExist(
            organisationUnitId);
    }

    @Test
    void shouldReturnFalseWhenScopusAfidDoesNotExist() {
        // given
        var identifier = "123456";
        var organisationUnitId = 1;
        when(organisationUnitRepository.existsByScopusAfid(identifier,
            organisationUnitId)).thenReturn(false);

        // when
        var result = organisationUnitService.isIdentifierInUse(identifier, organisationUnitId);

        // then
        assertFalse(result);
        verify(organisationUnitRepository).existsByScopusAfid(identifier, organisationUnitId);
    }

    @Test
    void shouldReturnTrueWhenScopusAfidExists() {
        // given
        var identifier = "123456";
        var organisationUnitId = 1;
        when(organisationUnitRepository.existsByScopusAfid(identifier,
            organisationUnitId)).thenReturn(true);

        // when
        var result = organisationUnitService.isIdentifierInUse(identifier, organisationUnitId);

        // then
        assertTrue(result);
        verify(organisationUnitRepository).existsByScopusAfid(identifier, organisationUnitId);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnSearchFields(Boolean onlyExportFields) {
        // Given
        var expectedFields = List.of(
            new Triple<>("field1", List.of(new MultilingualContentDTO()), "Type1"),
            new Triple<>("field2", List.of(new MultilingualContentDTO()), "Type2")
        );

        when(searchFieldsLoader.getSearchFields(any(), anyBoolean())).thenReturn(expectedFields);

        // When
        var result = organisationUnitService.getSearchFields(onlyExportFields);

        // Then
        assertNotNull(result);
        assertEquals(expectedFields.size(), result.size());
    }

    @Test
    void shouldReturnOUByAccountingId() {
        // Given
        var accountingId = "ACC123";
        var expectedOU = new OrganisationUnit();
        expectedOU.setAccountingIds(Set.of(accountingId));

        when(organisationUnitRepository.findApprovedOrganisationUnitByAccountingId(accountingId))
            .thenReturn(Optional.of(expectedOU));

        // When
        var actualOU =
            organisationUnitService.findOrganisationUnitByAccountingId(accountingId);

        // Then
        assertEquals(expectedOU, actualOU);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenAccountingIdIsNotFound() {
        // Given
        var accountingId = "MISSING_ID";
        when(organisationUnitRepository.findApprovedOrganisationUnitByAccountingId(accountingId))
            .thenReturn(Optional.empty());

        // When / Then
        var ex = assertThrows(NotFoundException.class, () ->
            organisationUnitService.findOrganisationUnitByAccountingId(accountingId)
        );
        assertEquals("Organisation unit with accounting ID MISSING_ID does not exist",
            ex.getMessage());
    }

    @Test
    void shouldThrowExceptionForInvalidMimeType() {
        // given
        var organisationUnitId = 1;
        var mockFile = createMockMultipartFile();

        try (MockedStatic<ImageUtil> mockedStatic = mockStatic(ImageUtil.class)) {
            mockedStatic.when(() -> ImageUtil.isMIMETypeInvalid(mockFile, true)).thenReturn(true);

            // when / then
            assertThrows(IllegalArgumentException.class, () ->
                organisationUnitService.setOrganisationUnitLogo(
                    organisationUnitId,
                    new ProfilePhotoOrLogoDTO(1, 2, 3, 4, "#ffffff", mockFile)
                )
            );

            verifyNoInteractions(fileService);
        }
    }

    @Test
    void shouldDeleteOldFileWhenReplacingLogo() throws IOException {
        // given
        var organisationUnitId = 1;
        var mockFile = createMockMultipartFile();
        var existingLogo = new ProfilePhotoOrLogo();
        existingLogo.setImageServerName("oldFile.jpg");

        var organisationUnit = new OrganisationUnit();
        organisationUnit.setLogo(existingLogo);

        try (MockedStatic<ImageUtil> mockedStatic = mockStatic(ImageUtil.class)) {
            mockedStatic.when(
                () -> ImageUtil.isMIMETypeInvalid(mockFile, true)).thenReturn(false);

            when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(
                organisationUnitId)).thenReturn(
                Optional.of(organisationUnit));
            when(fileService.store(any(), anyString())).thenReturn("newFile.jpg");

            // when
            organisationUnitService.setOrganisationUnitLogo(organisationUnitId,
                new ProfilePhotoOrLogoDTO(1, 2, 3, 4, "#ffffff", mockFile));

            // then
            verify(fileService).delete("oldFile.jpg");
            verify(fileService).store(eq(mockFile), anyString());
            verify(organisationUnitRepository).save(organisationUnit);
            assertEquals("newFile.jpg", organisationUnit.getLogo().getImageServerName());
        }
    }

    @Test
    void shouldSetLogoWhenNoneExists() throws IOException {
        // given
        var organisationUnitId = 1;
        var mockFile = createMockMultipartFile();

        var organisationUnit = new OrganisationUnit();
        organisationUnit.setLogo(null);

        try (MockedStatic<ImageUtil> mockedStatic = mockStatic(ImageUtil.class)) {
            mockedStatic.when(() -> ImageUtil.isMIMETypeInvalid(mockFile, true)).thenReturn(false);

            when(organisationUnitRepository
                .findByIdWithLangDataAndResearchArea(organisationUnitId)).thenReturn(
                Optional.of(organisationUnit));
            when(fileService.store(any(), anyString())).thenReturn("file123.jpg");

            // when
            organisationUnitService.setOrganisationUnitLogo(organisationUnitId,
                new ProfilePhotoOrLogoDTO(10, 20, 30, 40, "#123456", mockFile));

            // then
            verify(fileService).store(eq(mockFile), anyString());
            verify(organisationUnitRepository).save(organisationUnit);

            var logo = organisationUnit.getLogo();
            assertNotNull(logo);
            assertEquals(20, logo.getTopOffset());
            assertEquals(10, logo.getLeftOffset());
            assertEquals(40, logo.getHeight());
            assertEquals(30, logo.getWidth());
            assertEquals("#123456", logo.getBackgroundHex());
            assertEquals("file123.jpg", logo.getImageServerName());
        }
    }

    @Test
    void shouldRemoveLogoAndResetFields() {
        // given
        var organisationUnitId = 1;
        var logo = new ProfilePhotoOrLogo();
        logo.setImageServerName("logo.jpg");
        logo.setTopOffset(1);
        logo.setLeftOffset(2);
        logo.setHeight(3);
        logo.setWidth(4);
        logo.setBackgroundHex("#fff");

        var organisationUnit = new OrganisationUnit();
        organisationUnit.setLogo(logo);

        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(
            organisationUnitId)).thenReturn(Optional.of(organisationUnit));

        // when
        organisationUnitService.removeOrganisationUnitLogo(organisationUnitId);

        // then
        verify(fileService).delete("logo.jpg");
        verify(organisationUnitRepository).save(organisationUnit);

        assertNull(logo.getImageServerName());
        assertNull(logo.getTopOffset());
        assertNull(logo.getLeftOffset());
        assertNull(logo.getHeight());
        assertNull(logo.getWidth());
        assertNull(logo.getBackgroundHex());
    }

    @Test
    void shouldAddSubUnit() {
        // Given
        var sourceId = 1;
        var targetId = 2;
        when(organisationUnitsRelationRepository.getSuperOU(targetId))
            .thenReturn(Optional.empty());
        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(sourceId)).thenReturn(
            Optional.of(new OrganisationUnit() {{
                setId(1);
            }}));
        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(targetId)).thenReturn(
            Optional.of(new OrganisationUnit() {{
                setId(2);
            }}));

        OrganisationUnit sourceOU = new OrganisationUnit();
        sourceOU.setId(targetId);
        OrganisationUnit targetOU = new OrganisationUnit();
        targetOU.setId(sourceId);

        OrganisationUnitsRelation savedRelation = new OrganisationUnitsRelation();
        savedRelation.setId(100);
        savedRelation.setSourceOrganisationUnit(sourceOU);
        savedRelation.setTargetOrganisationUnit(targetOU);

        when(organisationUnitsRelationRepository.save(any())).thenReturn(savedRelation);
        OrganisationUnitIndex mockIndex = new OrganisationUnitIndex();
        when(organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(targetId))
            .thenReturn(Optional.of(mockIndex));

        // When
        OrganisationUnitsRelationDTO result =
            organisationUnitService.addSubOrganisationUnit(sourceId, targetId);

        // Then
        assertEquals(100, result.getId());
        assertEquals(sourceId, result.getTargetOrganisationUnitId());
        assertEquals(targetId, result.getSourceOrganisationUnitId());
        assertEquals(OrganisationUnitRelationType.BELONGS_TO, result.getRelationType());

        verify(organisationUnitsRelationRepository).save(any());
        verify(organisationUnitIndexRepository).save(mockIndex);
    }

    @Test
    void givenExistingSuperRelation_whenAddSubOrganisationUnit_thenThrowsException() {
        // Given
        var sourceId = 1;
        var targetId = 2;
        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(sourceId)).thenReturn(
            Optional.of(new OrganisationUnit() {{
                setId(1);
            }}));
        when(organisationUnitRepository.findByIdWithLangDataAndResearchArea(targetId)).thenReturn(
            Optional.of(new OrganisationUnit() {{
                setId(2);
            }}));
        when(organisationUnitsRelationRepository.getSuperOU(targetId))
            .thenReturn(Optional.of(new OrganisationUnitsRelation()));

        // When / Then
        assertThrows(OrganisationUnitReferenceConstraintViolationException.class, () ->
            organisationUnitService.addSubOrganisationUnit(sourceId, targetId)
        );

        verify(organisationUnitsRelationRepository, never()).save(any());
    }

    @Test
    void shouldReturnRawOrganisationUnit() {
        // Given
        var entityId = 123;
        var expected = new OrganisationUnit();
        expected.setId(entityId);
        when(organisationUnitRepository.findRaw(entityId)).thenReturn(Optional.of(expected));

        // When
        var actual = organisationUnitService.findRaw(entityId);

        // Then
        assertEquals(expected, actual);
        verify(organisationUnitRepository).findRaw(entityId);
    }

    @Test
    void shouldThrowsNotFoundExceptionWhenOrganisationUnitDoesNotExist() {
        // Given
        var entityId = 123;
        when(organisationUnitRepository.findRaw(entityId)).thenReturn(Optional.empty());

        // When & Then
        var exception = assertThrows(NotFoundException.class,
            () -> organisationUnitService.findRaw(entityId));

        assertEquals("Organisation Unit with given ID does not exist.", exception.getMessage());
        verify(organisationUnitRepository).findRaw(entityId);
    }

    @Test
    void shouldDoNothingWhenNoRelationsFound() {
        // Given
        var sourceId = 1;
        var targetId = 2;
        when(organisationUnitsRelationRepository
            .findBySourceOrganisationUnitIdAndTargetOrganisationUnitIdAndRelationType(
                sourceId, targetId, OrganisationUnitRelationType.BELONGS_TO))
            .thenReturn(Collections.emptyList());

        // When
        organisationUnitService.deleteOrganisationUnitsRelation(sourceId, targetId);

        // Then
        verify(organisationUnitsRelationRepository, never()).delete(any());
        verifyNoInteractions(organisationUnitIndexRepository);
    }

    @Test
    void shouldReindexAndDeleteRelationWhenRelationExists() {
        // Given
        var sourceId = 1;
        var targetId = 2;

        var subOU = new OrganisationUnit();
        subOU.setId(sourceId);

        var relation = new OrganisationUnitsRelation();
        relation.setSourceOrganisationUnit(subOU);

        when(organisationUnitsRelationRepository
            .findBySourceOrganisationUnitIdAndTargetOrganisationUnitIdAndRelationType(
                sourceId, targetId, OrganisationUnitRelationType.BELONGS_TO))
            .thenReturn(List.of(relation));

        var index = new OrganisationUnitIndex();
        when(organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(sourceId))
            .thenReturn(Optional.of(index));

        var spyService = Mockito.spy(organisationUnitService);
        doReturn(List.of(sourceId)).when(spyService)
            .getOrganisationUnitIdsFromSubHierarchy(sourceId);
        doReturn(subOU).when(spyService).findOne(sourceId);
        doReturn(subOU).when(spyService).save(subOU);

        // When
        spyService.deleteOrganisationUnitsRelation(sourceId, targetId);

        // Then
        assertNull(index.getSuperOUId());
        assertNull(index.getSuperOUNameSr());
        assertNull(index.getSuperOUNameOther());
        verify(organisationUnitIndexRepository).save(index);

        assertFalse(subOU.getIsClientInstitution());
        assertFalse(subOU.getValidateEmailDomain());
        assertFalse(subOU.getAllowSubdomains());
        verify(spyService).save(subOU);

        verify(organisationUnitsRelationRepository).delete(relation);
    }

    @Test
    void shouldSkipIndexUpdateWhenIndexNotFound() {
        // Given
        var sourceId = 1;
        var targetId = 2;

        var subOU = new OrganisationUnit();
        subOU.setId(sourceId);

        var relation = new OrganisationUnitsRelation();
        relation.setSourceOrganisationUnit(subOU);

        when(organisationUnitsRelationRepository
            .findBySourceOrganisationUnitIdAndTargetOrganisationUnitIdAndRelationType(
                sourceId, targetId, OrganisationUnitRelationType.BELONGS_TO))
            .thenReturn(List.of(relation));

        when(organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(sourceId))
            .thenReturn(Optional.empty());

        var spyService = Mockito.spy(organisationUnitService);
        doReturn(List.of(sourceId)).when(spyService)
            .getOrganisationUnitIdsFromSubHierarchy(sourceId);
        doReturn(subOU).when(spyService).findOne(sourceId);
        doReturn(subOU).when(spyService).save(subOU);

        // When
        spyService.deleteOrganisationUnitsRelation(sourceId, targetId);

        // Then
        verify(organisationUnitIndexRepository, never()).save(any());
        verify(spyService).save(subOU);
        verify(organisationUnitsRelationRepository).delete(relation);
    }

    private MultipartFile createMockMultipartFile() {
        return new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }
}
