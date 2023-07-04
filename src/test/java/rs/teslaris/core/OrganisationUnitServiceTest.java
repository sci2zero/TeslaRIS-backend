package rs.teslaris.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import rs.teslaris.core.dto.institution.OrganisationUnitDTORequest;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.exception.SelfRelationException;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.GeoLocation;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.repository.person.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.OrganisationUnitsRelationRepository;
import rs.teslaris.core.service.DocumentFileService;
import rs.teslaris.core.service.MultilingualContentService;
import rs.teslaris.core.service.ResearchAreaService;
import rs.teslaris.core.service.impl.OrganisationUnitServiceImpl;

@SpringBootTest
public class OrganisationUnitServiceTest {

    @Mock
    private OrganisationUnitRepository organisationUnitRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    @Mock
    private DocumentFileService documentFileService;

    @Mock
    private ResearchAreaService researchAreaService;
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

        when(organisationUnitRepository.findByIdWithLangDataAndResearchAreaAndDeletedIsFalse(id))
            .thenReturn(Optional.of(organisationUnit));

        // when
        OrganisationUnit result = organisationUnitService.findOrganisationUnitById(id);

        // then
        assertEquals(organisationUnit, result);
        verify(organisationUnitRepository, times(1)).findByIdWithLangDataAndResearchAreaAndDeletedIsFalse(id);

    }

    @Test
    public void shouldThrowNotFoundExceptionWhenOrganisationUnitDoesNotExist() {
        // given
        Integer id = 1;

        // when
        when(organisationUnitRepository.findByIdWithLangDataAndResearchAreaAndDeletedIsFalse(id))
            .thenReturn(Optional.empty());

        // then (NotFoundException should be thrown)
        assertThrows(NotFoundException.class,
            () -> organisationUnitService.findOrganisationUnitById(id));
        verify(organisationUnitRepository, times(1)).findByIdWithLangDataAndResearchAreaAndDeletedIsFalse(id);

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
        when(organisationUnitRepository.findAllWithLangDataAndDeletedIsFalse(pageable)).thenReturn(
            organisationUnitPage);

        Page<OrganisationUnitDTO> result = organisationUnitService.findOrganisationUnits(pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(organisationUnit.getNameAbbreviation(),
            result.getContent().get(0).getNameAbbreviation());
        verify(organisationUnitRepository, times(1)).findAllWithLangDataAndDeletedIsFalse(pageable);
    }

    @Test
    public void shouldReturnOrganisationUnitsRelationWhenItExists() {
        // given
        var expected = new OrganisationUnitsRelation();
        when(organisationUnitsRelationRepository.findByIdAndDeletedIsFalse(1)).thenReturn(Optional.of(expected));

        // when
        var result = organisationUnitService.findOrganisationUnitsRelationById(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenOrganisationUnitsRelationDoesNotExist() {
        // given
        when(organisationUnitsRelationRepository.findByIdAndDeletedIsFalse(1)).thenReturn(Optional.empty());

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

        var relationToDelete = Mockito.mock(OrganisationUnitsRelation.class);
        when(organisationUnitsRelationRepository.getReferenceById(relationId)).thenReturn(
            relationToDelete);

        // when
        organisationUnitService.deleteOrganisationUnitsRelation(relationId);

        // then
        verify(organisationUnitsRelationRepository).delete(relationToDelete);
    }

    @Test
    public void shouldApproveRelation() {
        // given
        var relationId = 1;
        var approve = true;

        var relationToApprove = Mockito.mock(OrganisationUnitsRelation.class);
        when(organisationUnitsRelationRepository.findByIdAndDeletedIsFalse(relationId)).thenReturn(
            Optional.of(relationToApprove));

        // when
        organisationUnitService.approveRelation(relationId, approve);

        // then
        verify(relationToApprove).setApproveStatus(ApproveStatus.APPROVED);
        verify(organisationUnitsRelationRepository).save(relationToApprove);
    }

    @Test
    public void shouldCreateOrganisationUnitsRelation() {
        // given
        var relationDTO = new OrganisationUnitsRelationDTO();
        relationDTO.setSourceOrganisationUnitId(1);
        relationDTO.setTargetOrganisationUnitId(2);

        var newRelation = Mockito.mock(OrganisationUnitsRelation.class);
        when(organisationUnitsRelationRepository.save(
            any(OrganisationUnitsRelation.class))).thenReturn(newRelation);
        when(organisationUnitRepository.findByIdWithLangDataAndResearchAreaAndDeletedIsFalse(any())).thenReturn(
            Optional.of(new OrganisationUnit()));

        // when
        var result = organisationUnitService.createOrganisationUnitsRelation(relationDTO);

        // then
        assertNotNull(result);
        assertEquals(newRelation, result);
        verify(organisationUnitsRelationRepository).save(any(OrganisationUnitsRelation.class));
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

        when(organisationUnitsRelationRepository.findByIdAndDeletedIsFalse(relationId)).thenReturn(
            Optional.of(relation));
        when(organisationUnitRepository.findByIdWithLangDataAndResearchAreaAndDeletedIsFalse(any())).thenReturn(
            Optional.of(new OrganisationUnit()));

        // when
        organisationUnitService.editOrganisationUnitsRelation(relationDTO, relationId);

        // then
        verify(organisationUnitsRelationRepository).save(relation);
    }

    @Test
    public void shouldAddInvolvementProofWhenInvolvementExists() {
        // given
        var relation = new OrganisationUnitsRelation();
        relation.setProofs(new HashSet<>());

        when(organisationUnitsRelationRepository.findByIdAndDeletedIsFalse(1)).thenReturn(Optional.of(relation));
        when(documentFileService.saveNewDocument(any(), eq(true))).thenReturn(new DocumentFile());

        // when
        organisationUnitService.addRelationProofs(
            List.of(new DocumentFileDTO(), new DocumentFileDTO()), 1);

        //then
        verify(organisationUnitsRelationRepository, times(2)).save(relation);
    }

    @Test
    public void shouldDeleteProofWhenDocumentExists() {
        // given
        var df = new DocumentFile();
        df.setServerFilename("UUID");
        var relation = new OrganisationUnitsRelation();
        relation.setProofs(new HashSet<>(Set.of(df)));

        when(organisationUnitsRelationRepository.findByIdAndDeletedIsFalse(1)).thenReturn(Optional.of(relation));
        when(documentFileService.findDocumentFileById(1)).thenReturn(df);

        // when
        organisationUnitService.deleteRelationProof(1, 1);

        //then
//        verify(organisationUnitsRelationRepository, times(1)).save(relation);
//        verify(documentFileService, times(1)).deleteDocumentFile(df.getServerFilename());
    }

    @Test
    void shouldCreateOrganisationUnits() {
        OrganisationUnitDTORequest organisationUnitDTORequest = new OrganisationUnitDTORequest();
        // Set properties for organisationUnitDTORequest

        MultiLingualContent name = new MultiLingualContent();
        name.setContent("A1");

        MultiLingualContent keyword = new MultiLingualContent();
        keyword.setContent("B1");

        ResearchArea researchArea = new ResearchArea();
        researchArea.setId(1);
        List<ResearchArea> researchAreas = List.of(new ResearchArea());


        GeoLocation location = new GeoLocation(1.0, 2.0, 3);
        Contact contact = new Contact("a", "b");

        organisationUnitDTORequest.setName(List.of(new MultilingualContentDTO()));
        organisationUnitDTORequest.setKeyword(List.of(new MultilingualContentDTO()));
        organisationUnitDTORequest.setResearchAreasId(List.of(1));
        organisationUnitDTORequest.setLocation(new GeoLocationDTO(1.0, 2.0, 3));
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

        OrganisationUnit result =
            organisationUnitService.createOrganisationalUnit(organisationUnitDTORequest);

        assertEquals(Set.of(name), result.getName());
        assertEquals(organisationUnitDTORequest.getNameAbbreviation(),
            result.getNameAbbreviation());
        assertEquals(Set.of(keyword), result.getKeyword());
        assertEquals(new HashSet<>(researchAreas), result.getResearchAreas());
        assertEquals(location.getLatitude(), result.getLocation().getLatitude());
        assertEquals(ApproveStatus.APPROVED, result.getApproveStatus());
        assertEquals(contact, result.getContact());
        assertEquals(1, result.getId());

        verify(multilingualContentService, times(1)).getMultilingualContent(
            organisationUnitDTORequest.getName());
        verify(multilingualContentService, times(1)).getMultilingualContent(
            organisationUnitDTORequest.getKeyword());
        verify(researchAreaService, times(1)).getResearchAreasByIds(
            organisationUnitDTORequest.getResearchAreasId());
        verify(organisationUnitRepository, times(1)).save(any(OrganisationUnit.class));
    }


    @Test
    void shouldEditOrganisationUnits() {
        OrganisationUnitDTORequest organisationUnitDTORequest = new OrganisationUnitDTORequest();
        organisationUnitDTORequest.setName(List.of(new MultilingualContentDTO()));
        organisationUnitDTORequest.setKeyword(List.of(new MultilingualContentDTO()));
        organisationUnitDTORequest.setResearchAreasId(List.of(1));
        organisationUnitDTORequest.setLocation(new GeoLocationDTO(10.0, 20.0, 30));
        organisationUnitDTORequest.setContact(new ContactDTO("b", "b"));

        OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setName(
            Stream.of(new MultiLingualContent()).collect(Collectors.toSet()));
        organisationUnit.setKeyword(
            Stream.of(new MultiLingualContent()).collect(Collectors.toSet()));
        organisationUnit.setResearchAreas(
            Stream.of(new ResearchArea()).collect(Collectors.toSet()));
        organisationUnit.setLocation(new GeoLocation(1.0, 2.0, 3));
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
            organisationUnitService.editOrganisationalUnit(organisationUnitDTORequest,
                organisationUnitId);

        // Assert
        assertNotNull(editedOrganisationUnit);
        assertEquals(organisationUnit, editedOrganisationUnit);
        assertEquals(organisationUnitDTORequest.getName().stream().findFirst().get().getContent(),
            editedOrganisationUnit.getName().stream().findFirst().get().getContent());
        assertEquals(
            organisationUnitDTORequest.getKeyword().stream().findFirst().get().getContent(),
            editedOrganisationUnit.getKeyword().stream().findFirst().get().getContent());
        verify(multilingualContentService, times(2)).getMultilingualContent(any());
        verify(researchAreaService, times(1)).getResearchAreasByIds(any());
        verify(organisationUnitRepository, times(1)).save(any(OrganisationUnit.class));
    }

    @Test
    public void shouldDeleteOrganisationalUnit() {
        Integer organisationUnitId = 1;
        OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setId(organisationUnitId);
        when(organisationUnitRepository.findByIdWithLangDataAndResearchAreaAndDeletedIsFalse(organisationUnitId)).thenReturn(
            Optional.of(organisationUnit));

        organisationUnitService.delete(organisationUnitId);

        verify(organisationUnitRepository, times(1)).save(organisationUnit);
    }

    @Test
    public void shouldIgnoreNullOrganisationalUnit() {
        Integer organisationUnitId = 1;
        var ou = new OrganisationUnit();
        when(organisationUnitRepository.findByIdWithLangDataAndResearchAreaAndDeletedIsFalse(organisationUnitId)).thenReturn(Optional.of(ou));

        organisationUnitService.delete(organisationUnitId);

        verify(organisationUnitRepository, times(1)).save(ou);

    }

    @Test
    public void shouldEditOrganisationalUnitApproveStatus() {
        Integer organisationUnitId = 1;
        ApproveStatus approveStatus = ApproveStatus.APPROVED;
        OrganisationUnit organisationUnit = new OrganisationUnit();
        organisationUnit.setId(organisationUnitId);
        when(organisationUnitRepository.findByIdWithLangDataAndResearchAreaAndDeletedIsFalse(
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
        when(organisationUnitRepository.findByIdWithLangDataAndResearchAreaAndDeletedIsFalse(
            organisationUnitId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            organisationUnitService.editOrganisationalUnitApproveStatus(approveStatus,
                organisationUnitId);
        });
    }
}
