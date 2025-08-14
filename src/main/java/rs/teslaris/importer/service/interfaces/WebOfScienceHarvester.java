package rs.teslaris.importer.service.interfaces;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

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
}
