package rs.teslaris.importer.service.interfaces;

import java.util.HashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface BibTexHarvester {

    HashMap<Integer, Integer> harvestDocumentsForAuthor(Integer userId,
                                                        MultipartFile bibTexFile,
                                                        HashMap<Integer, Integer> newEntriesCount);
}
