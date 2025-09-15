package rs.teslaris.assessment.service.impl;

import io.minio.GetObjectResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import rs.teslaris.assessment.dto.EnrichedResearcherAssessmentResponseDTO;
import rs.teslaris.assessment.dto.ReportDTO;
import rs.teslaris.assessment.model.CommissionReport;
import rs.teslaris.assessment.model.ReportType;
import rs.teslaris.assessment.repository.CommissionReportRepository;
import rs.teslaris.assessment.service.interfaces.CommissionService;
import rs.teslaris.assessment.service.interfaces.ReportingService;
import rs.teslaris.assessment.service.interfaces.classification.PersonAssessmentClassificationService;
import rs.teslaris.assessment.util.AssessmentReportGenerator;
import rs.teslaris.assessment.util.ReportTemplateEngine;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.core.util.scheduling.DateUtil;

@Service
@RequiredArgsConstructor
@Traceable
public class ReportingServiceImpl implements ReportingService {

    private final PersonAssessmentClassificationService personAssessmentClassificationService;

    private final CommissionService commissionService;

    private final TaskManagerService taskManagerService;

    private final CommissionReportRepository commissionReportRepository;

    private final FileService fileService;

    private final UserRepository userRepository;

    private final OrganisationUnitService organisationUnitService;


    @Override
    public void scheduleReportGeneration(LocalDateTime timeToRun, ReportType reportType,
                                         Integer assessmentYear, List<Integer> commissionIds,
                                         String locale, Integer topLevelInstitutionId,
                                         Integer userId, RecurrenceType recurrence) {
        checkCommissionAccessRights(commissionIds, userId);
        var commissionName =
            commissionService.findOne(commissionIds.getFirst()).getDescription().stream()
                .filter(desc -> desc.getLanguage().getLanguageTag().equals(locale.toUpperCase()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Locale " + locale + " does not exist."))
                .getContent().replace(" ", "_");
        var taskId = taskManagerService.scheduleTask(
            "ReportGeneration-" + userRepository.findOUIdForCommission(commissionIds.getFirst()) +
                "-" + commissionName +
                "-" + reportType + "-" + assessmentYear +
                "-" + UUID.randomUUID(), timeToRun,
            () -> generateReport(reportType, assessmentYear, commissionIds, locale,
                topLevelInstitutionId),
            userId, recurrence);

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timeToRun,
                ScheduledTaskType.REPORT_GENERATION, new HashMap<>() {{
                put("reportType", reportType);
                put("assessmentYear", assessmentYear);
                put("commissionIds", commissionIds);
                put("locale", locale);
                put("topLevelInstitutionId", topLevelInstitutionId);
                put("userId", userId);
            }}, recurrence));
    }

    @Override
    public void generateReport(ReportType reportType, Integer assessmentYear,
                               List<Integer> commissionIds, String locale,
                               Integer topLevelInstitutionId) {
        assessmentYear = DateUtil.calculateYearFromProvidedValue(assessmentYear);

        String templateName = getTemplateName(reportType);
        int startYear = getStartYear(reportType, assessmentYear);

        try {
            var document = ReportTemplateEngine.loadDocumentTemplate(templateName);
            var assessmentResponses =
                fetchAssessmentResponses(reportType, commissionIds, startYear, assessmentYear,
                    topLevelInstitutionId);

            if (reportType.equals(ReportType.TABLE_TOP_LEVEL_INSTITUTION_SUMMARY)) {
                var columns =
                    AssessmentReportGenerator.constructDataForCommissionColumns(commissionIds,
                        locale);
                ReportTemplateEngine.addColumnsToFirstRow(document, columns, 0);
            }

            processReportData(reportType, document, assessmentResponses, commissionIds, locale);

            saveReport(reportType, document, commissionIds, assessmentYear, locale);

        } catch (IOException e) {
            throw new LoadingException(
                "Failed to generate report for " + reportType + ". Reason: " + e.getMessage());
        }
    }

    @Override
    public List<String> getAvailableReportsForCommission(Integer commissionId, Integer userId) {
        checkCommissionAccessRights(List.of(commissionId), userId);
        return commissionReportRepository.getAvailableReportsForCommission(commissionId);
    }

    @Override
    public List<ReportDTO> getAvailableReportsForUser(Integer userId) {
        var employmentInstitutionId = userRepository.findOrganisationUnitIdForUser(userId);

        if (Objects.isNull(employmentInstitutionId)) { // ADMIN user
            return commissionReportRepository.findAll().stream()
                .map(report -> new ReportDTO(report.getCommission().getId(),
                    report.getReportFileName())).toList();
        }

        var subOUs =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(employmentInstitutionId);

        var returnData = new ArrayList<ReportDTO>();
        subOUs.forEach(institutionId -> {
            userRepository.findUserCommissionForOrganisationUnit(institutionId)
                .forEach(commission -> {
                    commissionReportRepository.getAvailableReportsForCommission(commission.getId())
                        .forEach(report -> {
                            returnData.add(new ReportDTO(commission.getId(), report));
                        });
                });
        });

        return returnData;
    }

    @Override
    public GetObjectResponse serveReportFile(String reportName, Integer userId,
                                             Integer commissionId) throws IOException {
        checkCommissionAccessRights(List.of(commissionId), userId);
        if (!commissionReportRepository.reportExists(commissionId, reportName)) {
            throw new NotFoundException("Report " + reportName + " does not exist.");
        }

        return fileService.loadAsResource(reportName);
    }

    private List<EnrichedResearcherAssessmentResponseDTO> fetchAssessmentResponses(
        ReportType reportType,
        List<Integer> commissionIds, int startYear, int assessmentYear,
        Integer topLevelInstitutionId) {

        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses = new ArrayList<>();
        boolean isTopLevelInstitutionReport = isTopLevelInstitutionReport(reportType);

        if (reportType.equals(ReportType.TABLE_64)) {
            for (int year = startYear; year <= assessmentYear; year++) {
                var responses = personAssessmentClassificationService.assessResearchers(
                    commissionIds.getFirst(), new ArrayList<>(), year, year, null);
                if (!responses.isEmpty()) {
                    responses.getFirst().setToYear(assessmentYear);
                }
                assessmentResponses.addAll(responses);
            }
        } else if (isTopLevelInstitutionReport) {
            commissionIds.forEach(commissionId -> assessmentResponses.addAll(
                personAssessmentClassificationService.assessResearchers(
                    commissionId, new ArrayList<>(), startYear, assessmentYear,
                    topLevelInstitutionId)));
        } else {
            assessmentResponses.addAll(personAssessmentClassificationService.assessResearchers(
                commissionIds.getFirst(), new ArrayList<>(), startYear, assessmentYear, null));
        }

        return assessmentResponses;
    }

    private boolean isTopLevelInstitutionReport(ReportType reportType) {
        return reportType.equals(ReportType.TABLE_TOP_LEVEL_INSTITUTION) ||
            reportType.equals(ReportType.TABLE_TOP_LEVEL_INSTITUTION_COLORED) ||
            reportType.equals(ReportType.TABLE_TOP_LEVEL_INSTITUTION_SUMMARY);
    }

    private void processReportData(ReportType reportType, XWPFDocument document,
                                   List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses,
                                   List<Integer> commissionIds, String locale) {

        var reportData = generateReportData(reportType, assessmentResponses, commissionIds, locale);
        ReportTemplateEngine.insertFields(document, reportData.a);
        ReportTemplateEngine.dynamicallyGenerateTableRows(document, reportData.b, 0);

        if (isTopLevelInstitutionReport(reportType)) {
            reportData = AssessmentReportGenerator.constructDataForTableForAllPublications(
                assessmentResponses,
                reportType.equals(ReportType.TABLE_TOP_LEVEL_INSTITUTION_COLORED));
            ReportTemplateEngine.dynamicallyGenerateTableRows(document, reportData.b, 1);
        }
    }

    private void saveReport(ReportType reportType, XWPFDocument document,
                            List<Integer> commissionIds, int assessmentYear, String locale)
        throws IOException {

        String reportFileName =
            getReportFileName(reportType, commissionIds.getFirst(), assessmentYear, locale);

        commissionIds.forEach(commissionId -> {
            commissionReportRepository.getReport(commissionId, reportFileName)
                .ifPresent(commissionReportRepository::delete);
            commissionReportRepository.save(
                new CommissionReport(commissionService.findOne(commissionId), reportFileName));
        });

        ReportTemplateEngine.saveReport(document, reportFileName);
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
            case TABLE_63, TABLE_TOP_LEVEL_INSTITUTION, TABLE_TOP_LEVEL_INSTITUTION_SUMMARY,
                 TABLE_TOP_LEVEL_INSTITUTION_COLORED -> assessmentYear;
            case TABLE_64 -> assessmentYear - 2;
        };
    }

    private Pair<Map<String, String>, List<List<String>>> generateReportData(
        ReportType reportType, List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses,
        List<Integer> commissionIds, String locale) {
        return switch (reportType) {
            case TABLE_67 ->
                AssessmentReportGenerator.constructDataForTable67(assessmentResponses, locale);
            case TABLE_67_POSITIONS ->
                AssessmentReportGenerator.constructDataForTable67WithPosition(assessmentResponses,
                    locale);
            case TABLE_63 ->
                AssessmentReportGenerator.constructDataForTable63(assessmentResponses, locale);
            case TABLE_64 ->
                AssessmentReportGenerator.constructDataForTable64(assessmentResponses, locale);
            case TABLE_TOP_LEVEL_INSTITUTION ->
                AssessmentReportGenerator.constructDataForTableTopLevelInstitution(
                    assessmentResponses,
                    locale);
            case TABLE_TOP_LEVEL_INSTITUTION_COLORED ->
                AssessmentReportGenerator.constructDataForTableTopLevelInstitutionColored(
                    assessmentResponses,
                    locale);
            case TABLE_TOP_LEVEL_INSTITUTION_SUMMARY ->
                AssessmentReportGenerator.constructDataForTableTopLevelInstitutionSummary(
                    assessmentResponses, commissionIds, locale);
        };
    }

    private String getReportFileName(ReportType reportType, Integer commissionId,
                                     Integer assessmentYear, String locale) {
        return reportType + "_" + commissionId + "_" + assessmentYear + "_" + locale + ".docx";
    }

    private void checkCommissionAccessRights(List<Integer> commissionIds, Integer userId) {
        var userOptional = userRepository.findByIdWithOrganisationUnit(userId);

        userOptional.ifPresent((user) -> {
            if (user.getAuthority().getAuthority().equals(UserRole.ADMIN.name())) {
                return;
            }

            if (!commissionIds.stream().map(userRepository::findOUIdForCommission).toList()
                .contains(user.getOrganisationUnit().getId())) {
                throw new LoadingException("You don't have permissions to download this report.");
            }
        });
    }
}
