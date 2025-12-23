package rs.teslaris.exporter.service.interfaces;

import java.io.OutputStream;
import org.springframework.stereotype.Service;

@Service
public interface RoCrateExportService {

    void createRoCrateZip(Integer documentId, String exportId, OutputStream outputStream);

    void createRoCrateBibliographyZip(Integer personId, String exportId, OutputStream outputStream);
}
