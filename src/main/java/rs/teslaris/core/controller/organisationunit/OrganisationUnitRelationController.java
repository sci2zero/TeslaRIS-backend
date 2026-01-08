package rs.teslaris.core.controller.organisationunit;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.OrgUnitEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationResponseDTO;
import rs.teslaris.core.dto.institution.RelationGraphDataDTO;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/organisation-unit-relation")
@RequiredArgsConstructor
@Traceable
public class OrganisationUnitRelationController {

    private final OrganisationUnitService organisationUnitService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping("/get-all/{sourceId}")
    public List<OrganisationUnitsRelationResponseDTO> getOrganisationUnitsRelations(
        @PathVariable Integer sourceId) {
        return organisationUnitService.getOrganisationUnitsRelations(sourceId);
    }

    @GetMapping("/{leafId}")
    public RelationGraphDataDTO getOrganisationUnitsRelationsChain(
        @PathVariable Integer leafId) {
        return organisationUnitService.getOrganisationUnitsRelationsChain(leafId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_OU_RELATIONS')")
    public OrganisationUnitsRelationDTO createOrganisationUnitsRelations(
        @RequestBody @Valid OrganisationUnitsRelationDTO relationDTO) {
        var newRelation = organisationUnitService.createOrganisationUnitsRelation(relationDTO);
        relationDTO.setId(newRelation.getId());

        return relationDTO;
    }

    @PostMapping("/{organisationUnitId}/{targetOrganisationUnitId}")
    @PreAuthorize("hasAnyAuthority('EDIT_OU_RELATIONS', 'ADD_SUB_UNIT')")
    @OrgUnitEditCheck
    @Idempotent
    @ResponseStatus(HttpStatus.CREATED)
    public OrganisationUnitsRelationDTO addSubOrganisationUnit(
        @PathVariable Integer organisationUnitId, @PathVariable Integer targetOrganisationUnitId) {
        return organisationUnitService.addSubOrganisationUnit(organisationUnitId,
            targetOrganisationUnitId);
    }

    @PutMapping("/{relationId}")
    @PreAuthorize("hasAnyAuthority('EDIT_OU_RELATIONS', 'ADD_SUB_UNIT')")
    @OrgUnitEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateOrganisationUnitsRelations(
        @RequestBody @Valid OrganisationUnitsRelationDTO relationDTO,
        @PathVariable Integer relationId) {
        organisationUnitService.editOrganisationUnitsRelation(relationDTO, relationId);
    }

    @DeleteMapping("/{relationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_OU_RELATIONS')")
    public void deleteOrganisationUnitsRelation(@PathVariable Integer relationId) {
        organisationUnitService.deleteOrganisationUnitsRelation(relationId);
    }

    @DeleteMapping("/delete/{sourceId}/{targetId}")
    @PreAuthorize("hasAnyAuthority('EDIT_OU_RELATIONS', 'ADD_SUB_UNIT')")
    @OrgUnitEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrganisationUnitsRelation(@PathVariable Integer sourceId,
                                                @PathVariable Integer targetId) {
        organisationUnitService.deleteOrganisationUnitsRelation(sourceId, targetId);
    }

    @PatchMapping("/{relationId}/{approve}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_OU_RELATIONS')")
    public void setOrganisationUnitsRelationApproveStatus(@PathVariable Integer relationId,
                                                          @PathVariable Boolean approve) {
        organisationUnitService.approveRelation(relationId, approve);
    }

    @PatchMapping(value = "/{relationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_OU_RELATIONS')")
    @Idempotent
    public void addRelationProofs(@ModelAttribute @Valid DocumentFileDTO proof,
                                  @PathVariable Integer relationId) {
        organisationUnitService.addRelationProofs(List.of(proof), relationId);
    }

    @DeleteMapping("/{relationId}/{proofId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_OU_RELATIONS')")
    public void deleteRelationProof(@PathVariable Integer relationId,
                                    @PathVariable Integer proofId) {
        organisationUnitService.deleteRelationProof(relationId, proofId);
    }
}
