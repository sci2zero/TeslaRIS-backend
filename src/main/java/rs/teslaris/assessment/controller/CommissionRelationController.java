package rs.teslaris.assessment.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.dto.CommissionRelationDTO;
import rs.teslaris.assessment.dto.CommissionRelationResponseDTO;
import rs.teslaris.assessment.dto.ReorderCommissionRelationDTO;
import rs.teslaris.assessment.service.interfaces.CommissionRelationService;
import rs.teslaris.core.annotation.CommissionEditCheck;
import rs.teslaris.core.annotation.Idempotent;

@RestController
@RequestMapping("/api/assessment/commission-relation")
@RequiredArgsConstructor
public class CommissionRelationController {

    private final CommissionRelationService commissionRelationService;

    @GetMapping("/{commissionId}")
    @PreAuthorize("hasAuthority('UPDATE_COMMISSION')")
    public List<CommissionRelationResponseDTO> fetchCommissionRelations(
        @PathVariable("commissionId") Integer sourceCommissionId) {
        return commissionRelationService.fetchCommissionRelations(sourceCommissionId);
    }

    @PatchMapping("/{commissionId}")
    @PreAuthorize("hasAuthority('UPDATE_COMMISSION')")
    @CommissionEditCheck
    @Idempotent
    @ResponseStatus(HttpStatus.CREATED)
    public void addCommissionRelation(@RequestBody CommissionRelationDTO commissionRelationDTO) {
        commissionRelationService.addCommissionRelation(commissionRelationDTO);
    }

    @PutMapping("/{commissionId}/{commissionRelationId}")
    @PreAuthorize("hasAuthority('UPDATE_COMMISSION')")
    @CommissionEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCommissionRelation(@PathVariable Integer commissionRelationId,
                                         @RequestBody CommissionRelationDTO commissionRelationDTO) {
        commissionRelationService.updateCommissionRelation(commissionRelationId,
            commissionRelationDTO);
    }

    @DeleteMapping("/{commissionId}/{commissionRelationId}")
    @PreAuthorize("hasAuthority('UPDATE_COMMISSION')")
    @CommissionEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommissionRelation(@PathVariable Integer commissionRelationId) {
        commissionRelationService.deleteCommissionRelation(commissionRelationId);
    }

    @PatchMapping("/{commissionId}/{commissionRelationId}")
    @PreAuthorize("hasAuthority('UPDATE_COMMISSION')")
    @CommissionEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Idempotent
    public void reorderCommissionRelations(@PathVariable Integer commissionId,
                                           @PathVariable Integer commissionRelationId,
                                           @RequestBody ReorderCommissionRelationDTO reorderDTO) {
        commissionRelationService.reorderCommissionRelations(commissionId, commissionRelationId,
            reorderDTO);
    }
}
