package rs.teslaris.thesislibrary.controller;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.thesislibrary.dto.ThesisPublicReviewResponseDTO;
import rs.teslaris.thesislibrary.service.interfaces.ThesisLibraryDissertationReportService;

@RestController
@RequestMapping("/api/thesis-library/dissertation-report")
@RequiredArgsConstructor
public class ThesisLibraryDissertationReportController {

    private final ThesisLibraryDissertationReportService thesisLibraryDissertationReportService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping
    public Page<ThesisPublicReviewResponseDTO> fetchPublicReviewDissertations(
        @RequestParam(value = "institutionId", required = false) Integer institutionId,
        @RequestParam(value = "year", required = false) Integer year,
        @RequestParam(value = "notDefendedOnly", required = false) Boolean notDefendedOnly,
        @RequestParam(value = "forMyInstitution", required = false) Boolean forMyInstitution,
        @RequestHeader(value = "Authorization", required = false) String bearerToken,
        Pageable pageable) {
        Integer userInstitutionId = null;
        if (Objects.nonNull(forMyInstitution) && forMyInstitution && Objects.nonNull(bearerToken)) {
            userInstitutionId = userService.getUserOrganisationUnitId(
                tokenUtil.extractUserIdFromToken(bearerToken));
        }

        return thesisLibraryDissertationReportService.fetchPublicReviewDissertations(institutionId,
            year, notDefendedOnly, userInstitutionId, pageable);
    }
}
