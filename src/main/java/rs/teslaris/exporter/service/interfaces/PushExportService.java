package rs.teslaris.exporter.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.exporter.model.push.PushEntityType;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;

@Service
public interface PushExportService {

    void pushEntities(PushTarget target, PushEntityType entityType, boolean force);
}
