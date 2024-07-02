package rs.teslaris.core.importer.service.impl;

import ai.djl.translate.TranslateException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import rs.teslaris.core.importer.model.common.DocumentImport;
import rs.teslaris.core.importer.model.converter.harvest.ScopusConverter;
import rs.teslaris.core.importer.service.interfaces.ScopusHarvester;
import rs.teslaris.core.importer.utility.scopus.ScopusImportUtility;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.deduplication.DeduplicationUtil;
import rs.teslaris.core.util.exceptionhandling.exception.ScopusIdMissingException;
import rs.teslaris.core.util.exceptionhandling.exception.UserIsNotResearcherException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScopusHarvesterImpl implements ScopusHarvester {

    private final UserService userService;

    private final PersonService personService;

    private final ScopusImportUtility scopusImportUtility;

    private final MongoTemplate mongoTemplate;


    @Override
    public HashMap<Integer, Integer> harvestDocumentsForAuthor(Integer userId, Integer startYear,
                                                               Integer endYear,
                                                               HashMap<Integer, Integer> newEntriesCount) {
        var personId = userService.getPersonIdForUser(userId);

        if (personId == -1) {
            throw new UserIsNotResearcherException("You are not a researcher.");
        }

        var person = personService.readPersonWithBasicInfo(personId);
        var scopusId = person.getPersonalInfo().getScopusAuthorId();

        if (Objects.isNull(scopusId)) {
            throw new ScopusIdMissingException("You have not set your Scopus ID.");
        }

        var yearlyResults = scopusImportUtility.getDocumentsByAuthor(scopusId, startYear, endYear);

        yearlyResults.forEach(
            yearlyResult -> yearlyResult.searchResults().entries().forEach(entry -> {
                if (Objects.isNull(entry.title())) {
                    return;
                }

                var query = new Query();
                query.addCriteria(Criteria.where("identifier").is(entry.identifier()));
                var documentImportBackup =
                    mongoTemplate.findOne(query, DocumentImport.class, "documentImports");

                INDArray importedDocumentEmbedding = null;
                try {
                    var flattenedDocument = DeduplicationUtil.flattenJson(
                        new ObjectMapper().writeValueAsString(entry));
                    importedDocumentEmbedding = DeduplicationUtil.getEmbedding(flattenedDocument);
                } catch (JsonProcessingException | TranslateException e) {
                    log.error(
                        "Unexpected error while calculating imported document's embedding. Exception: {}",
                        e.getMessage());
                }

                if (Objects.nonNull(documentImportBackup)) {
                    if (Objects.nonNull(importedDocumentEmbedding) &&
                        Objects.nonNull(documentImportBackup.getEmbedding())) {
                        var backupEmbedding = Nd4j.create(documentImportBackup.getEmbedding());
                        double similarity =
                            DeduplicationUtil.cosineSimilarity(importedDocumentEmbedding,
                                backupEmbedding);
                        if (similarity > DeduplicationUtil.MIN_SIMILARITY_THRESHOLD) {
                            return;
                        }
                    }
                }

                var documentImport =
                    ScopusConverter.toCommonImportModel(entry, scopusImportUtility);
                documentImport.setIdentifier(entry.identifier());

                if (Objects.nonNull(importedDocumentEmbedding)) {
                    documentImport.setEmbedding(importedDocumentEmbedding.toFloatVector());
                }

                documentImport.getImportUsersId().add(userId);

                documentImport.getContributions().forEach(personDocumentContribution -> {
                    var contributorUserOptional = personService.findUserByScopusAuthorId(
                        personDocumentContribution.getPerson().getScopusAuthorId());

                    if (contributorUserOptional.isEmpty()) {
                        return;
                    }

                    var contributorUserId = contributorUserOptional.get().getId();
                    documentImport.getImportUsersId().add(contributorUserId);
                    newEntriesCount.merge(contributorUserId, 1, Integer::sum);
                });

                mongoTemplate.save(documentImport, "documentImports");
            }));

        return newEntriesCount;
    }
}
