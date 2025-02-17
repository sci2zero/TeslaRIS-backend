package rs.teslaris.core.assessment.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.EnrichedResearcherAssessmentResponseDTO;
import rs.teslaris.core.assessment.model.ReportType;
import rs.teslaris.core.assessment.service.interfaces.PersonAssessmentClassificationService;
import rs.teslaris.core.assessment.service.interfaces.ReportingService;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.ReportGenerationUtil;

@Service
@RequiredArgsConstructor
public class ReportingServiceImpl implements ReportingService {

    private final PersonAssessmentClassificationService personAssessmentClassificationService;

    @Override
    public void generateReport(ReportType reportType, Integer assessmentYear,
                               Integer commissionId) {
        String templateName = getTemplateName(reportType);
        int startYear = getStartYear(reportType, assessmentYear);

        try {
            var document = ReportGenerationUtil.loadDocumentTemplate(templateName);
            var assessmentResponses = personAssessmentClassificationService.assessResearchers(
                commissionId, new ArrayList<>(), startYear, assessmentYear);

            var reportData = generateReportData(reportType, assessmentResponses);

            ReportGenerationUtil.insertFields(document, reportData.a);
            ReportGenerationUtil.dynamicallyGenerateTable(document, reportData.b, 0);
            ReportGenerationUtil.saveReport(document,
                getReportFileName(reportType, commissionId, assessmentYear));
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate report for " + reportType, e);
        }
    }

    private String getTemplateName(ReportType reportType) {
        return switch (reportType) {
            case TABLE_67 -> "table67Template.docx";
            case TABLE_67_POSITIONS -> "table67TemplateWithPositions.docx";
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
        ReportType reportType, List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses) {
        return switch (reportType) {
            case TABLE_67 -> ReportGenerationUtil.constructDataForTable67(assessmentResponses);
            case TABLE_67_POSITIONS ->
                ReportGenerationUtil.constructDataForTable67WithPosition(assessmentResponses);
            case TABLE_63 -> ReportGenerationUtil.constructDataForTable63(assessmentResponses);
        };
    }

    private String getReportFileName(ReportType reportType, Integer commissionId,
                                     Integer assessmentYear) {
        return reportType + "_" + commissionId + "_" + assessmentYear + ".docx";
    }
}
