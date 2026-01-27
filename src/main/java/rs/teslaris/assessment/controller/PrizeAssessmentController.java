package rs.teslaris.assessment.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.dto.classification.PrizeAssessmentClassificationDTO;
import rs.teslaris.assessment.service.interfaces.classification.PrizeAssessmentClassificationService;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PersonEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/assessment/prize-assessment-classification")
@RequiredArgsConstructor
@Traceable
public class PrizeAssessmentController {

    private final PrizeAssessmentClassificationService prizeAssessmentClassificationService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping("/{personId}/can-classify")
    @PersonEditCheck("ASSESS")
    @PreAuthorize("hasAuthority('ASSESS_PRIZE')")
    public boolean canClassifyPrize() {
        return true;
    }

    @GetMapping("/{prizeId}")
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPrize(
        @PathVariable Integer prizeId) {
        return prizeAssessmentClassificationService.getAssessmentClassificationsForPrize(prizeId);
    }

    @PostMapping
    @Idempotent
    @PreAuthorize("hasAuthority('ASSESS_PRIZE')")
    @ResponseStatus(HttpStatus.CREATED)
    public EntityAssessmentClassificationResponseDTO createPrizeClassification(
        @RequestBody PrizeAssessmentClassificationDTO prizeAssessmentClassificationDTO,
        @RequestHeader("Authorization") String bearerToken) {
        if (tokenUtil.extractUserRoleFromToken(bearerToken).equals(UserRole.COMMISSION.name())) {
            var user = userService.findOne(tokenUtil.extractUserIdFromToken(bearerToken));
            prizeAssessmentClassificationDTO.setCommissionId(user.getCommission().getId());
        }

        return prizeAssessmentClassificationService.createPrizeAssessmentClassification(
            prizeAssessmentClassificationDTO);
    }

    @PutMapping("/{prizeClassificationId}")
    @PreAuthorize("hasAuthority('ASSESS_PRIZE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePrizeClassification(@PathVariable Integer prizeClassificationId,
                                          @RequestBody
                                          PrizeAssessmentClassificationDTO prizeAssessmentClassificationDTO) {
        prizeAssessmentClassificationService.editPrizeAssessmentClassification(
            prizeClassificationId, prizeAssessmentClassificationDTO);
    }
}
