package rs.teslaris.core.controller;

import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitsRelationResponseDTO;
import rs.teslaris.core.service.OrganisationUnitService;

@RestController
@RequestMapping("/api/relation")
@RequiredArgsConstructor
public class OrganisationUnitRelationController {

    private final OrganisationUnitService organisationUnitService;

    @GetMapping("/{sourceId}/{targetId}")
    public Page<OrganisationUnitsRelationResponseDTO> getOrganisationUnitsRelations(
        @PathVariable Integer sourceId, @PathVariable Integer targetId, Pageable pageable) {
        return organisationUnitService.getOrganisationUnitsRelations(sourceId, targetId, pageable);
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

    @PutMapping("/{relationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_OU_RELATIONS')")
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
