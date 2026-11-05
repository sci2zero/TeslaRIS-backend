package rs.teslaris.project.service.impl.identifier;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.identifier.EntityIdentifierConverter;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.service.impl.identifier.EntityIdentifierServiceImpl;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
import rs.teslaris.project.dto.identifier.ProjectIdentifierDTO;
import rs.teslaris.project.model.identifier.ProjectIdentifier;
import rs.teslaris.project.repository.identifier.ProjectIdentifierRepository;
import rs.teslaris.project.service.impl.identifier.cruddelegate.ProjectIdentifierJPAServiceImpl;
import rs.teslaris.project.service.interfaces.identifier.ProjectIdentifierService;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@Service
@Traceable
public class ProjectIdentifierServiceImpl extends EntityIdentifierServiceImpl
    implements ProjectIdentifierService {

    private final ProjectIdentifierRepository projectIdentifierRepository;

    private final ProjectIdentifierJPAServiceImpl projectIdentifierJPAService;

    private final ProjectService projectService;


    @Autowired
    public ProjectIdentifierServiceImpl(EntityIdentifierRepository entityIdentifierRepository,
                                        IdentifierService identifierService,
                                        ProjectIdentifierRepository projectIdentifierRepository,
                                        ProjectIdentifierJPAServiceImpl projectIdentifierJPAService,
                                        ProjectService projectService) {
        super(entityIdentifierRepository, identifierService);
        this.projectIdentifierRepository = projectIdentifierRepository;
        this.projectIdentifierJPAService = projectIdentifierJPAService;
        this.projectService = projectService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityIdentifierResponseDTO> getIdentifiersForProject(Integer projectId,
                                                                      AccessLevel accessLevel) {
        return projectIdentifierRepository.findIdentifiersForProjectAndIdentifierAccessLevel(
            projectId,
            accessLevel).stream().map(EntityIdentifierConverter::toDTO).toList();
    }

    @Override
    @Transactional
    public ProjectIdentifier createProjectIdentifier(ProjectIdentifierDTO projectIdentifierDTO) {
        var newProjectIdentifier = new ProjectIdentifier();

        setCommonFields(newProjectIdentifier, projectIdentifierDTO);

        newProjectIdentifier.setProject(
            projectService.findOne(projectIdentifierDTO.getProjectId()));

        return projectIdentifierJPAService.save(newProjectIdentifier);
    }

    @Override
    @Transactional
    public void updateProjectIdentifier(Integer projectIdentifierId,
                                        ProjectIdentifierDTO projectIdentifierDTO) {
        var projectIdentifierToUpdate = projectIdentifierJPAService.findOne(projectIdentifierId);

        setCommonFields(projectIdentifierToUpdate, projectIdentifierDTO);

        projectIdentifierToUpdate.setProject(
            projectService.findOne(projectIdentifierDTO.getProjectId()));

        projectIdentifierJPAService.save(projectIdentifierToUpdate);
    }
}
