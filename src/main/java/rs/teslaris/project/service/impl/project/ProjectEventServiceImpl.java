package rs.teslaris.project.service.impl.project;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.dto.project.ProjectEventDTO;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.funding.FundingPart;
import rs.teslaris.project.model.project.ProjectEvent;
import rs.teslaris.project.repository.project.ProjectEventRepository;
import rs.teslaris.project.service.interfaces.project.ProjectEventService;
import rs.teslaris.project.service.interfaces.project.ProjectService;

import java.util.HashSet;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectEventServiceImpl extends JPAServiceImpl<ProjectEvent>
    implements ProjectEventService {

    private final ProjectEventRepository projectEventRepository;

    private final IndexBulkUpdateService indexBulkUpdateService;
    private final MultilingualContentService multilingualContentService;
    private final CurrencyService currencyService;
    private final ProjectService projectService;
    private final EventService eventService;

    @Override
    protected JpaRepository<ProjectEvent, Integer> getEntityRepository() {
        return projectEventRepository;
    }

    @Override
    @Transactional
    public ProjectEvent createProjectEvent(ProjectEventDTO projectEventDTO) {
        var newProjectEvent = new ProjectEvent();

        setCommonFields(newProjectEvent, projectEventDTO);

        var savedProjectEvent = save(newProjectEvent);

        indexBulkUpdateService.setIdFieldForRecord("events", "databaseId",
                savedProjectEvent.getEvent().getId(), "project_id", savedProjectEvent.getProject().getId());

        return savedProjectEvent;
    }

    private void setCommonFields(ProjectEvent projectEvent, ProjectEventDTO dto) {

        buildFundingParts(projectEvent, dto);
        projectEvent.setTextualDescription(
                multilingualContentService.getMultilingualContent(dto.getTextualDescription()));

        projectEvent.setRelationType(dto.getRelationType());

        if (Objects.nonNull(dto.getProjectId())) {
            projectEvent.setProject(projectService.findOne(dto.getProjectId()));
        } else {
            projectEvent.setProject(null);
        }

        if (Objects.nonNull(dto.getEventId())) {
            projectEvent.setEvent(eventService.findOne(dto.getEventId()));
        } else {
            projectEvent.setEvent(null);
        }
    }

    private void buildFundingParts(ProjectEvent projectEvent,
                                   ProjectEventDTO dto) {
        if (Objects.isNull(projectEvent.getFundingParts())) {
            projectEvent.setFundingParts(new HashSet<>());
        }

        dto.getFundingParts().forEach(partDTO -> {
            var part = buildFundingPart(partDTO, projectEvent);
            projectEvent.getFundingParts().add(part);
        });
    }

    private FundingPart buildFundingPart(FundingPartDTO dto, ProjectEvent parent) {
        var part = new FundingPart();

        part.setDescription(
                multilingualContentService.getMultilingualContent(dto.getDescription()));

        part.setAmount(new MonetaryAmount());
        part.getAmount().setCurrency(
                currencyService.findOne(dto.getAmount().getCurrencyId()));
        part.getAmount().setAmount(dto.getAmount().getAmount());

        if (Objects.nonNull(dto.getFundingId())) {
            part.setProjectEvent(parent);
        }

        return part;
    }

}
