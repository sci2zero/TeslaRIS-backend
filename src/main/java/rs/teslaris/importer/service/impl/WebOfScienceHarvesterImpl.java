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
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.deduplication.DeduplicationUtil;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.importer.model.converter.harvest.WebOfScienceConverter;
import rs.teslaris.importer.service.interfaces.OrganisationUnitImportSourceConfigurationService;
import rs.teslaris.importer.service.interfaces.WebOfScienceHarvester;
import rs.teslaris.importer.utility.CommonHarvestUtility;
import rs.teslaris.importer.utility.CommonImportUtility;
import rs.teslaris.importer.utility.DeepObjectMerger;
import rs.teslaris.importer.utility.webofscience.WebOfScienceImportUtility;

@Service
@RequiredArgsConstructor
public class WebOfScienceHarvesterImpl implements WebOfScienceHarvester {

    private final WebOfScienceImportUtility webOfScienceImportUtility;

    private final MongoTemplate mongoTemplate;

    private final PersonService personService;

    private final UserService userService;

    private final OrganisationUnitService organisationUnitService;

    private final InvolvementService involvementService;

    private final OrganisationUnitImportSourceConfigurationService
        organisationUnitImportSourceConfigurationService;


    @Override
    public HashMap<Integer, Integer> harvestDocumentsForAuthor(Integer userId, LocalDate startDate,
                                                               LocalDate endDate,
                                                               HashMap<Integer, Integer> newEntriesCount) {
        var personId = personService.getPersonIdForUserId(userId);

        if (!organisationUnitImportSourceConfigurationService.readConfigurationForPerson(personId)
            .importWebOfScience()) {
            return newEntriesCount;
        }

        var person = personService.findOne(personId);
        if (Objects.isNull(person.getWebOfScienceResearcherId()) ||
            person.getWebOfScienceResearcherId().isBlank()) {
            return newEntriesCount;
        }

        var employmentInstitutionIds =
            involvementService.getDirectEmploymentInstitutionIdsForPerson(personId);
        var adminUserIds = CommonImportUtility.getAdminUserIds();

        var harvestedRecords =
            webOfScienceImportUtility.getPublicationsForAuthors(
                List.of(person.getWebOfScienceResearcherId()),
                startDate.toString(), endDate.toString());

        processHarvestedRecords(harvestedRecords, userId, adminUserIds, employmentInstitutionIds,
            newEntriesCount, false);

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

        if (!organisationUnitImportSourceConfigurationService.readConfigurationForInstitution(
            organisationUnitId).importWebOfScience()) {
            return newEntriesCount;
        }

        var institution = organisationUnitService.findOne(organisationUnitId);
        var institutionName = institution.getName().stream()
            .filter(mc -> mc.getLanguage().getLanguageTag().equals(LanguageAbbreviations.ENGLISH))
            .findFirst();

        // If no english name variant is specified, pick any other
        if (institutionName.isEmpty()) {
            institutionName = institution.getName().stream().findFirst();
        }

        if (institutionName.isEmpty()) {
            return newEntriesCount;
        }

        var allInstitutionsThatCanImport =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);
        var adminUserIds = CommonImportUtility.getAdminUserIds();
        var harvestedRecords = webOfScienceImportUtility.getPublicationsForInstitution(
            institutionName.get().getContent(), startDate.toString(), endDate.toString());

        processHarvestedRecords(harvestedRecords, userId, adminUserIds,
            allInstitutionsThatCanImport, newEntriesCount, true);

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

        if (!organisationUnitImportSourceConfigurationService.readConfigurationForInstitution(
            organisationUnitId).importWebOfScience()) {
            return newEntriesCount;
        }

        var allInstitutionsThatCanImport =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);
        var adminUserIds = CommonImportUtility.getAdminUserIds();

        List<String> importAuthorIds;
        if (performImportForAllAuthors) {
            importAuthorIds =
                personService.findPeopleForOrganisationUnit(organisationUnitId, List.of("*"),
                        Pageable.unpaged(), false)
                    .map(PersonIndex::getWebOfScienceResearcherId)
                    .filter(wosId -> Objects.nonNull(wosId) && !wosId.isBlank())
                    .toList();
        } else {
            importAuthorIds =
                authorIds.stream()
                    .map(id -> personService.findOne(id).getWebOfScienceResearcherId())
                    .filter(wosId -> Objects.nonNull(wosId) && !wosId.isBlank())
                    .toList();
        }

        var batchSize = 10;
        for (var i = 0; i < importAuthorIds.size(); i += batchSize) {
            var batch = importAuthorIds.subList(i, Math.min(i + batchSize, importAuthorIds.size()));

            var harvestedRecords =
                webOfScienceImportUtility.getPublicationsForAuthors(batch, startDate.toString(),
                    endDate.toString());

            processHarvestedRecords(harvestedRecords, userId, adminUserIds,
                allInstitutionsThatCanImport, newEntriesCount, true);
        }

        return newEntriesCount;
    }

    private void processHarvestedRecords(
        List<WebOfScienceImportUtility.WosPublication> harvestedRecords, Integer userId,
        Set<Integer> adminUserIds, List<Integer> institutionIds,
        HashMap<Integer, Integer> newEntriesCount, Boolean employeeUser) {
        harvestedRecords.forEach(
            publication -> WebOfScienceConverter.toCommonImportModel(publication)
                .ifPresent(documentImport -> {
                    var existingImport =
                        CommonImportUtility.findExistingImport(documentImport.getIdentifier());
                    if (Objects.isNull(existingImport)) {
                        if (Objects.nonNull(
                            (existingImport =
                                CommonImportUtility.findImportByDOIOrMetadata(documentImport)))) {
                            // Probably imported before from Scopus/OpenAlex, which have higher priorities
                            // perform metadata enrichment, if possible
                            DeepObjectMerger.deepMerge(existingImport, documentImport);
                            mongoTemplate.save(existingImport, "documentImports");
                            return;
                        }
                    }

                    var embedding = CommonImportUtility.generateEmbedding(documentImport);
                    if (DeduplicationUtil.isDuplicate(existingImport, embedding, documentImport)) {
                        return;
                    }

                    if (Objects.nonNull(embedding)) {
                        documentImport.setEmbedding(DeduplicationUtil.toDoubleList(embedding));
                    }

                    documentImport.getImportUsersId().add(userId);
                    documentImport.getImportUsersId().addAll(adminUserIds);
                    documentImport.getImportInstitutionsId().addAll(institutionIds);

                    if (employeeUser) {
                        newEntriesCount.merge(userId, 1, Integer::sum);
                    }

                    CommonHarvestUtility.updateContributorEntryCount(documentImport,
                        documentImport.getContributions().stream()
                            .map(c -> c.getPerson().getWebOfScienceResearcherId()).toList(),
                        newEntriesCount,
                        personService);

                    mongoTemplate.save(documentImport, "documentImports");
                }));
    }
}
