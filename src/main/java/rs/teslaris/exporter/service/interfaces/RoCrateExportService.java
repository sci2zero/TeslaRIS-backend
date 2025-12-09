package rs.teslaris.exporter.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.rocrate.RoCrate;

@Service
public interface RoCrateExportService {

    RoCrate createMetadataInfo(Integer documentId);
}
