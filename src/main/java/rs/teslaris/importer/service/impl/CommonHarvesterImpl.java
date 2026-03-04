package rs.teslaris.importer.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.util.deduplication.DeduplicationUtil;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.service.impl.worker.DocumentEnrichmentWorker;
import rs.teslaris.importer.service.interfaces.BibTexHarvester;
import rs.teslaris.importer.service.interfaces.CSVHarvester;
import rs.teslaris.importer.service.interfaces.CommonHarvester;
import rs.teslaris.importer.service.interfaces.EndNoteHarvester;
import rs.teslaris.importer.service.interfaces.LoadingConfigurationService;
import rs.teslaris.importer.service.interfaces.OpenAlexHarvester;
import rs.teslaris.importer.service.interfaces.RefManHarvester;
import rs.teslaris.importer.service.interfaces.ScopusHarvester;
import rs.teslaris.importer.service.interfaces.WebOfScienceHarvester;
import rs.teslaris.importer.utility.CommonImportUtility;
import rs.teslaris.importer.utility.DeepObjectMerger;

@Service
@RequiredArgsConstructor
public class CommonHarvesterImpl implements CommonHarvester {

    private final ScopusHarvester scopusHarvester;

    private final OpenAlexHarvester openAlexHarvester;

    private final WebOfScienceHarvester webOfScienceHarvester;

    private final BibTexHarvester bibTexHarvester;

    private final RefManHarvester refManHarvester;

    private final EndNoteHarvester endNoteHarvester;

    private final CSVHarvester csvHarvester;

    private final NotificationService notificationService;

    private final UserRepository userRepository;

    private final LoadingConfigurationService loadingConfigurationService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final MongoTemplate mongoTemplate;

    private final DocumentEnrichmentWorker documentEnrichmentWorker;

    private final TaskManagerService taskManagerService;

    @Qualifier("metadataFetchExecutor")
    private final Executor metadataFetchExecutor;


    @Override
    @Async("taskExecutor")
    public void performHarvestAsync(Integer userId, String userRole, LocalDate dateFrom,
                                    LocalDate dateTo, Integer institutionId) {
        performHarvest(userId, userRole, dateFrom, dateTo, institutionId);
    }

    @Override
    public Integer performHarvest(Integer userId, String userRole, LocalDate dateFrom,
                                  LocalDate dateTo, Integer institutionId) {
        Map<Integer, Integer> newDocumentImportCountByUser = new HashMap<>();

        if (userRole.equals(UserRole.RESEARCHER.name())) {
            scopusHarvester.harvestDocumentsForAuthor(userId, dateFrom, dateTo, new HashMap<>())
                .forEach((key, value) ->
                    newDocumentImportCountByUser.merge(key, value, Integer::sum)
                );
            openAlexHarvester.harvestDocumentsForAuthor(userId, dateFrom, dateTo, new HashMap<>())
                .forEach((key, value) ->
                    newDocumentImportCountByUser.merge(key, value, Integer::sum)
                );
            webOfScienceHarvester.harvestDocumentsForAuthor(userId, dateFrom, dateTo,
                    new HashMap<>())
                .forEach((key, value) ->
                    newDocumentImportCountByUser.merge(key, value, Integer::sum)
                );
        } else if (userRole.equals(UserRole.INSTITUTIONAL_EDITOR.name())) {
            scopusHarvester.harvestDocumentsForInstitutionalEmployee(userId, null, dateFrom,
                dateTo,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            openAlexHarvester.harvestDocumentsForInstitutionalEmployee(userId, null, dateFrom,
                dateTo,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            webOfScienceHarvester.harvestDocumentsForInstitutionalEmployee(userId, null, dateFrom,
                dateTo,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
        } else if (userRole.equals(UserRole.ADMIN.name())) {
            scopusHarvester.harvestDocumentsForInstitutionalEmployee(userId, institutionId,
                dateFrom, dateTo,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            openAlexHarvester.harvestDocumentsForInstitutionalEmployee(userId, institutionId,
                dateFrom, dateTo,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            webOfScienceHarvester.harvestDocumentsForInstitutionalEmployee(userId, institutionId,
                dateFrom, dateTo,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
        } else {
            return 0;
        }

        dispatchNotifications(newDocumentImportCountByUser, userId);
        return newDocumentImportCountByUser.getOrDefault(userId, 0);
    }

    @Override
    @Async("taskExecutor")
    public void performAuthorCentricHarvestAsync(Integer userId, String userRole,
                                                 LocalDate dateFrom, LocalDate dateTo,
                                                 List<Integer> authorIds, Boolean allAuthors,
                                                 Integer institutionId) {
        performAuthorCentricHarvest(userId, userRole, dateFrom, dateTo, authorIds, allAuthors,
            institutionId);
    }

    @Override
    public Integer performAuthorCentricHarvest(Integer userId, String userRole, LocalDate dateFrom,
                                               LocalDate dateTo, List<Integer> authorIds,
                                               Boolean allAuthors, Integer institutionId) {
        Map<Integer, Integer> newDocumentImportCountByUser = new HashMap<>();
        if (userRole.equals(UserRole.INSTITUTIONAL_EDITOR.name())) {
            scopusHarvester.harvestDocumentsForInstitution(userId, null, dateFrom,
                dateTo, authorIds, allAuthors,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            openAlexHarvester.harvestDocumentsForInstitution(userId, null, dateFrom,
                dateTo, authorIds, allAuthors,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            webOfScienceHarvester.harvestDocumentsForInstitution(userId, null, dateFrom,
                dateTo, authorIds, allAuthors,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
        } else if (userRole.equals(UserRole.ADMIN.name())) {
            scopusHarvester.harvestDocumentsForInstitution(userId, institutionId,
                dateFrom, dateTo, authorIds, allAuthors,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            openAlexHarvester.harvestDocumentsForInstitution(userId, institutionId,
                dateFrom, dateTo, authorIds, allAuthors,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            webOfScienceHarvester.harvestDocumentsForInstitution(userId, institutionId,
                dateFrom, dateTo, authorIds, allAuthors,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
        } else {
            return 0;
        }

        dispatchNotifications(newDocumentImportCountByUser, userId);
        return newDocumentImportCountByUser.getOrDefault(userId, 0);
    }

    public void processVerifiedFile(Integer userId, MultipartFile file, String filename,
                                    HashMap<Integer, Integer> counts) {
        if (filename.endsWith(".bib")) {
            bibTexHarvester.harvestDocumentsForAuthor(userId, file, counts);
        } else if (filename.endsWith(".ris")) {
            refManHarvester.harvestDocumentsForAuthor(userId, file, counts);
        } else if (filename.endsWith(".enw")) {
            endNoteHarvester.harvestDocumentsForAuthor(userId, file, counts);
        } else if (filename.endsWith(".csv")) {
            csvHarvester.harvestDocumentsForAuthor(userId, file, counts);
        }
    }

    @Override
    public String performDocumentCentricHarvest(Integer documentId) {
        var documentIndex =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId);

        if (documentIndex.isEmpty() || !StringUtil.valueExists(documentIndex.get().getDoi())) {
            return null;
        }

        return scanSourcesForDocumentMetadata(documentIndex.get(), false);
    }

    @Override
    public void scheduleMetadataEnrichmentForInstitution(LocalDateTime timeToRun,
                                                         List<Integer> institutionIds,
                                                         boolean autoload,
                                                         RecurrenceType recurrenceType,
                                                         Integer userId) {
        var taskId = taskManagerService.scheduleTask(
            "Enrichment-" +
                institutionIds.stream().map(String::valueOf).collect(Collectors.joining("_")) +
                "-" + (autoload ? "AUTO" : "UI"),
            timeToRun,
            () -> enrichMetadataForInstitution(institutionIds, autoload),
            userId, recurrenceType);

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timeToRun,
                ScheduledTaskType.METADATA_ENRICHMENT, new HashMap<>() {{
                put("institutionIds", institutionIds);
                put("autoload", autoload);
                put("userId", userId);
            }}, recurrenceType));
    }

    @Override
    @Async
    public void enrichMetadataForInstitution(List<Integer> institutionIds, boolean autoupdate) {
        var typesToFetch =
            autoupdate ? Arrays.stream(DocumentPublicationType.values()).map(Enum::name).toList() :
                List.of(DocumentPublicationType.JOURNAL_PUBLICATION.name(),
                    DocumentPublicationType.PROCEEDINGS_PUBLICATION.name());

        FunctionalUtil.performBulkOperation(
            (pageRequest -> documentPublicationIndexRepository.fetchForInstitutionsAndTypes(
                institutionIds,
                typesToFetch, pageRequest)),
            Sort.by(Sort.Direction.ASC, "databaseId"),
            (documentIndex) -> scanSourcesForDocumentMetadata(documentIndex, autoupdate)
        );

        institutionIds.forEach(
            institutionId -> {
                var loadingConfiguration =
                    loadingConfigurationService.getLoadingConfigurationForInstitution(
                        institutionId);
                loadingConfiguration.setPriorityLoading(true);

                loadingConfigurationService.saveLoadingConfiguration(institutionId,
                    loadingConfiguration);
            });
    }

    private String scanSourcesForDocumentMetadata(DocumentPublicationIndex documentIndex,
                                                  boolean autoupdate) {
        var doi = documentIndex.getDoi();
        if (!StringUtil.valueExists(doi)) {
            return null;
        }

        CompletableFuture<Optional<DocumentImport>> scopusFuture =
            CompletableFuture.supplyAsync(
                () -> scopusHarvester.harvestDocumentForDoi(doi, !autoupdate),
                metadataFetchExecutor
            );

        CompletableFuture<Optional<DocumentImport>> openAlexFuture =
            CompletableFuture.supplyAsync(
                () -> openAlexHarvester.harvestDocumentForDoi(doi, !autoupdate),
                metadataFetchExecutor
            );

        CompletableFuture<Optional<DocumentImport>> wosFuture =
            CompletableFuture.supplyAsync(
                () -> webOfScienceHarvester.harvestDocumentForDoi(doi, !autoupdate),
                metadataFetchExecutor
            );

        CompletableFuture.allOf(scopusFuture, openAlexFuture, wosFuture).join();

        var mergedMetadata = new DocumentImport();

        mergeIfPresent(scopusFuture.join(), mergedMetadata);
        mergeIfPresent(openAlexFuture.join(), mergedMetadata);
        mergeIfPresent(wosFuture.join(), mergedMetadata);

        if (!StringUtil.valueExists(mergedMetadata.getDoi())) {
            return null;
        }

        if (autoupdate) {
            documentEnrichmentWorker.enrichDocumentMetadata(
                documentIndex.getDatabaseId(),
                mergedMetadata
            );
            return null;
        }

        return handleManualImport(documentIndex, mergedMetadata);
    }

    private void mergeIfPresent(Optional<DocumentImport> optional,
                                DocumentImport target) {
        optional.ifPresent(source ->
            DeepObjectMerger.deepMerge(target, source));
    }

    private String handleManualImport(DocumentPublicationIndex documentIndex,
                                      DocumentImport mergedMetadata) {
        var embedding = CommonImportUtility.generateEmbedding(mergedMetadata);
        if (Objects.nonNull(embedding)) {
            mergedMetadata.setEmbedding(DeduplicationUtil.toDoubleList(embedding));
        }

        var existingImport = CommonImportUtility.findImportByDOIOrMetadata(mergedMetadata);
        if (Objects.nonNull(existingImport)) {
            if (DeduplicationUtil.isDuplicate(existingImport, embedding, mergedMetadata) &&
                existingImport.getLoaded()) {
                return null;
            } else {
                DeepObjectMerger.deepMerge(existingImport, mergedMetadata);
                existingImport.setLoaded(false);
            }

            existingImport.setSource("ENRICHMENT");
            mongoTemplate.save(existingImport, "documentImports");
            return existingImport.getId();
        }

        documentIndex.getAuthorIds().forEach(authorId ->
            userRepository.findForResearcher(authorId)
                .ifPresent(user -> mergedMetadata.getImportUsersId().add(user.getId())));

        documentIndex.getOrganisationUnitIdsActive()
            .forEach(mergedMetadata.getImportInstitutionsId()::add);

        mergedMetadata.getImportUsersId().addAll(CommonImportUtility.getAdminUserIds());

        mergedMetadata.setSource("ENRICHMENT");
        return mongoTemplate.save(mergedMetadata, "documentImports").getId();
    }

    private void dispatchNotifications(Map<Integer, Integer> newDocumentImportCountByUser,
                                       Integer userId) {
        if (newDocumentImportCountByUser.isEmpty()) {
            newDocumentImportCountByUser.put(userId, 0);
        }

        newDocumentImportCountByUser.keySet().forEach(key ->
            userRepository.findById(key).ifPresent(user -> {
                var notificationValues = new HashMap<String, String>();
                notificationValues.put("newImportCount",
                    String.valueOf(newDocumentImportCountByUser.get(key)));
                notificationService.createNotification(
                    NotificationFactory.contructNewImportsNotification(notificationValues, user));
            })
        );
    }
}
