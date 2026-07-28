package rs.teslaris.revisioner.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.revisioner.converter.DataQualityAssessmentConverter;
import rs.teslaris.revisioner.converter.DataQualityProfileConverter;
import rs.teslaris.revisioner.dto.DataQualityAssessmentDTO;
import rs.teslaris.revisioner.dto.DataQualityProfileDTO;
import rs.teslaris.revisioner.dto.QualityReportResponseDTO;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;
import rs.teslaris.revisioner.repository.EntityRevisionRepository;
import rs.teslaris.revisioner.service.interfaces.DataQualityService;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataQualityServiceImpl implements DataQualityService {

    private final EntityRevisionRepository entityRevisionRepository;

    private final LanguageTagService languageTagService;


    @Override
    @Transactional(readOnly = true)
    public List<QualityReportResponseDTO> getQualityReportForEntity(String entityType,
                                                                    Integer entityId) {
        var entityRevision = entityRevisionRepository
            .findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(entityType, entityId);

        if (entityRevision.isEmpty()) {
            return List.of();
        }

        var qualityReport = new ArrayList<QualityReportResponseDTO>();

        entityRevision.get().getAssessments().forEach(assessment -> {
            List<Pair<IssueSeverity, List<MultilingualContentDTO>>> assessmentReport =
                new ArrayList<>();

            assessment.getIssues().forEach(issue -> {
                var remarks = DataQualityAssessmentConfigurationLoader.getDataQualityRemark(
                    assessment.getProfileName(),
                    assessment.getProfileVersion(),
                    issue.getKey(),
                    issue.getParameters().toArray()
                );

                var multilingualContents = remarks.stream()
                    .map(r -> new MultilingualContentDTO(
                        r.getLanguage().getId(),
                        r.getLanguage().getLanguageTag(),
                        r.getContent(),
                        r.getPriority()
                    ))
                    .toList();

                assessmentReport.add(new Pair<>(issue.getSeverity(), multilingualContents));
            });

            qualityReport.add(
                new QualityReportResponseDTO(
                    assessment.getProfileName() + " (" + assessment.getProfileVersion() + ")",
                    assessment.getQualityScore(),
                    assessment.getInfoFailedRules() +
                        assessment.getWarningFailedRules() +
                        assessment.getErrorFailedRules(),
                    LocalDate.ofInstant(assessment.getStartedAt(), ZoneId.systemDefault()),
                    assessment.getPublicationCandidate(),
                    assessmentReport
                )
            );
        });

        return qualityReport;
    }

    @Transactional(readOnly = true)
    public List<DataQualityAssessmentDTO> findLatestAssessmentsForEntity(String entityType,
                                                                         Integer entityId) {
        var entityRevision = entityRevisionRepository
            .findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(entityType, entityId)
            .orElseThrow(() -> new NotFoundException(
                "No data quality assessment found for " + entityType + " with ID " + entityId +
                    "."));

        return entityRevision.getAssessments().stream().map(DataQualityAssessmentConverter::toDTO)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataQualityProfileDTO> listAllDataQualityProfiles() {
        var allProfiles = new ArrayList<DataQualityProfileDTO>();

        DataQualityAssessmentConfigurationLoader.listAvailableProfilesWithVersion()
            .forEach(profileAndVersion -> {
                var profile =
                    DataQualityAssessmentConfigurationLoader.getProfile(profileAndVersion.a,
                        profileAndVersion.b);

                allProfiles.add(DataQualityProfileConverter.toDTO(profile, languageTagService));
            });

        return allProfiles;
    }
}
