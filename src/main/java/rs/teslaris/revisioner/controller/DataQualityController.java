package rs.teslaris.revisioner.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.revisioner.annotation.DataQualityEditCheck;
import rs.teslaris.revisioner.dto.DataQualityAssessmentDTO;
import rs.teslaris.revisioner.dto.DataQualityIssueDTO;
import rs.teslaris.revisioner.dto.DataQualityIssueDetailsDTO;
import rs.teslaris.revisioner.dto.DataQualityProfileDTO;
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

    @GetMapping(value = "/issues/{entityType}/{entityId}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    @DataQualityEditCheck
    public Page<DataQualityIssueDTO> findIssues(@PathVariable String entityType,
                                                @PathVariable Integer entityId,
                                                @RequestParam String profileName,
                                                @RequestParam(required = false) String target,
                                                @RequestParam(required = false)
                                                QualityDimension dimension,
                                                @RequestParam(required = false)
                                                IssueSeverity severity,
                                                @RequestParam(required = false)
                                                String constraintKey,
                                                Pageable pageable) {
        return dataQualityService.findIssuesForEntity(entityType, entityId, profileName, target,
            dimension, severity, constraintKey, pageable);
    }

    @GetMapping(value = "/issue/{assessmentId}/{ruleKey}",
        produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    @DataQualityEditCheck
    public DataQualityIssueDetailsDTO findIssueDetails(@PathVariable Integer assessmentId,
                                                       @PathVariable String ruleKey) {
        return dataQualityService.findIssueDetails(assessmentId, ruleKey);
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
}
