package rs.teslaris.core.importer.service.interfaces;

import org.springframework.stereotype.Service;

@Service
public interface ScopusHarvester {

    Integer harvestDocumentsForAuthor(Integer userId, Integer startYear, Integer endYear);
}
