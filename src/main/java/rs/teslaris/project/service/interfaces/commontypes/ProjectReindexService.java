package rs.teslaris.project.service.interfaces.commontypes;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.applicationevent.ProjectEventReindexingEvent;
import rs.teslaris.core.indexmodel.EntityType;

@Service
public interface ProjectReindexService {

    void reindexDatabase(List<EntityType> indexesToRepopulate);

    void handleProjectEventReindexingEvent(ProjectEventReindexingEvent event);
}
