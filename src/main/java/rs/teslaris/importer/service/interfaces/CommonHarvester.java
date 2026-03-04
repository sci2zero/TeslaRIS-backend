package rs.teslaris.importer.service.interfaces;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.model.commontypes.RecurrenceType;

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

    String performDocumentCentricHarvest(Integer documentId);

    void scheduleMetadataEnrichmentForInstitution(LocalDateTime timeToRun,
                                                  List<Integer> institutionIds, boolean autoload,
                                                  RecurrenceType recurrenceType, Integer userId);

    void enrichMetadataForInstitution(List<Integer> institutionIds, boolean autoupdate);
}
