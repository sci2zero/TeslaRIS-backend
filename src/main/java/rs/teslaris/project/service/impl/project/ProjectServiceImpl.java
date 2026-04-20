package rs.teslaris.project.service.impl.project;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.util.exceptionhandling.exception.DateRangeException;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.project.converter.project.ProjectConverter;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.indexmodel.project.ProjectIndex;
import rs.teslaris.project.indexrepository.project.ProjectIndexRepository;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.repository.project.ProjectRepository;
import rs.teslaris.project.service.interfaces.project.ProjectService;

import java.util.HashSet;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends JPAServiceImpl<Project> implements ProjectService {

    private final ProjectRepository projectRepository;

    private final MultilingualContentService multilingualContentService;

    private final ResearchAreaService researchAreaService;

    private final CurrencyService currencyService;

    private final ProjectIndexRepository projectIndexRepository;

    @Override
    protected JpaRepository<Project, Integer> getEntityRepository() {
        return projectRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDTO readProject(Integer projectId) {
        return ProjectConverter.toDTO(findOne(projectId));
    }

    @Override
    public Project createProject(ProjectDTO projectDTO) {
        var newProject = new Project();

        setCommonFields(newProject, projectDTO);

        var savedProject = save(newProject);

        projectIndexRepository.save(
                indexCommonFields(savedProject, new ProjectIndex()));

        return savedProject;
    }

    @Override
    @Transactional
    public void updateProject(Integer projectId,
                                  ProjectDTO projectDTO) {
        var projectToUpdate = findOne(projectId);

        clearCommonFields(projectToUpdate);
        setCommonFields(projectToUpdate, projectDTO);

        projectIndexRepository.findProjectIndexByDatabaseId(projectId)
                .ifPresent(index -> {
                    indexCommonFields(projectToUpdate, index);
                    projectIndexRepository.save(index);
                });
    }

    @Override
    @Transactional
    public void deleteProject(Integer projectId) {
        delete(projectId);
    }

    private void setCommonFields(Project project, ProjectDTO projectDTO) {
        if (Objects.nonNull(projectDTO.getDateFrom()) &&
                Objects.nonNull(projectDTO.getDateTo()) &&
                projectDTO.getDateTo().isBefore(projectDTO.getDateFrom())) {
            throw new DateRangeException(
                    "Project must start before it ends.");
        }

        project.setName(
                multilingualContentService.getMultilingualContent(projectDTO.getName()));
        project.setDescription(
                multilingualContentService.getMultilingualContent(projectDTO.getDescription()));
        project.setNameAbbreviation(
                multilingualContentService.getMultilingualContent(projectDTO.getNameAbbreviation()));
        project.setKeywords(
                multilingualContentService.getMultilingualContent(projectDTO.getKeywords()));

        var researchAreas = researchAreaService.getResearchAreasByIds(
                projectDTO.getResearchAreasId().stream().toList());
        project.setResearchAreas(new HashSet<>(researchAreas));

        project.setUris(projectDTO.getUris());
        project.setDoi(projectDTO.getDoi());
        project.setRaid(projectDTO.getRaid());
        project.setDateFrom(projectDTO.getDateFrom());
        project.setDateTo(projectDTO.getDateTo());
        project.setStatus(projectDTO.getStatus());
        project.setCollaborationType(projectDTO.getCollaborationType());
        project.setResearchType(projectDTO.getResearchType());
        project.setNotFunded(projectDTO.getNotFunded());
        project.setInternalIdentifiers(projectDTO.getInternalIdentifiers());

        if (Objects.nonNull(projectDTO.getCosts())) {
            if (Objects.isNull(project.getCosts())) {
                project.setCosts(new MonetaryAmount());
            }
            project.getCosts().setCurrency(
                    currencyService.findOne(projectDTO.getCosts().getCurrencyId()));
            project.getCosts().setAmount(projectDTO.getCosts().getAmount());
        } else {
            project.setCosts(null);
        }
    }

    private void clearCommonFields(Project project) {
        project.getName().clear();
        project.getDescription().clear();
        project.getNameAbbreviation().clear();
        project.getKeywords().clear();
        project.getResearchAreas().clear();
    }

    private ProjectIndex indexCommonFields(Project project, ProjectIndex index) {
        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
                project.getName(), true);

        if (srContent.isEmpty() && !otherContent.isEmpty()) {
            srContent.append(otherContent);
        } else if (!srContent.isEmpty() && otherContent.isEmpty()) {
            otherContent.append(srContent);
        }

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
                project.getNameAbbreviation(), false);

        StringUtil.removeTrailingDelimiters(srContent, otherContent);
        index.setNameSr(!srContent.isEmpty() ? srContent.toString() : otherContent.toString());
        index.setNameSrSortable(index.getNameSr());
        index.setNameOther(
                !otherContent.isEmpty() ? otherContent.toString() : srContent.toString());
        index.setNameOtherSortable(index.getNameOther());

        index.setDatabaseId(project.getId());

        return index;
    }


}
