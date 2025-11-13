package rs.teslaris.importer.service.interfaces;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface SKGIFHarvester {

    void harvest(String sourceName, String entityId, LocalDate startDate, LocalDate endDate,
                 Integer userId);

    List<String> getSources();
}
