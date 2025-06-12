package rs.teslaris.importer.service.interfaces;

import java.time.LocalDate;
import java.util.HashMap;
import org.springframework.stereotype.Service;

@Service
public interface OpenAlexHarvester {

    HashMap<Integer, Integer> harvestDocumentsForAuthor(Integer userId, LocalDate startDate,
                                                        LocalDate endDate,
                                                        HashMap<Integer, Integer> newEntriesCount);

    HashMap<Integer, Integer> harvestDocumentsForInstitutionalEmployee(Integer userId,
                                                                       Integer institutionId,
                                                                       LocalDate startDate,
                                                                       LocalDate endDate,
                                                                       HashMap<Integer, Integer> newEntriesCount);
}
