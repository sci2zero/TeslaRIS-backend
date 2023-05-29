package rs.teslaris.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.exception.SelfRelationException;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.institution.OrganisationUnitsRelation;
import rs.teslaris.core.repository.person.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.OrganisationUnitsRelationRepository;
import rs.teslaris.core.service.DocumentFileService;
import rs.teslaris.core.service.MultilingualContentService;
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

    @InjectMocks
    private OrganisationUnitServiceImpl organisationUnitService;


    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(organisationUnitService, "approvedByDefault", true);
    }

    @Test
    public void shouldReturnOrganisationUnitWhenItExists() {
        // given
        var expected = new OrganisationUnit();
        when(organisationUnitRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var result = organisationUnitService.findOrganisationUnitById(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenOrganisationUnitDoesNotExist() {
        // given
        when(organisationUnitRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> organisationUnitService.findOrganisationUnitById(1));

        // then (NotFoundException should be thrown)
    }


    @Test
    public void shouldReturnOrganisationUnitsRelationWhenItExists() {
        // given
        var expected = new OrganisationUnitsRelation();
        when(organisationUnitsRelationRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var result = organisationUnitService.findOrganisationUnitsRelationById(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenOrganisationUnitsRelationDoesNotExist() {
        // given
        when(organisationUnitsRelationRepository.findById(1)).thenReturn(Optional.empty());

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
        when(organisationUnitsRelationRepository.findById(relationId)).thenReturn(
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
        when(organisationUnitRepository.findById(any())).thenReturn(
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

        when(organisationUnitsRelationRepository.findById(relationId)).thenReturn(
            Optional.of(relation));
        when(organisationUnitRepository.findById(any())).thenReturn(
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

        when(organisationUnitsRelationRepository.findById(1)).thenReturn(Optional.of(relation));
        when(documentFileService.saveNewDocument(any())).thenReturn(new DocumentFile());

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

        when(organisationUnitsRelationRepository.findById(1)).thenReturn(Optional.of(relation));
        when(documentFileService.findDocumentFileById(1)).thenReturn(df);

        // when
        organisationUnitService.deleteRelationProof(1, 1);

        //then
        verify(organisationUnitsRelationRepository, times(1)).save(relation);
        verify(documentFileService, times(1)).deleteDocumentFile(df.getServerFilename());
    }
}
