package rs.teslaris.core.importer.service.impl;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
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
import rs.teslaris.core.util.exceptionhandling.exception.ScopusIdMissingException;
import rs.teslaris.core.util.exceptionhandling.exception.UserIsNotResearcherException;

@Service
@RequiredArgsConstructor
public class ScopusHarvesterImpl implements ScopusHarvester {

    private final UserService userService;

    private final PersonService personService;

    private final ScopusImportUtility scopusImportUtility;

    private final MongoTemplate mongoTemplate;


    @Override
    public Integer harvestDocumentsForAuthor(Integer userId, Integer startYear, Integer endYear) {
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

        var newEntriesCount = new AtomicInteger();
        yearlyResults.forEach(
            yearlyResult -> yearlyResult.searchResults().entries().forEach(entry -> {
                if (Objects.isNull(entry.title())) {
                    return;
                }

                var query = new Query();
                query.addCriteria(Criteria.where("scopus_id").is(entry.identifier()));
                boolean exists =
                    mongoTemplate.exists(query, DocumentImport.class, "documentImports");

                if (exists) {
                    return;
                }

                newEntriesCount.addAndGet(1);
                var documentImport = ScopusConverter.toCommonImportModel(entry);
                mongoTemplate.save(documentImport, "documentImports");
            }));

        return newEntriesCount.get();
    }
}
