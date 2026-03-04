package rs.teslaris.exporter.service.interfaces;

import java.nio.file.Path;
import org.springframework.stereotype.Service;

@Service
public interface RoCrateExportService {

    Path createRoCrateZip(Integer documentId, String exportId);

    Path createRoCrateBibliographyZip(Integer personId, String exportId);
}
