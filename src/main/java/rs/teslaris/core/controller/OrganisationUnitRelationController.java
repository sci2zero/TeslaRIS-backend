package rs.teslaris.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
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
    public OrganisationUnitsRelationDTO createOrganisationUnitsRelations(
        @RequestBody OrganisationUnitsRelationDTO relationDTO) {
        var newRelation = organisationUnitService.createOrganisationUnitsRelation(relationDTO);
        relationDTO.setId(newRelation.getId());

        return relationDTO;
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateOrganisationUnitsRelations(
        @RequestBody OrganisationUnitsRelationDTO relationDTO, @PathVariable Integer id) {
        organisationUnitService.editOrganisationUnitsRelation(relationDTO, id);
    }

    @DeleteMapping("/{id}")
    public void deleteOrganisationUnitsRelation(@PathVariable Integer id) {
        organisationUnitService.deleteOrganisationUnitsRelation(id);
    }
}
