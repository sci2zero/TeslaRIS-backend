package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.json.JsonData;
import io.minio.GetObjectResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.stereotype.Service;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.DocumentCSVExportRequestDTO;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.DocumentFileBackup;
import rs.teslaris.core.model.document.DocumentFileSection;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.repository.document.DocumentFileBackupRepository;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.commontypes.CSVExportService;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.DocumentBackupService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.util.BackupZipBuilder;
import rs.teslaris.core.util.exceptionhandling.exception.BackupException;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.search.StringUtil;


@Service
@RequiredArgsConstructor
@Slf4j
@Traceable
public class DocumentBackupServiceImpl implements DocumentBackupService {

    private final FileService fileService;

    private final ElasticsearchOperations elasticsearchOperations;

    private final DocumentRepository documentRepository;

    private final OrganisationUnitService organisationUnitService;

    private final DocumentFileBackupRepository documentFileBackupRepository;

    private final TaskManagerService taskManagerService;

    private final UserRepository userRepository;

    private final MessageSource messageSource;

    private final CSVExportService csvExportService;


    private final Map<DocumentFileSection, Function<Document, Set<DocumentFile>>> sectionAccessors =
        Map.of(
            DocumentFileSection.FILE_ITEMS, Document::getFileItems,
            DocumentFileSection.PROOFS, Document::getProofs
        );


    @Override
    public String scheduleBackupGeneration(Integer institutionId,
                                           Integer from, Integer to,
                                           List<DocumentPublicationType> types,
                                           List<DocumentFileSection> documentFileSections,
                                           Integer userId, String language,
                                           ExportFileType metadataFormat) {
        if (from > to) {
            throw new BackupException("dateRangeIssueMessage");
        }

        var reportGenerationTime = taskManagerService.findNextFreeExecutionTime();
        taskManagerService.scheduleTask(
            "Document_Backup-" + institutionId +
                "-" + from + "_" + to +
                "-" + UUID.randomUUID(), reportGenerationTime,
            () -> generateBackupForPeriodAndInstitution(institutionId, from, to, types,
                documentFileSections, language, metadataFormat),
            userId);
        return reportGenerationTime.getHour() + ":" + reportGenerationTime.getMinute() + "h";
    }

    private void generateBackupForPeriodAndInstitution(Integer institutionId,
                                                       Integer from, Integer to,
                                                       List<DocumentPublicationType> types,
                                                       List<DocumentFileSection> documentFileSections,
                                                       String language,
                                                       ExportFileType metadataFormat) {
        int chunkSize = 10;
        var institutionIds = new HashSet<>(
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId)
        );

        BackupZipBuilder zipBuilder = null;
        try {
            zipBuilder = new BackupZipBuilder("document-backup");

            var processedDocumentIds = new ArrayList<Integer>();
            for (var documentType : types) {
                int pageNumber = 0;
                boolean hasNextPage = true;

                var documentIdsForType =
                    getDocumentDatabaseIdsForBackup(from, to, institutionIds.stream().toList(),
                        documentType.name());
                while (hasNextPage) {
                    List<Document> chunk = fetchProcessableChunk(documentIdsForType,
                        PageRequest.of(pageNumber, chunkSize));

                    for (var document : chunk) {
                        processDocument(document, documentFileSections, zipBuilder, language);
                        processedDocumentIds.add(document.getId());
                    }

                    pageNumber++;
                    hasNextPage = chunk.size() == chunkSize;
                }
            }

            createMetadataCSV(processedDocumentIds, language, types, zipBuilder, metadataFormat);

            var institution = organisationUnitService.findOne(institutionId);
            var serverFilename = generateBackupFileName(from, to, institution, language);
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
                                   List<DocumentPublicationType> types,
                                   BackupZipBuilder zipBuilder, ExportFileType metadataFormat) {
        var exportRequest = new DocumentCSVExportRequestDTO();
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
        exportRequest.setAllowedTypes(types);

        var metadataFile = csvExportService.exportDocumentsToCSV(exportRequest);
        try {
            zipBuilder.copyFileToRoot(metadataFile.getInputStream(), "metadata.csv");
        } catch (IOException e) {
            throw new RuntimeException(e); // should never happen
        }
    }

    private List<Integer> getDocumentDatabaseIdsForBackup(Integer startYear, Integer endYear,
                                                          List<Integer> organisationUnitIds,
                                                          String type) {
        var query = NativeQuery.builder()
            .withQuery(q -> q.bool(b -> b
                .must(m -> m.terms(t -> t.field("organisation_unit_ids").terms(tf -> tf.value(
                    organisationUnitIds.stream()
                        .map(FieldValue::of)
                        .toList()
                ))))
                .must(m -> m.range(r -> r.field("year")
                    .gte(JsonData.of(startYear))
                    .lte(JsonData.of(endYear))))
                .must(m -> m.term(t -> t.field("type").value(type)))
            ))
            .withSourceFilter(new FetchSourceFilter(new String[] {"databaseId"}, null))
            .build();

        var hits = elasticsearchOperations.search(query, DocumentPublicationIndex.class);

        return hits.getSearchHits().stream()
            .map(hit -> hit.getContent().getDatabaseId())
            .toList();
    }

    private List<Document> fetchProcessableChunk(List<Integer> documentIds, Pageable pageable) {
        return documentRepository.findDocumentByIdIn(documentIds, pageable);
    }

    private void processDocument(Document document, List<DocumentFileSection> documentFileSections,
                                 BackupZipBuilder zipBuilder, String language)
        throws IOException {
        var author = document.getContributors().stream()
            .filter(contributor -> contributor.getContributionType()
                .equals(DocumentContributionType.AUTHOR))
            .min(Comparator.comparing(PersonContribution::getOrderNumber));

        if (author.isEmpty()) {
            log.warn("Author is missing for Document with ID {}", document.getId());
            return;
        }

        var documentDir = zipBuilder.createSubDir("documents/" +
            author.get().getAffiliationStatement().getDisplayPersonName().toString()
                .replace(" ", "_")
            + "_" + document.getId());

        for (var section : documentFileSections) {
            var fileItems = sectionAccessors.get(section).apply(document);
            if (Objects.isNull(fileItems) || fileItems.isEmpty()) {
                continue;
            }

            var sectionPaths = getSectionPaths(language);
            var sectionDir =
                zipBuilder.createSubDir(documentDir.resolve(sectionPaths.get(section)).toString());
            for (var fileItem : fileItems) {
                try (var file = fileService.loadAsResource(fileItem.getServerFilename())) {
                    zipBuilder.copyFile(file, sectionDir.resolve(fileItem.getFilename()));
                } catch (IOException e) {
                    log.warn("Failed to load file {}", fileItem.getServerFilename(), e);
                }
            }
        }
    }

    public Map<DocumentFileSection, String> getSectionPaths(String language) {
        return Map.of(
            DocumentFileSection.FILE_ITEMS, messageSource.getMessage(
                DocumentFileSection.FILE_ITEMS.getInternationalizationMessageName(),
                new Object[] {},
                Locale.forLanguageTag(language)),
            DocumentFileSection.PROOFS, messageSource.getMessage(
                DocumentFileSection.PROOFS.getInternationalizationMessageName(), new Object[] {},
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

    private String generateBackupFileName(Integer from, Integer to,
                                          OrganisationUnit institution, String lang) {
        return "DOCUMENT_BACKUP_" +
            StringUtil.getStringContent(institution.getName(), lang)
                .replace(" ", "_") + "_" + from + "_" + to + "_" + UUID.randomUUID() + ".zip";
    }
}
