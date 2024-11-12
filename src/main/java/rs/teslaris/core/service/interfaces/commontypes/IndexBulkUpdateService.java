package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.stereotype.Service;

@Service
public interface IndexBulkUpdateService {

    void removeIdFromRecord(String indexName, String fieldMappingName, Integer queryValue);

    void removeIdFromListField(String indexName, String fieldMappingName, Integer idToRemove);

    void removeIdFromListAndRelatedArrayField(String indexName, String fieldMappingName,
                                              String relatedArrayFieldName,
                                              String relatedSortFieldName,
                                              Integer idToRemove);
}
