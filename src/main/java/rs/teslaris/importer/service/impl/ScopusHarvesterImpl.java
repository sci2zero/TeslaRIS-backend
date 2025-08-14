package rs.teslaris.importer.service.impl;

import ai.djl.translate.TranslateException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.deduplication.DeduplicationUtil;
import rs.teslaris.core.util.exceptionhandling.exception.UserIsNotResearcherException;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.converter.harvest.ScopusConverter;
import rs.teslaris.importer.service.interfaces.OrganisationUnitImportSourceConfigurationService;
import rs.teslaris.importer.service.interfaces.ScopusHarvester;
import rs.teslaris.importer.utility.CommonHarvestUtility;
import rs.teslaris.importer.utility.CommonImportUtility;
import rs.teslaris.importer.utility.scopus.ScopusImportUtility;

@Slf4j
@Service
@RequiredArgsConstructor
@Traceable
public class ScopusHarvesterImpl implements ScopusHarvester {

    private final UserService userService;

    private final PersonService personService;

    private final OrganisationUnitService organisationUnitService;

    private final ScopusImportUtility scopusImportUtility;

    private final InvolvementService involvementService;

    private final MongoTemplate mongoTemplate;

    private final OrganisationUnitImportSourceConfigurationService
        organisationUnitImportSourceConfigurationService;


    @Override
    public HashMap<Integer, Integer> harvestDocumentsForAuthor(Integer userId, LocalDate startDate,
                                                               LocalDate endDate,
                                                               HashMap<Integer, Integer> newEntriesCount) {
        var startYear = startDate.getYear();
        var endYear = endDate.getYear();
        var personId = userService.getPersonIdForUser(userId);

        if (personId == -1) {
            throw new UserIsNotResearcherException("You are not a researcher.");
        }

        if (!organisationUnitImportSourceConfigurationService.readConfigurationForPerson(personId)
            .importScopus()) {
            return newEntriesCount;
        }

        var person = personService.readPersonWithBasicInfo(personId);
        var scopusId = person.getPersonalInfo().getScopusAuthorId();

        if (Objects.isNull(scopusId) || scopusId.isBlank()) {
            return newEntriesCount;
        }

        var employmentInstitutionIds =
            involvementService.getDirectEmploymentInstitutionIdsForPerson(personId);
        var adminUserIds = CommonImportUtility.getAdminUserIds();
        List<ScopusImportUtility.ScopusSearchResponse> yearlyResults;

        try {
            yearlyResults =
                scopusImportUtility.getDocumentsByIdentifier(List.of(scopusId), true, startYear,
                    endYear);
        } catch (Exception e) {
            log.warn("Exception occurred during Scopus harvest: {}", e.getMessage());
            return newEntriesCount;
        }

        performDocumentHarvest(yearlyResults, userId, false, newEntriesCount,
            employmentInstitutionIds, adminUserIds);

        return newEntriesCount;
    }

    @Override
    public HashMap<Integer, Integer> harvestDocumentsForInstitutionalEmployee(Integer userId,
                                                                              Integer institutionId,
                                                                              LocalDate startDate,
                                                                              LocalDate endDate,
                                                                              HashMap<Integer, Integer> newEntriesCount) {
        var startYear = startDate.getYear();
        var endYear = endDate.getYear();
        var organisationUnitId = Objects.nonNull(institutionId) ? institutionId :
            userService.getUserOrganisationUnitId(userId);

        if (!organisationUnitImportSourceConfigurationService.readConfigurationForInstitution(
            organisationUnitId).importScopus()) {
            return newEntriesCount;
        }

        var institution = organisationUnitService.findOne(organisationUnitId);
        var allInstitutionsThatCanImport =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);
        var adminUserIds = CommonImportUtility.getAdminUserIds();

        if (Objects.isNull(institution.getScopusAfid()) || institution.getScopusAfid().isBlank()) {
            return newEntriesCount;
        }

        var scopusAfid = institution.getScopusAfid();

        var yearlyResults =
            scopusImportUtility.getDocumentsByIdentifier(List.of(scopusAfid), false, startYear,
                endYear);

        performDocumentHarvest(yearlyResults, userId, true, newEntriesCount,
            allInstitutionsThatCanImport, adminUserIds);

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
        var startYear = startDate.getYear();
        var endYear = endDate.getYear();
        var organisationUnitId = Objects.nonNull(institutionId) ? institutionId :
            userService.getUserOrganisationUnitId(userId);

        if (!performImportForAllAuthors && authorIds.isEmpty()) {
            return newEntriesCount;
        }

        if (!organisationUnitImportSourceConfigurationService.readConfigurationForInstitution(
            organisationUnitId).importScopus()) {
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
                    .map(PersonIndex::getScopusAuthorId)
                    .filter(scopusAuthorId -> Objects.nonNull(scopusAuthorId) &&
                        !scopusAuthorId.isBlank())
                    .toList();
        } else {
            importAuthorIds =
                authorIds.stream()
                    .map(id -> personService.findOne(id).getScopusAuthorId())
                    .filter(scopusAuthorId -> Objects.nonNull(scopusAuthorId) &&
                        !scopusAuthorId.isBlank())
                    .toList();
        }

        var batchSize = 10;
        for (var i = 0; i < importAuthorIds.size(); i += batchSize) {
            var batch = importAuthorIds.subList(i, Math.min(i + batchSize, importAuthorIds.size()));

            var yearlyResults =
                scopusImportUtility.getDocumentsByIdentifier(batch, true, startYear, endYear);

            performDocumentHarvest(yearlyResults, userId, true, newEntriesCount,
                allInstitutionsThatCanImport, adminUserIds);
        }

        return newEntriesCount;
    }

    private void performDocumentHarvest(
        List<ScopusImportUtility.ScopusSearchResponse> yearlyResults,
        Integer userId, Boolean employeeUser,
        HashMap<Integer, Integer> newEntriesCount,
        List<Integer> institutionIds,
        Set<Integer> adminUserIds) {

        for (var yearlyResult : yearlyResults) {
            for (var entry : yearlyResult.searchResults().entries()) {
                if (Objects.isNull(entry.title())) {
                    continue;
                }

                var existingImport = findExistingImport(entry.identifier());
                var embedding = generateEmbedding(entry);
                if (DeduplicationUtil.isDuplicate(existingImport, embedding)) {
                    continue;
                }

                var optionalDocument =
                    ScopusConverter.toCommonImportModel(entry, scopusImportUtility);
                if (optionalDocument.isEmpty()) {
                    log.info("Harvested entry is retracted: {}", entry.title());
                    continue;
                }

                if (employeeUser) {
                    newEntriesCount.merge(userId, 1, Integer::sum);
                }

                var documentImport = optionalDocument.get();
                enrichDocumentImport(documentImport, entry.identifier(), embedding, userId,
                    adminUserIds, institutionIds);

                CommonHarvestUtility.updateContributorEntryCount(documentImport,
                    documentImport.getContributions().stream()
                        .map(c -> c.getPerson().getScopusAuthorId()).toList(), newEntriesCount,
                    personService);

                mongoTemplate.save(documentImport, "documentImports");
            }
        }
    }

    private DocumentImport findExistingImport(String identifier) {
        var query = new Query(Criteria.where("identifier").is(identifier));
        return mongoTemplate.findOne(query, DocumentImport.class, "documentImports");
    }

    private INDArray generateEmbedding(ScopusImportUtility.Entry entry) {
        try {
            var json = new ObjectMapper().writeValueAsString(entry);
            var flattened = DeduplicationUtil.flattenJson(json);
            return DeduplicationUtil.getEmbedding(flattened);
        } catch (JsonProcessingException | TranslateException e) {
            log.error("Error generating embedding: {}", e.getMessage());
            return null;
        }
    }

    private void enrichDocumentImport(DocumentImport doc,
                                      String identifier,
                                      INDArray embedding,
                                      Integer userId,
                                      Set<Integer> adminUserIds,
                                      List<Integer> institutionIds) {
        doc.setIdentifier(identifier);
        if (Objects.nonNull(embedding)) {
            doc.setEmbedding(embedding.toFloatVector());
        }
        doc.getImportUsersId().add(userId);
        doc.getImportUsersId().addAll(adminUserIds);
        doc.getImportInstitutionsId().addAll(institutionIds);
    }
}
