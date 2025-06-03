package rs.teslaris.importer.service.impl;

import ai.djl.translate.TranslateException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.deduplication.DeduplicationUtil;
import rs.teslaris.core.util.exceptionhandling.exception.ScopusIdMissingException;
import rs.teslaris.core.util.exceptionhandling.exception.UserIsNotResearcherException;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.converter.harvest.ScopusConverter;
import rs.teslaris.importer.service.interfaces.ScopusHarvester;
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


    @Override
    public HashMap<Integer, Integer> harvestDocumentsForAuthor(Integer userId, Integer startYear,
                                                               Integer endYear,
                                                               HashMap<Integer, Integer> newEntriesCount) {
        var personId = userService.getPersonIdForUser(userId);

        if (personId == -1) {
            throw new UserIsNotResearcherException("You are not a researcher.");
        }

        if (!personService.canPersonScanDataSources(personId)) {
            throw new ScopusIdMissingException("You have not set your Scopus ID.");
        }

        var person = personService.readPersonWithBasicInfo(personId);
        var employmentInstitutionIds =
            involvementService.getDirectEmploymentInstitutionIdsForPerson(personId);
        var scopusId = person.getPersonalInfo().getScopusAuthorId();

        var yearlyResults =
            scopusImportUtility.getDocumentsByIdentifier(scopusId, true, startYear, endYear);

        performDocumentHarvest(yearlyResults, userId, false, newEntriesCount,
            employmentInstitutionIds);

        return newEntriesCount;
    }

    @Override
    public HashMap<Integer, Integer> harvestDocumentsForInstitutionalEmployee(Integer userId,
                                                                              Integer institutionId,
                                                                              Integer startYear,
                                                                              Integer endYear,
                                                                              HashMap<Integer, Integer> newEntriesCount) {
        var organisationUnitId = Objects.nonNull(institutionId) ? institutionId :
            userService.getUserOrganisationUnitId(userId);
        var institution = organisationUnitService.findOne(organisationUnitId);
        var allInstitutionsThatCanImport =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);

        if (Objects.isNull(institution.getScopusAfid())) {
            throw new ScopusIdMissingException("You have not set your institution Scopus AFID.");
        }

        var scopusAfid = institution.getScopusAfid();

        var yearlyResults =
            scopusImportUtility.getDocumentsByIdentifier(scopusAfid, false, startYear, endYear);

        performDocumentHarvest(yearlyResults, userId, true, newEntriesCount,
            allInstitutionsThatCanImport);

        return newEntriesCount;
    }

    private void performDocumentHarvest(
        List<ScopusImportUtility.ScopusSearchResponse> yearlyResults,
        Integer userId, Boolean employeeUser,
        HashMap<Integer, Integer> newEntriesCount,
        List<Integer> institutionIds) {

        var adminUserIds = getAdminUserIds();

        for (var yearlyResult : yearlyResults) {
            for (var entry : yearlyResult.searchResults().entries()) {
                if (Objects.isNull(entry.title())) {
                    continue;
                }

                var existingImport = findExistingImport(entry.identifier());
                var embedding = generateEmbedding(entry);
                if (isDuplicate(existingImport, embedding)) {
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
                updateContributors(documentImport, newEntriesCount);
                mongoTemplate.save(documentImport, "documentImports");
            }
        }
    }

    private Set<Integer> getAdminUserIds() {
        return userService.findAllSystemAdminUsers().stream()
            .map(BaseEntity::getId)
            .collect(Collectors.toSet());
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

    private boolean isDuplicate(DocumentImport backup, INDArray newEmbedding) {
        if (Objects.isNull(backup) || Objects.isNull(newEmbedding) ||
            Objects.isNull(backup.getEmbedding())) {
            return false;
        }

        var oldEmbedding = Nd4j.create(backup.getEmbedding());
        var similarity = DeduplicationUtil.cosineSimilarity(newEmbedding, oldEmbedding);
        return similarity > DeduplicationUtil.MIN_SIMILARITY_THRESHOLD;
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

    private void updateContributors(DocumentImport doc, Map<Integer, Integer> newEntriesCount) {
        for (var contribution : doc.getContributions()) {
            var scopusId = contribution.getPerson().getScopusAuthorId();
            var userOpt = personService.findUserByScopusAuthorId(scopusId);
            userOpt.ifPresent(user -> {
                var contributorId = user.getId();
                doc.getImportUsersId().add(contributorId);
                newEntriesCount.merge(contributorId, 1, Integer::sum);
            });
        }
    }
}
