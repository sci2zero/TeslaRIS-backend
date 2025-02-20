package rs.teslaris.core.assessment.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.EnrichedResearcherAssessmentResponseDTO;
import rs.teslaris.core.assessment.model.CommissionReport;
import rs.teslaris.core.assessment.model.ReportType;
import rs.teslaris.core.assessment.repository.CommissionReportRepository;
import rs.teslaris.core.assessment.service.interfaces.CommissionService;
import rs.teslaris.core.assessment.service.interfaces.PersonAssessmentClassificationService;
import rs.teslaris.core.assessment.service.interfaces.ReportingService;
import rs.teslaris.core.assessment.util.ReportGenerationUtil;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
public class ReportingServiceImpl implements ReportingService {

    private final PersonAssessmentClassificationService personAssessmentClassificationService;

    private final CommissionService commissionService;

    private final TaskManagerService taskManagerService;

    private final CommissionReportRepository commissionReportRepository;


    @Override
    public void scheduleReportGeneration(LocalDateTime timeToRun, ReportType reportType,
                                         Integer assessmentYear, List<Integer> commissionIds,
                                         String locale, Integer topLevelInstitutionId,
                                         Integer userId) {
        var commissionName =
            commissionService.findOne(commissionIds.getFirst()).getDescription().stream()
                .filter(desc -> desc.getLanguage().getLanguageTag().equals(locale.toUpperCase()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Locale " + locale + " does not exist."))
                .getContent().replace(" ", "_");
        taskManagerService.scheduleTask(
            "ReportGeneration-" + commissionName +
                "-" + reportType + "-" + assessmentYear +
                "-" + UUID.randomUUID(), timeToRun,
            () -> generateReport(reportType, assessmentYear, commissionIds, locale,
                topLevelInstitutionId),
            userId);
    }

    @Override
    public void generateReport(ReportType reportType, Integer assessmentYear,
                               List<Integer> commissionIds, String locale,
                               Integer topLevelInstitutionId) {
        String templateName = getTemplateName(reportType);
        int startYear = getStartYear(reportType, assessmentYear);

        try {
            var document = ReportGenerationUtil.loadDocumentTemplate(templateName);

            List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses = new ArrayList<>();

            var topLevelInstitutionReport =
                reportType.equals(ReportType.TABLE_TOP_LEVEL_INSTITUTION) ||
                    reportType.equals(ReportType.TABLE_TOP_LEVEL_INSTITUTION_COLORED) ||
                    reportType.equals(ReportType.TABLE_TOP_LEVEL_INSTITUTION_SUMMARY);
            if (reportType.equals(ReportType.TABLE_64)) {
                for (var year = startYear; year <= assessmentYear; year++) {
                    assessmentResponses.addAll(
                        personAssessmentClassificationService.assessResearchers(
                            commissionIds.getFirst(), new ArrayList<>(), year, year, null));
                    if (!assessmentResponses.isEmpty()) {
                        assessmentResponses.getFirst().setToYear(assessmentYear);
                    }
                }
            } else if (topLevelInstitutionReport) {
                commissionIds.forEach(commissionId -> {
                    assessmentResponses.addAll(
                        personAssessmentClassificationService.assessResearchers(
                            commissionId, new ArrayList<>(), startYear, assessmentYear,
                            topLevelInstitutionId));
                });
            } else {
                assessmentResponses.addAll(personAssessmentClassificationService.assessResearchers(
                    commissionIds.getFirst(), new ArrayList<>(), startYear, assessmentYear, null));
            }

            if (reportType.equals(ReportType.TABLE_TOP_LEVEL_INSTITUTION_SUMMARY)) {
                var columns =
                    ReportGenerationUtil.constructDataForCommissionColumns(commissionIds, locale);
                ReportGenerationUtil.addColumnsToFirstRow(document, columns, 0);
            }

            var reportData =
                generateReportData(reportType, assessmentResponses, commissionIds, locale);

            ReportGenerationUtil.insertFields(document, reportData.a);

            ReportGenerationUtil.dynamicallyGenerateTableRows(document, reportData.b, 0);

            if (topLevelInstitutionReport) {
                reportData = ReportGenerationUtil.constructDataForTableForAllPublications(
                    assessmentResponses,
                    reportType.equals(ReportType.TABLE_TOP_LEVEL_INSTITUTION_COLORED));
                ReportGenerationUtil.dynamicallyGenerateTableRows(document, reportData.b, 1);
            }

            var reportFileName =
                getReportFileName(reportType, commissionIds.getFirst(), assessmentYear, locale);

            commissionIds.forEach(commissionId -> {
                commissionReportRepository.getReport(commissionId, reportFileName)
                    .ifPresent(commissionReportRepository::delete);
                commissionReportRepository.save(
                    new CommissionReport(commissionService.findOne(commissionId), reportFileName));
            });
            ReportGenerationUtil.saveReport(document, reportFileName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate report for " + reportType, e);
        }
    }

    @Override
    public List<String> getAvailableReportsForCommission(Integer commissionId) {
        return commissionReportRepository.getAvailableReportsForCommission(commissionId);
    }

    private String getTemplateName(ReportType reportType) {
        return switch (reportType) {
            case TABLE_67, TABLE_67_POSITIONS -> "table67Template.docx";
            case TABLE_63 -> "table63Template.docx";
            case TABLE_64 -> "table64Template.docx";
            case TABLE_TOP_LEVEL_INSTITUTION -> "tableInstitutionTemplate.docx";
            case TABLE_TOP_LEVEL_INSTITUTION_SUMMARY -> "tableInstitutionSummaryTemplate.docx";
            case TABLE_TOP_LEVEL_INSTITUTION_COLORED -> "tableInstitutionColoredTemplate.docx";
        };
    }

    private int getStartYear(ReportType reportType, int assessmentYear) {
        return switch (reportType) {
            case TABLE_67, TABLE_67_POSITIONS -> assessmentYear - 9;
            case TABLE_63, TABLE_TOP_LEVEL_INSTITUTION, TABLE_TOP_LEVEL_INSTITUTION_SUMMARY, TABLE_TOP_LEVEL_INSTITUTION_COLORED ->
                assessmentYear;
            case TABLE_64 -> assessmentYear - 2;
        };
    }

    private Pair<Map<String, String>, List<List<String>>> generateReportData(
        ReportType reportType, List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses,
        List<Integer> commissionIds, String locale) {
        return switch (reportType) {
            case TABLE_67 ->
                ReportGenerationUtil.constructDataForTable67(assessmentResponses, locale);
            case TABLE_67_POSITIONS ->
                ReportGenerationUtil.constructDataForTable67WithPosition(assessmentResponses,
                    locale);
            case TABLE_63 ->
                ReportGenerationUtil.constructDataForTable63(assessmentResponses, locale);
            case TABLE_64 ->
                ReportGenerationUtil.constructDataForTable64(assessmentResponses, locale);
            case TABLE_TOP_LEVEL_INSTITUTION ->
                ReportGenerationUtil.constructDataForTableTopLevelInstitution(assessmentResponses,
                    locale);
            case TABLE_TOP_LEVEL_INSTITUTION_COLORED ->
                ReportGenerationUtil.constructDataForTableTopLevelInstitutionColored(
                    assessmentResponses,
                    locale);
            case TABLE_TOP_LEVEL_INSTITUTION_SUMMARY ->
                ReportGenerationUtil.constructDataForTableTopLevelInstitutionSummary(
                    assessmentResponses, commissionIds, locale);
        };
    }

    private String getReportFileName(ReportType reportType, Integer commissionId,
                                     Integer assessmentYear, String locale) {
        return reportType + "_" + commissionId + "_" + assessmentYear + "_" + locale + ".docx";
    }
}
