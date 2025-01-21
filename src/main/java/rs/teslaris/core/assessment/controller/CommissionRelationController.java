package rs.teslaris.core.assessment.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.assessment.dto.CommissionRelationDTO;
import rs.teslaris.core.assessment.dto.CommissionRelationResponseDTO;
import rs.teslaris.core.assessment.dto.ReorderCommissionRelationDTO;
import rs.teslaris.core.assessment.service.interfaces.CommissionRelationService;

@RestController
@RequestMapping("/api/assessment/commission-relation")
@RequiredArgsConstructor
public class CommissionRelationController {

    private final CommissionRelationService commissionRelationService;

    @GetMapping("/{sourceCommissionId}")
    @PreAuthorize("hasAuthority('EDIT_COMMISSIONS')")
    public List<CommissionRelationResponseDTO> fetchCommissionRelations(
        @PathVariable Integer sourceCommissionId) {
        return commissionRelationService.fetchCommissionRelations(sourceCommissionId);
    }

    @PatchMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_COMMISSIONS')")
    @Idempotent
    public void addCommissionRelation(@RequestBody CommissionRelationDTO commissionRelationDTO) {
        commissionRelationService.addCommissionRelation(commissionRelationDTO);
    }

    @PatchMapping("/{commissionRelationId}")
    @PreAuthorize("hasAuthority('EDIT_COMMISSIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCommissionRelation(@PathVariable Integer commissionRelationId,
                                         @RequestBody CommissionRelationDTO commissionRelationDTO) {
        commissionRelationService.updateCommissionRelation(commissionRelationId,
            commissionRelationDTO);
    }

    @DeleteMapping("/{commissionRelationId}")
    @PreAuthorize("hasAuthority('EDIT_COMMISSIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommissionRelation(@PathVariable Integer commissionRelationId) {
        commissionRelationService.deleteCommissionRelation(commissionRelationId);
    }

    @PatchMapping("/{commissionId}/{commissionRelationId}")
    @PreAuthorize("hasAuthority('EDIT_COMMISSIONS')")
    @Idempotent
    public void reorderCommissionRelations(@PathVariable Integer commissionId,
                                           @PathVariable Integer commissionRelationId,
                                           @RequestBody ReorderCommissionRelationDTO reorderDTO) {
        commissionRelationService.reorderCommissionRelations(commissionId, commissionRelationId,
            reorderDTO);
    }
}
