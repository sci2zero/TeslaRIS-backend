package rs.teslaris.importer.service.impl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.deduplication.DeduplicationUtil;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.session.SessionUtil;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.service.interfaces.BibTexHarvester;
import rs.teslaris.importer.service.interfaces.CSVHarvester;
import rs.teslaris.importer.service.interfaces.CommonHarvester;
import rs.teslaris.importer.service.interfaces.EndNoteHarvester;
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

    private final UserService userService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final MongoTemplate mongoTemplate;


    @Override
    @Async
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
    @Async
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
    public void performDocumentCentricHarvest(Integer documentId) {
        var mergedMetadata = new DocumentImport();

        var documentIndex =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId);

        if (documentIndex.isEmpty() || !StringUtil.valueExists(documentIndex.get().getDoi())) {
            return;
        }

        var doi = documentIndex.get().getDoi();

        var scopusHarvestedData = scopusHarvester.harvestDocumentForDoi(doi);
        scopusHarvestedData.ifPresent(
            documentImport ->
                DeepObjectMerger.deepMerge(mergedMetadata, documentImport));

        var openAlexHarvestedData = openAlexHarvester.harvestDocumentForDoi(doi);
        openAlexHarvestedData.ifPresent(
            documentImport ->
                DeepObjectMerger.deepMerge(mergedMetadata, documentImport));

        var webOfScienceHarvestedData = webOfScienceHarvester.harvestDocumentForDoi(doi);
        webOfScienceHarvestedData.ifPresent(
            documentImport ->
                DeepObjectMerger.deepMerge(mergedMetadata, documentImport));

        if (!StringUtil.valueExists(mergedMetadata.getDoi())) {
            return; // nothing was found
        }

        var existingImport =
            CommonImportUtility.findImportByDOIOrMetadata(mergedMetadata);
        if (Objects.nonNull(existingImport)) {
            DeepObjectMerger.deepMerge(existingImport, mergedMetadata);
            mongoTemplate.save(existingImport, "documentImports");
            return;
        }

        var embedding = CommonImportUtility.generateEmbedding(mergedMetadata);
        if (Objects.nonNull(embedding)) {
            mergedMetadata.setEmbedding(DeduplicationUtil.toDoubleList(embedding));
        }

//        var personId = personService.getPersonIdForUserId(userId);
//
//        var employmentInstitutionIds =
//            involvementService.getDirectEmploymentInstitutionIdsForPerson(personId);
        var adminUserIds = CommonImportUtility.getAdminUserIds();

        mergedMetadata.getImportUsersId().add(SessionUtil.getLoggedInUser().getId());
        mergedMetadata.getImportUsersId().addAll(adminUserIds);
//        mergedMetadata.getImportInstitutionsId().addAll(employmentInstitutionIds);
        mongoTemplate.save(mergedMetadata, "documentImports");
    }

    private void dispatchNotifications(Map<Integer, Integer> newDocumentImportCountByUser,
                                       Integer userId) {
        if (newDocumentImportCountByUser.isEmpty()) {
            newDocumentImportCountByUser.put(userId, 0);
        }

        newDocumentImportCountByUser.keySet().forEach(key -> {
            var notificationValues = new HashMap<String, String>();
            notificationValues.put("newImportCount",
                String.valueOf(newDocumentImportCountByUser.get(key)));
            notificationService.createNotification(
                NotificationFactory.contructNewImportsNotification(notificationValues,
                    userService.findOne(key)));
        });
    }
}
