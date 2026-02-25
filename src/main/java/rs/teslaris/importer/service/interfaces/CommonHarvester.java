package rs.teslaris.importer.service.interfaces;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface CommonHarvester {

    void performHarvestAsync(Integer userId, String userRole, LocalDate dateFrom, LocalDate dateTo,
                             Integer institutionId);

    Integer performHarvest(Integer userId, String userRole, LocalDate dateFrom, LocalDate dateTo,
                           Integer institutionId);

    void performAuthorCentricHarvestAsync(Integer userId, String userRole, LocalDate dateFrom,
                                          LocalDate dateTo, List<Integer> authorIds,
                                          Boolean allAuthors, Integer institutionId);

    Integer performAuthorCentricHarvest(Integer userId, String userRole, LocalDate dateFrom,
                                        LocalDate dateTo, List<Integer> authorIds,
                                        Boolean allAuthors, Integer institutionId);

    void processVerifiedFile(Integer userId, MultipartFile file, String filename,
                             HashMap<Integer, Integer> counts);

    void performDocumentCentricHarvest(Integer documentId);
}
