package rs.teslaris.thesislibrary.service.impl;

import io.minio.GetObjectResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
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
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.DocumentFileBackup;
import rs.teslaris.core.model.document.FileSection;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.repository.document.DocumentFileBackupRepository;
import rs.teslaris.core.repository.document.ThesisRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.util.BackupZipBuilder;
import rs.teslaris.core.util.exceptionhandling.exception.BackupException;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.thesislibrary.service.interfaces.ThesisLibraryBackupService;
import rs.teslaris.thesislibrary.util.RegistryBookGenerationUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThesisLibraryBackupServiceImpl implements ThesisLibraryBackupService {

    private final FileService fileService;

    private final ThesisRepository thesisRepository;

    private final OrganisationUnitService organisationUnitService;

    private final DocumentFileBackupRepository documentFileBackupRepository;

    private final TaskManagerService taskManagerService;

    private final UserRepository userRepository;

    private final MessageSource messageSource;

    private final Map<FileSection, Function<Thesis, Set<DocumentFile>>> sectionAccessors = Map.of(
        FileSection.FILE_ITEMS, Thesis::getFileItems,
        FileSection.PROOFS, Thesis::getProofs,
        FileSection.PRELIMINARY_FILES, Thesis::getPreliminaryFiles,
        FileSection.PRELIMINARY_SUPPLEMENTS, Thesis::getPreliminarySupplements,
        FileSection.COMMISSION_REPORTS, Thesis::getCommissionReports
    );


    @Override
    public String scheduleBackupGeneration(Integer institutionId,
                                           LocalDate from, LocalDate to,
                                           List<ThesisType> types,
                                           List<FileSection> fileSections,
                                           Boolean defended,
                                           Boolean putOnReview, Integer userId, String language) {
        if (!defended && !putOnReview) {
            throw new BackupException("You must select at least one of: defended or putOnReview.");
        }

        if (from.isAfter(to)) {
            throw new BackupException("'From' date cannot be later than 'to' date.");
        }

        var reportGenerationTime = taskManagerService.findNextFreeExecutionTime();
        taskManagerService.scheduleTask(
            "Library_Backup-" + institutionId +
                "-" + from + "_" + to +
                "-" + UUID.randomUUID(), reportGenerationTime,
            () -> generateBackupForPeriodAndInstitution(institutionId, from, to, types,
                fileSections, defended, putOnReview, language),
            userId);
        return reportGenerationTime.getHour() + ":" + reportGenerationTime.getMinute() + "h";
    }

    private void generateBackupForPeriodAndInstitution(Integer institutionId,
                                                       LocalDate from, LocalDate to,
                                                       List<ThesisType> types,
                                                       List<FileSection> fileSections,
                                                       Boolean defended,
                                                       Boolean putOnReview, String language) {
        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        BackupZipBuilder zipBuilder = null;
        try {
            zipBuilder = new BackupZipBuilder("thesis-backup");

            while (hasNextPage) {
                List<Thesis> chunk = thesisRepository
                    .findThesesForBackup(from, to, types, institutionId, defended, putOnReview,
                        PageRequest.of(pageNumber, chunkSize)).getContent();

                for (var thesis : chunk) {
                    processThesis(thesis, fileSections, zipBuilder, language);
                }

                pageNumber++;
                hasNextPage = chunk.size() == chunkSize;
            }

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

    private void processThesis(Thesis thesis, List<FileSection> fileSections,
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

        for (var section : fileSections) {
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

    public Map<FileSection, String> getSectionPaths(String language) {
        return Map.of(
            FileSection.FILE_ITEMS, messageSource.getMessage("backup.fileItems", new Object[] {},
                Locale.forLanguageTag(language)),
            FileSection.PROOFS, messageSource.getMessage("backup.proofs", new Object[] {},
                Locale.forLanguageTag(language)),
            FileSection.PRELIMINARY_FILES,
            messageSource.getMessage("backup.preliminaryFiles", new Object[] {},
                Locale.forLanguageTag(language)),
            FileSection.PRELIMINARY_SUPPLEMENTS,
            messageSource.getMessage("backup.preliminarySupplements", new Object[] {},
                Locale.forLanguageTag(language)),
            FileSection.COMMISSION_REPORTS,
            messageSource.getMessage("backup.commissionReports", new Object[] {},
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
        return "BACKUP_" +
            RegistryBookGenerationUtil.getTransliteratedContent(institution.getName())
                .replace(" ", "_") + "_" + from + "_" + to + "_" + UUID.randomUUID() + ".zip";
    }
}
