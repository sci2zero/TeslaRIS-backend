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
                                         Integer assessmentYear, Integer commissionId,
                                         String locale, Integer userId) {
        var commissionName = commissionService.findOne(commissionId).getDescription().stream()
            .filter(desc -> desc.getLanguage().getLanguageTag().equals(locale.toUpperCase()))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Locale " + locale + " does not exist."))
            .getContent().replace(" ", "_");
        taskManagerService.scheduleTask(
            "ReportGeneration-" + commissionName +
                "-" + reportType + "-" + assessmentYear +
                "-" + UUID.randomUUID(), timeToRun,
            () -> generateReport(reportType, assessmentYear, commissionId, locale),
            userId);
    }

    @Override
    public void generateReport(ReportType reportType, Integer assessmentYear,
                               Integer commissionId, String locale) {
        String templateName = getTemplateName(reportType);
        int startYear = getStartYear(reportType, assessmentYear);

        try {
            var document = ReportGenerationUtil.loadDocumentTemplate(templateName);
            var assessmentResponses = personAssessmentClassificationService.assessResearchers(
                commissionId, new ArrayList<>(), startYear, assessmentYear);

            var reportData = generateReportData(reportType, assessmentResponses, locale);

            ReportGenerationUtil.insertFields(document, reportData.a);
            ReportGenerationUtil.dynamicallyGenerateTable(document, reportData.b, 0);

            var reportFileName =
                getReportFileName(reportType, commissionId, assessmentYear, locale);

            commissionReportRepository.getReport(commissionId, reportFileName)
                .ifPresent(commissionReportRepository::delete);
            commissionReportRepository.save(
                new CommissionReport(commissionService.findOne(commissionId), reportFileName));

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
        };
    }

    private int getStartYear(ReportType reportType, int assessmentYear) {
        return switch (reportType) {
            case TABLE_67, TABLE_67_POSITIONS -> assessmentYear - 9;
            case TABLE_63 -> assessmentYear;
        };
    }

    private Pair<Map<String, String>, List<List<String>>> generateReportData(
        ReportType reportType, List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses,
        String locale) {
        return switch (reportType) {
            case TABLE_67 ->
                ReportGenerationUtil.constructDataForTable67(assessmentResponses, locale);
            case TABLE_67_POSITIONS ->
                ReportGenerationUtil.constructDataForTable67WithPosition(assessmentResponses,
                    locale);
            case TABLE_63 ->
                ReportGenerationUtil.constructDataForTable63(assessmentResponses, locale);
        };
    }

    private String getReportFileName(ReportType reportType, Integer commissionId,
                                     Integer assessmentYear, String locale) {
        return reportType + "_" + commissionId + "_" + assessmentYear + "_" + locale + ".docx";
    }
}
