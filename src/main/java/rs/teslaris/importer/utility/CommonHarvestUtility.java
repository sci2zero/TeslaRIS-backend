package rs.teslaris.importer.utility;

import java.util.List;
import java.util.Map;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.importer.model.common.DocumentImport;

public class CommonHarvestUtility {

    public static void updateContributorEntryCount(DocumentImport doc,
                                                   List<String> contributorIdentifiers,
                                                   Map<Integer, Integer> newEntriesCount,
                                                   PersonService personService) {
        for (var identifier : contributorIdentifiers) {
            var userOpt = personService.findUserByIdentifier(identifier);
            userOpt.ifPresent(user -> {
                var contributorId = user.getId();
                doc.getImportUsersId().add(contributorId);
                newEntriesCount.merge(contributorId, 1, Integer::sum);
            });
        }
    }
}
