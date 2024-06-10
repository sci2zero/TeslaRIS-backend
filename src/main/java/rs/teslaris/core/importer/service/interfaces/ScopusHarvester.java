package rs.teslaris.core.importer.service.interfaces;

import java.util.HashMap;
import org.springframework.stereotype.Service;

@Service
public interface ScopusHarvester {

    HashMap<Integer, Integer> harvestDocumentsForAuthor(Integer userId, Integer startYear,
                                                        Integer endYear,
                                                        HashMap<Integer, Integer> newEntriesCount);
}
