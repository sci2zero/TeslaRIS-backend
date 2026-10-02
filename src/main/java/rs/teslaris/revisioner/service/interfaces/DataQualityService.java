package rs.teslaris.revisioner.service.interfaces;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.revisioner.dto.ConstraintSummaryDTO;
import rs.teslaris.revisioner.dto.DataQualityAssessmentDTO;
import rs.teslaris.revisioner.dto.DataQualityIssueDetailsDTO;
import rs.teslaris.revisioner.dto.DataQualityIssuePageDTO;
import rs.teslaris.revisioner.dto.DataQualityProfileDTO;
import rs.teslaris.revisioner.dto.DataQualityProfileSummaryDTO;
import rs.teslaris.revisioner.dto.MetricSummaryDTO;
import rs.teslaris.revisioner.dto.PolicyExplorerDTO;
import rs.teslaris.revisioner.dto.ProfileRelatedQualityDTO;
import rs.teslaris.revisioner.dto.QualityReportResponseDTO;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

@Service
public interface DataQualityService {

    List<QualityReportResponseDTO> getQualityReportForEntity(String entityType, Integer entityId);

    List<DataQualityAssessmentDTO> findLatestAssessmentsForEntity(String entityType,
                                                                  Integer entityId);

    List<DataQualityAssessmentDTO> findAssessmentsForEntityVersion(String entityType,
                                                                   Integer entityId,
                                                                   Integer majorVersion,
                                                                   Integer minorVersion);

    List<ProfileRelatedQualityDTO> getRelatedQualityForEntity(String entityType,
                                                              Integer entityId);

    DataQualityIssuePageDTO findIssuesForEntity(String entityType, Integer entityId,
                                                String profileName, String target,
                                                QualityDimension dimension,
                                                IssueSeverity severity,
                                                String constraintKey,
                                                @Nullable String metric,
                                                @Nullable LocalDate assessmentDate,
                                                @Nullable String cursor,
                                                @Nullable Integer size);

    DataQualityIssuePageDTO findRepositoryIssues(@Nullable Integer organisationUnitId,
                                                 String profileName, String target,
                                                 QualityDimension dimension,
                                                 IssueSeverity severity, String constraintKey,
                                                 @Nullable String metric,
                                                 @Nullable LocalDate assessmentDate,
                                                 @Nullable String cursor,
                                                 @Nullable Integer size);

    DataQualityIssueDetailsDTO findIssueDetails(Integer assessmentId, String ruleKey);

    List<DataQualityProfileDTO> listAllDataQualityProfiles();

    List<DataQualityProfileSummaryDTO> listDataQualityProfileNames();

    List<ConstraintSummaryDTO> listProfileConstraints(String profileName,
                                                      @Nullable String target,
                                                      @Nullable QualityDimension dimension,
                                                      @Nullable String metric);

    List<MetricSummaryDTO> listProfileMetrics(String profileName);

    PolicyExplorerDTO getPolicy(@Nullable Integer organisationUnitId, String profileName,
                                @Nullable LocalDate assessmentDate);

    boolean reassessLatestRevision(String entityType, Integer entityId,
                                   String profileName);
}
