package rs.teslaris.thesislibrary.service.impl;

import io.minio.GetObjectResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.dto.commontypes.RelativeDateDTO;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.DocumentFileBackup;
import rs.teslaris.core.model.document.DocumentFileSection;
import rs.teslaris.core.model.document.FileSection;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.repository.document.DocumentFileBackupRepository;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.commontypes.CSVExportService;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.BackupZipBuilder;
import rs.teslaris.core.util.exceptionhandling.exception.BackupException;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.thesislibrary.dto.ThesisCSVExportRequestDTO;
import rs.teslaris.thesislibrary.model.ThesisFileSection;
import rs.teslaris.thesislibrary.service.interfaces.ThesisLibraryBackupService;
import rs.teslaris.thesislibrary.util.RegistryBookGenerationUtil;

@Service
@RequiredArgsConstructor
@Slf4j
@Traceable
public class ThesisLibraryBackupServiceImpl implements ThesisLibraryBackupService {

    private final FileService fileService;

    private final ThesisRepository thesisRepository;

    private final OrganisationUnitService organisationUnitService;

    private final DocumentFileBackupRepository documentFileBackupRepository;

    private final TaskManagerService taskManagerService;

    private final UserRepository userRepository;

    private final MessageSource messageSource;

    private final CSVExportService csvExportService;


    private final Map<FileSection, Function<Thesis, Set<DocumentFile>>> sectionAccessors = Map.of(
        DocumentFileSection.FILE_ITEMS, Thesis::getFileItems,
        DocumentFileSection.PROOFS, Thesis::getProofs,
        ThesisFileSection.PRELIMINARY_FILES, Thesis::getPreliminaryFiles,
        ThesisFileSection.PRELIMINARY_SUPPLEMENTS, Thesis::getPreliminarySupplements,
        ThesisFileSection.COMMISSION_REPORTS, Thesis::getCommissionReports
    );


    @Override
    public String scheduleBackupGeneration(Integer institutionId,
                                           RelativeDateDTO from, RelativeDateDTO to,
                                           List<ThesisType> types,
                                           List<FileSection> thesisFileSections,
                                           Boolean defended, Boolean putOnReview,
                                           Integer userId, String language,
                                           ExportFileType metadataFormat,
                                           RecurrenceType recurrence) {
        if (!defended && !putOnReview) {
            throw new BackupException("You must select at least one of: defended or putOnReview.");
        }

        var fromDate = from.computeDate();
        var toDate = to.computeDate();

        if (fromDate.isAfter(toDate)) {
            throw new BackupException("dateRangeIssueMessage");
        }

        var reportGenerationTime = taskManagerService.findNextFreeExecutionTime();
        var taskId = taskManagerService.scheduleTask(
            "Library_Backup-" + institutionId +
                "-" + from + "_" + to +
                "-" + UUID.randomUUID(), reportGenerationTime,
            () -> generateBackupForPeriodAndInstitution(institutionId, fromDate, toDate, types,
                thesisFileSections, defended, putOnReview, language, metadataFormat),
            userId, recurrence);

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, reportGenerationTime,
                ScheduledTaskType.THESIS_LIBRARY_BACKUP, new HashMap<>() {{
                put("institutionId", institutionId);
                put("from", from.toString());
                put("to", to.toString());
                put("types", types);
                put("thesisFileSections", thesisFileSections);
                put("defended", defended);
                put("putOnReview", putOnReview);
                put("userId", userId);
                put("language", language);
                put("metadataFormat", metadataFormat);
            }}, recurrence));

        return reportGenerationTime.getHour() + ":" + reportGenerationTime.getMinute() + "h";
    }

    private void generateBackupForPeriodAndInstitution(Integer institutionId,
                                                       LocalDate from, LocalDate to,
                                                       List<ThesisType> types,
                                                       List<FileSection> thesisFileSections,
                                                       Boolean defended, Boolean putOnReview,
                                                       String language,
                                                       ExportFileType metadataFormat) {
        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        BackupZipBuilder zipBuilder = null;
        try {
            zipBuilder = new BackupZipBuilder("thesis-backup");

            var processedThesisIds = new ArrayList<Integer>();
            while (hasNextPage) {
                List<Thesis> chunk = thesisRepository
                    .findThesesForBackup(from, to, types, institutionId, defended, putOnReview,
                        PageRequest.of(pageNumber, chunkSize)).getContent();

                for (var thesis : chunk) {
                    processThesis(thesis, thesisFileSections, zipBuilder, language);
                    processedThesisIds.add(thesis.getId());
                }

                pageNumber++;
                hasNextPage = chunk.size() == chunkSize;
            }

            createMetadataCSV(processedThesisIds, language, zipBuilder, metadataFormat);

            var institution = organisationUnitService.findOne(institutionId);
            var serverFilename = generateBackupFileName(from, to, institution);
            serverFilename = fileService.store(zipBuilder.convertToMultipartFile(
                    zipBuilder.buildZipAndGetResource().getContentAsByteArray(), serverFilename),
                serverFilename.split("\\.")[0]);

            var newBackupFile = new DocumentFileBackup();
            newBackupFile.setInstitution(institution);
            newBackupFile.setBackupFileName(serverFilename);
            documentFileBackupRepository.save(newBackupFile);
        } catch (IOException e) {
            throw new StorageException("Failed to generate backup file.");
        } finally {
            if (Objects.nonNull(zipBuilder)) {
                zipBuilder.cleanup();
            }
        }
    }

    private void createMetadataCSV(List<Integer> exportEntityIds, String language,
                                   BackupZipBuilder zipBuilder, ExportFileType metadataFormat) {
        var exportRequest = new ThesisCSVExportRequestDTO();
        exportRequest.setExportMaxPossibleAmount(false);
        exportRequest.setExportEntityIds(exportEntityIds);
        exportRequest.setExportLanguage(language);
        exportRequest.setExportFileType(metadataFormat);
        exportRequest.setColumns(
            List.of("title_sr", "title_other", "year", "description_sr", "description_other",
                "keywords_sr", "keywords_other", "author_names", "editor_names",
                "board_member_names", "board_president_name", "advisor_names", "reviewer_names",
                "type", "publication_type", "doi", "scopus_id", "is_open_access", "event_id",
                "journal_id"));
        exportRequest.setApa(true);
        exportRequest.setMla(true);
        exportRequest.setChicago(true);
        exportRequest.setHarvard(true);
        exportRequest.setVancouver(true);

        var metadataFile = csvExportService.exportDocumentsToCSV(exportRequest);
        try {
            zipBuilder.copyFileToRoot(metadataFile.getInputStream(), "metadata.csv");
        } catch (IOException e) {
            throw new RuntimeException(e); // should never happen
        }
    }

    private void processThesis(Thesis thesis, List<FileSection> thesisFileSections,
                               BackupZipBuilder zipBuilder, String language)
        throws IOException {
        var author = thesis.getContributors().stream()
            .filter(contributor -> contributor.getContributionType()
                .equals(DocumentContributionType.AUTHOR))
            .findFirst();

        if (author.isEmpty()) {
            log.warn("Author is missing for Thesis with ID {}", thesis.getId());
            return;
        }

        var thesisDir = zipBuilder.createSubDir("theses/" +
            author.get().getAffiliationStatement().getDisplayPersonName().toString()
                .replace(" ", "_")
            + "_" + thesis.getId());

        for (var section : thesisFileSections) {
            var fileItems = sectionAccessors.get(section).apply(thesis);
            if (Objects.isNull(fileItems) || fileItems.isEmpty()) {
                continue;
            }

            var sectionPaths = getSectionPaths(language);
            var sectionDir =
                zipBuilder.createSubDir(thesisDir.resolve(sectionPaths.get(section)).toString());
            for (var fileItem : fileItems) {
                try (var file = fileService.loadAsResource(fileItem.getServerFilename())) {
                    zipBuilder.copyFile(file, sectionDir.resolve(fileItem.getFilename()));
                } catch (IOException e) {
                    log.warn("Failed to load file {}", fileItem.getServerFilename(), e);
                }
            }
        }
    }

    private Map<FileSection, String> getSectionPaths(String language) {
        return Map.of(
            DocumentFileSection.FILE_ITEMS, messageSource.getMessage(
                DocumentFileSection.FILE_ITEMS.getInternationalizationMessageName(),
                new Object[] {},
                Locale.forLanguageTag(language)),
            DocumentFileSection.PROOFS, messageSource.getMessage(
                DocumentFileSection.PROOFS.getInternationalizationMessageName(), new Object[] {},
                Locale.forLanguageTag(language)),
            ThesisFileSection.PRELIMINARY_FILES,
            messageSource.getMessage(
                ThesisFileSection.PRELIMINARY_FILES.getInternationalizationMessageName(),
                new Object[] {},
                Locale.forLanguageTag(language)),
            ThesisFileSection.PRELIMINARY_SUPPLEMENTS,
            messageSource.getMessage(
                ThesisFileSection.PRELIMINARY_SUPPLEMENTS.getInternationalizationMessageName(),
                new Object[] {},
                Locale.forLanguageTag(language)),
            ThesisFileSection.COMMISSION_REPORTS,
            messageSource.getMessage(
                ThesisFileSection.COMMISSION_REPORTS.getInternationalizationMessageName(),
                new Object[] {},
                Locale.forLanguageTag(language))
        );
    }

    @Override
    public List<String> listAvailableBackups(Integer userId) {
        var availableReports = new ArrayList<String>();
        var userInstitution = userRepository.findOrganisationUnitIdForUser(userId);

        if (Objects.nonNull(userInstitution) && userInstitution > 0) {
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
                userInstitution).forEach(institutionId -> {
                availableReports.addAll(
                    documentFileBackupRepository.findByInstitution(institutionId).stream()
                        .map(DocumentFileBackup::getBackupFileName).toList());
            });
        } else {
            availableReports.addAll(documentFileBackupRepository.findAll().stream()
                .map(DocumentFileBackup::getBackupFileName).toList());
        }

        return availableReports;
    }

    @Override
    public GetObjectResponse serveAndDeleteBackupFile(String backupFileName, Integer userId)
        throws IOException {
        var report = documentFileBackupRepository.findByBackupFileName(backupFileName)
            .orElseThrow(() -> new StorageException("No backup with given filename."));
        var userInstitution = userRepository.findOrganisationUnitIdForUser(userId);

        if (Objects.nonNull(userInstitution) && userInstitution > 0 &&
            !organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(userInstitution)
                .contains(report.getInstitution().getId())) {
            throw new LoadingException("Unauthorised to download backup.");
        }

        var resource = fileService.loadAsResource(backupFileName);
        fileService.delete(backupFileName);
        documentFileBackupRepository.delete(report);
        return resource;
    }

    private String generateBackupFileName(LocalDate from, LocalDate to,
                                          OrganisationUnit institution) {
        return "THESIS_BACKUP_" +
            RegistryBookGenerationUtil.getTransliteratedContent(institution.getName())
                .replace(" ", "_") + "_" + from + "_" + to + "_" + UUID.randomUUID() + ".zip";
    }
}
