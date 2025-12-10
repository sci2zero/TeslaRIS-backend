package rs.teslaris.exporter.service.interfaces;

import java.io.OutputStream;
import org.springframework.stereotype.Service;

@Service
public interface RoCrateExportService {

    void createRoCrateZip(Integer documentId, OutputStream outputStream);
}
