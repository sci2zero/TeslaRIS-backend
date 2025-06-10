package rs.teslaris.importer.service.interfaces;

import java.util.HashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface CSVHarvester {

    HashMap<Integer, Integer> harvestDocumentsForAuthor(Integer userId,
                                                        MultipartFile csvFile,
                                                        HashMap<Integer, Integer> newEntriesCount);
}
