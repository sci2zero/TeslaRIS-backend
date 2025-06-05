package rs.teslaris.core.service.interfaces.commontypes;

import org.springframework.stereotype.Service;
import rs.teslaris.core.util.Pair;

@Service
public interface NavigationBackwardCompatibilityService {

    Pair<String, Integer> readResourceByOldId(Integer oldId, String source, String language);

    Pair<String, String> readDocumentFileByOldId(String oldServerFilename, String source,
                                                 String language);
}
