package rs.teslaris.revisioner.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.revisioner.annotation.DataQualityEditCheck;
import rs.teslaris.revisioner.dto.ConstraintSummaryDTO;
import rs.teslaris.revisioner.dto.DataQualityAssessmentDTO;
import rs.teslaris.revisioner.dto.DataQualityIssueDetailsDTO;
import rs.teslaris.revisioner.dto.DataQualityIssuePageDTO;
import rs.teslaris.revisioner.dto.DataQualityProfileDTO;
import rs.teslaris.revisioner.dto.PolicyExplorerDTO;
import rs.teslaris.revisioner.dto.DataQualityProfileSummaryDTO;
import rs.teslaris.revisioner.dto.ProfileRelatedQualityDTO;
import rs.teslaris.revisioner.dto.QualityReportResponseDTO;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;
import rs.teslaris.revisioner.service.interfaces.DataQualityService;

@RestController
@RequestMapping("/api/data-quality")
@RequiredArgsConstructor
public class DataQualityController {

    private final DataQualityService dataQualityService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping("/{entityType}/{entityId}/can-assess")
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    @DataQualityEditCheck
    public boolean canAssessDataQuality() {
        return true;
    }

    @GetMapping(value = "/report/{entityType}/{entityId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    @DataQualityEditCheck
    public List<QualityReportResponseDTO> getQualityReportForEntity(@PathVariable String entityType,
                                                                    @PathVariable
                                                                    Integer entityId) {
        return dataQualityService.getQualityReportForEntity(entityType, entityId);
    }

    @GetMapping(value = "/assessments/{entityType}/{entityId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    @DataQualityEditCheck
    public List<DataQualityAssessmentDTO> findOne(@PathVariable String entityType,
                                                  @PathVariable Integer entityId) {
        return dataQualityService.findLatestAssessmentsForEntity(entityType, entityId);
    }

    @GetMapping(value = "/assessments/{entityType}/{entityId}/{majorVersion}/{minorVersion}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    @DataQualityEditCheck
    public List<DataQualityAssessmentDTO> findForVersion(@PathVariable String entityType,
                                                         @PathVariable Integer entityId,
                                                         @PathVariable Integer majorVersion,
                                                         @PathVariable Integer minorVersion) {
        return dataQualityService.findAssessmentsForEntityVersion(entityType, entityId,
            majorVersion, minorVersion);
    }

    @GetMapping(value = "/related/{entityType}/{entityId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    @DataQualityEditCheck
    public List<ProfileRelatedQualityDTO> getRelatedQuality(@PathVariable String entityType,
                                                            @PathVariable Integer entityId) {
        return dataQualityService.getRelatedQualityForEntity(entityType, entityId);
    }

    @GetMapping(value = "/issues", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public DataQualityIssuePageDTO findRepositoryIssues(
        @RequestParam String profileName,
        @RequestParam(required = false) String target,
        @RequestParam(required = false) QualityDimension dimension,
        @RequestParam(required = false) IssueSeverity severity,
        @RequestParam(required = false) String constraintKey,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate assessmentDate,
        @RequestParam(required = false) String cursor,
        @RequestParam(required = false) Integer size,
        @RequestHeader("Authorization") String bearerToken) {
        return dataQualityService.findRepositoryIssues(resolveOrganisationUnitId(bearerToken),
            profileName, target, dimension, severity, constraintKey, assessmentDate, cursor,
            size);
    }

    @GetMapping(value = "/issues/{entityType}/{entityId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    @DataQualityEditCheck
    public DataQualityIssuePageDTO findIssues(@PathVariable String entityType,
                                              @PathVariable Integer entityId,
                                              @RequestParam String profileName,
                                              @RequestParam(required = false) String target,
                                              @RequestParam(required = false)
                                              QualityDimension dimension,
                                              @RequestParam(required = false)
                                              IssueSeverity severity,
                                              @RequestParam(required = false)
                                              String constraintKey,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                              LocalDate assessmentDate,
                                              @RequestParam(required = false) String cursor,
                                              @RequestParam(required = false) Integer size) {
        return dataQualityService.findIssuesForEntity(entityType, entityId, profileName, target,
            dimension, severity, constraintKey, assessmentDate, cursor, size);
    }

    @GetMapping(value = "/issue/{assessmentId}/{ruleKey}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    @DataQualityEditCheck
    public DataQualityIssueDetailsDTO findIssueDetails(@PathVariable Integer assessmentId,
                                                       @PathVariable String ruleKey) {
        return dataQualityService.findIssueDetails(assessmentId, ruleKey);
    }

    @GetMapping(value = "/profiles/{profileName}/constraints",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public List<ConstraintSummaryDTO> listProfileConstraints(@PathVariable String profileName,
                                                             @RequestParam(required = false)
                                                             String target) {
        return dataQualityService.listProfileConstraints(profileName, target);
    }

    @GetMapping(value = "/profiles/names", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public List<DataQualityProfileSummaryDTO> listPolicyNames() {
        return dataQualityService.listDataQualityProfileNames();
    }

    @GetMapping(value = "/profiles", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public List<DataQualityProfileDTO> listAllPolicies() {
        return dataQualityService.listAllDataQualityProfiles();
    }

    @GetMapping(value = "/policy", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public PolicyExplorerDTO getPolicy(
        @RequestParam String profileName,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate assessmentDate,
        @RequestHeader("Authorization") String bearerToken) {
        return dataQualityService.getPolicy(resolveOrganisationUnitId(bearerToken), profileName,
            assessmentDate);
    }

    // An admin has no unit and sees the repository; everyone else sees their own sub-hierarchy.
    private Integer resolveOrganisationUnitId(String bearerToken) {
        var user = userService.findOne(tokenUtil.extractUserIdFromToken(bearerToken));

        return Objects.nonNull(user.getOrganisationUnit())
            ? user.getOrganisationUnit().getId()
            : null;
    }
}
