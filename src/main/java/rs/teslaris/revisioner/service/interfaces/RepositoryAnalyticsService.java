package rs.teslaris.revisioner.service.interfaces;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import rs.teslaris.revisioner.dto.DimensionQualityDTO;
import rs.teslaris.revisioner.dto.EntityTypeQualityDTO;
import rs.teslaris.revisioner.dto.IssueStatisticsDTO;
import rs.teslaris.revisioner.dto.PublicationCandidateAnalysisDTO;
import rs.teslaris.revisioner.dto.QualityTrendDTO;
import rs.teslaris.revisioner.dto.RepositoryOverviewDTO;
import rs.teslaris.revisioner.util.dataquality.TrendGranularity;
import rs.teslaris.revisioner.util.dataquality.TrendMetric;

@Service
public interface RepositoryAnalyticsService {

    RepositoryOverviewDTO getOverview(String profileName, Integer organisationUnitId,
                                      @Nullable LocalDate assessmentDate);

    PublicationCandidateAnalysisDTO getPublicationCandidateAnalysis(
        String profileName, Integer organisationUnitId, @Nullable LocalDate assessmentDate);

    List<EntityTypeQualityDTO> getQualityByEntityType(String profileName,
                                                      Integer organisationUnitId,
                                                      @Nullable LocalDate assessmentDate);

    IssueStatisticsDTO getIssueStatistics(String profileName, Integer organisationUnitId,
                                          @Nullable LocalDate assessmentDate);

    QualityTrendDTO getQualityTrend(String profileName, Integer organisationUnitId,
                                    TrendMetric metric, TrendGranularity granularity,
                                    @Nullable Integer points);

    List<DimensionQualityDTO> getQualityByDimension(String profileName,
                                                    Integer organisationUnitId,
                                                    @Nullable LocalDate assessmentDate);

    InputStreamResource exportPublicationCandidateAnalysis(String profileName,
                                                           Integer organisationUnitId,
                                                           @Nullable LocalDate assessmentDate,
                                                           String language);

    InputStreamResource exportOverview(String profileName, Integer organisationUnitId,
                                       @Nullable LocalDate assessmentDate, String language);

    InputStreamResource exportQualityByEntityType(String profileName, Integer organisationUnitId,
                                                  @Nullable LocalDate assessmentDate,
                                                  String language);

    InputStreamResource exportQualityByDimension(String profileName, Integer organisationUnitId,
                                                 @Nullable LocalDate assessmentDate,
                                                 String language);

    InputStreamResource exportIssueStatistics(String profileName, Integer organisationUnitId,
                                              @Nullable LocalDate assessmentDate, String language);

    InputStreamResource exportQualityTrend(String profileName, Integer organisationUnitId,
                                           TrendMetric metric, TrendGranularity granularity,
                                           @Nullable Integer points, String language);
}
