package rs.teslaris.importer.service.impl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.deduplication.DeduplicationUtil;
import rs.teslaris.importer.model.converter.harvest.OpenAlexConverter;
import rs.teslaris.importer.service.interfaces.OpenAlexHarvester;
import rs.teslaris.importer.utility.CommonImportUtility;
import rs.teslaris.importer.utility.DeepObjectMerger;
import rs.teslaris.importer.utility.openalex.OpenAlexImportUtility;

@Service
@RequiredArgsConstructor
public class OpenAlexHarvesterImpl implements OpenAlexHarvester {

    private final OpenAlexImportUtility openAlexImportUtility;

    private final MongoTemplate mongoTemplate;

    private final PersonService personService;

    private final UserService userService;

    private final OrganisationUnitService organisationUnitService;

    private final InvolvementService involvementService;


    @Override
    public HashMap<Integer, Integer> harvestDocumentsForAuthor(Integer userId, LocalDate startDate,
                                                               LocalDate endDate,
                                                               HashMap<Integer, Integer> newEntriesCount) {
        var personId = personService.getPersonIdForUserId(userId);
        var person = personService.findOne(personId);

        if (Objects.isNull(person.getOpenAlexId()) || person.getOpenAlexId().isBlank()) {
            return newEntriesCount;
        }

        var employmentInstitutionIds =
            involvementService.getDirectEmploymentInstitutionIdsForPerson(personId);
        var adminUserIds = CommonImportUtility.getAdminUserIds();

        var harvestedRecords =
            openAlexImportUtility.getPublicationsForAuthors(List.of(person.getOpenAlexId()),
                startDate.toString(),
                endDate.toString(), false);

        processHarvestedRecords(harvestedRecords, userId, adminUserIds, employmentInstitutionIds,
            newEntriesCount);

        return newEntriesCount;
    }

    @Override
    public HashMap<Integer, Integer> harvestDocumentsForInstitutionalEmployee(Integer userId,
                                                                              Integer institutionId,
                                                                              LocalDate startDate,
                                                                              LocalDate endDate,
                                                                              HashMap<Integer, Integer> newEntriesCount) {
        var organisationUnitId = Objects.nonNull(institutionId) ? institutionId :
            userService.getUserOrganisationUnitId(userId);
        var institution = organisationUnitService.findOne(organisationUnitId);
        var openAlexId = institution.getOpenAlexId();

        if (Objects.isNull(openAlexId) || openAlexId.isBlank()) {
            return newEntriesCount;
        }

        var allInstitutionsThatCanImport =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);
        var adminUserIds = CommonImportUtility.getAdminUserIds();
        var harvestedRecords =
            openAlexImportUtility.getPublicationsForAuthors(List.of(openAlexId),
                startDate.toString(),
                endDate.toString(), true);

        processHarvestedRecords(harvestedRecords, userId, adminUserIds,
            allInstitutionsThatCanImport,
            newEntriesCount);

        return newEntriesCount;
    }

    @Override
    public HashMap<Integer, Integer> harvestDocumentsForInstitution(Integer userId,
                                                                    Integer institutionId,
                                                                    LocalDate startDate,
                                                                    LocalDate endDate,
                                                                    List<Integer> authorIds,
                                                                    boolean performImportForAllAuthors,
                                                                    HashMap<Integer, Integer> newEntriesCount) {
        var organisationUnitId = Objects.nonNull(institutionId) ? institutionId :
            userService.getUserOrganisationUnitId(userId);

        if (!performImportForAllAuthors && authorIds.isEmpty()) {
            return newEntriesCount;
        }

        var allInstitutionsThatCanImport =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);
        var adminUserIds = CommonImportUtility.getAdminUserIds();

        List<String> importAuthorIds;
        if (performImportForAllAuthors) {
            importAuthorIds =
                personService.findPeopleForOrganisationUnit(organisationUnitId, Pageable.unpaged(),
                        false)
                    .map(PersonIndex::getOpenAlexId)
                    .stream()
                    .toList();
        } else {
            importAuthorIds =
                authorIds.stream()
                    .map(id -> personService.findOne(id).getOpenAlexId())
                    .toList();
        }

        var batchSize = 10;
        for (var i = 0; i < importAuthorIds.size(); i += batchSize) {
            var batch = importAuthorIds.subList(i, Math.min(i + batchSize, importAuthorIds.size()));

            var harvestedRecords =
                openAlexImportUtility.getPublicationsForAuthors(batch,
                    startDate.toString(), endDate.toString(), false);

            processHarvestedRecords(harvestedRecords, userId, adminUserIds,
                allInstitutionsThatCanImport, newEntriesCount);
        }

        return newEntriesCount;
    }

    private void processHarvestedRecords(
        List<OpenAlexImportUtility.OpenAlexPublication> harvestedRecords, Integer userId,
        Set<Integer> adminUserIds, List<Integer> institutionIds,
        HashMap<Integer, Integer> newEntriesCount) {
        harvestedRecords.forEach(
            publication -> OpenAlexConverter.toCommonImportModel(publication)
                .ifPresent(documentImport -> {
                    var existingImport =
                        CommonImportUtility.findExistingImport(documentImport.getIdentifier());
                    if (Objects.isNull(existingImport) &&
                        Objects.nonNull(documentImport.getDoi())) {
                        if (Objects.nonNull(
                            (existingImport =
                                CommonImportUtility.findImportByDOI(documentImport.getDoi())))) {
                            // Probably imported before from scopus, which has higher priority (for now)
                            // perform enrichment, if possible
                            DeepObjectMerger.deepMerge(existingImport, documentImport);
                            mongoTemplate.save(existingImport, "documentImports");
                            return;
                        }
                    }

                    var embedding = CommonImportUtility.generateEmbedding(documentImport);
                    if (DeduplicationUtil.isDuplicate(existingImport, embedding)) {
                        return;
                    }

                    if (Objects.nonNull(embedding)) {
                        documentImport.setEmbedding(embedding.toFloatVector());
                    }

                    documentImport.getImportUsersId().add(userId);
                    documentImport.getImportUsersId().addAll(adminUserIds);
                    documentImport.getImportInstitutionsId().addAll(institutionIds);
                    mongoTemplate.save(documentImport, "documentImports");
                    newEntriesCount.merge(userId, 1, Integer::sum);
                }));
    }
}
