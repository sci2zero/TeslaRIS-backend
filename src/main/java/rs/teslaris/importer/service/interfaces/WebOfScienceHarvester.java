package rs.teslaris.importer.service.interfaces;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import rs.teslaris.importer.model.common.DocumentImport;

public interface WebOfScienceHarvester {

    HashMap<Integer, Integer> harvestDocumentsForAuthor(Integer userId, LocalDate startDate,
                                                        LocalDate endDate,
                                                        HashMap<Integer, Integer> newEntriesCount);

    HashMap<Integer, Integer> harvestDocumentsForInstitutionalEmployee(Integer userId,
                                                                       Integer institutionId,
                                                                       LocalDate startDate,
                                                                       LocalDate endDate,
                                                                       HashMap<Integer, Integer> newEntriesCount);

    HashMap<Integer, Integer> harvestDocumentsForInstitution(Integer userId,
                                                             Integer institutionId,
                                                             LocalDate startDate,
                                                             LocalDate endDate,
                                                             List<Integer> authorIds,
                                                             boolean performImportForAllAuthors,
                                                             HashMap<Integer, Integer> newEntriesCount);

    Optional<DocumentImport> harvestDocumentForDoi(String doi);
}
