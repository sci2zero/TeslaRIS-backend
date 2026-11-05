package rs.teslaris.project.service.interfaces.identifier;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.project.dto.identifier.ProjectIdentifierDTO;
import rs.teslaris.project.model.identifier.ProjectIdentifier;

@Service
public interface ProjectIdentifierService {

    List<EntityIdentifierResponseDTO> getIdentifiersForProject(Integer projectId,
                                                               AccessLevel accessLevel);

    ProjectIdentifier createProjectIdentifier(ProjectIdentifierDTO projectIdentifierDTO);

    void updateProjectIdentifier(Integer projectIdentifierId,
                                 ProjectIdentifierDTO projectIdentifierDTO);
}
