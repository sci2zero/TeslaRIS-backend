package rs.teslaris.project.service.impl.project;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.util.exceptionhandling.exception.MissingDataException;
import rs.teslaris.project.converter.project.ProjectEventConverter;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.dto.project.ProjectEventDTO;
import rs.teslaris.project.model.funding.FundingPart;
import rs.teslaris.project.model.project.ProjectEvent;
import rs.teslaris.project.repository.project.ProjectEventRepository;
import rs.teslaris.project.service.interfaces.project.ProjectEventService;
import rs.teslaris.project.service.interfaces.project.ProjectService;
import rs.teslaris.project.util.FundingPartFactory;
import rs.teslaris.project.repository.funding.FundingPartRepository;

@Service
@RequiredArgsConstructor
public class ProjectEventServiceImpl extends JPAServiceImpl<ProjectEvent>
    implements ProjectEventService {

    private final FundingPartFactory fundingPartFactory;

    private final ProjectEventRepository projectEventRepository;

    private final FundingPartRepository fundingPartRepository;

    private final IndexBulkUpdateService indexBulkUpdateService;
    private final MultilingualContentService multilingualContentService;
    private final CurrencyService currencyService;
    private final ProjectService projectService;
    private final EventService eventService;
    private final EventIndexRepository eventIndexRepository;

    @Override
    protected JpaRepository<ProjectEvent, Integer> getEntityRepository() {
        return projectEventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectEventDTO> readProjectEvents(Integer projectId) {
        var projectEvents = projectEventRepository.findAllByProjectId(projectId).stream()
            .map(ProjectEventConverter::toDTO)
            .toList();

        setDisplayFieldsFromIndex(projectEvents);

        return projectEvents;
    }

    private void setDisplayFieldsFromIndex(List<ProjectEventDTO> projectEvents) {
        var eventIds = projectEvents.stream()
            .map(ProjectEventDTO::getEventId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        if (eventIds.isEmpty()) {
            return;
        }

        var indexes = eventIndexRepository
            .findByDatabaseIdIn(eventIds, PageRequest.of(0, eventIds.size()))
            .getContent().stream()
            .collect(Collectors.toMap(EventIndex::getDatabaseId, index -> index,
                (first, second) -> first));

        projectEvents.forEach(projectEvent -> {
            var index = indexes.get(projectEvent.getEventId());
            if (Objects.nonNull(index)) {
                projectEvent.setEventNameSr(index.getNameSr());
                projectEvent.setEventNameOther(index.getNameOther());

                if (Objects.nonNull(index.getEventType())) {
                    projectEvent.setEventType(index.getEventType().name());
                }
            }
        });
    }

    @Override
    @Transactional
    public ProjectEvent createProjectEvent(ProjectEventDTO projectEventDTO) {
        var newProjectEvent = new ProjectEvent();

        setCommonFields(newProjectEvent, projectEventDTO);

        var savedProjectEvent = save(newProjectEvent);

        if (Objects.nonNull(savedProjectEvent.getEvent())) {
            indexBulkUpdateService.setIdFieldForRecord("events", "databaseId",
                savedProjectEvent.getEvent().getId(), "project_id",
                savedProjectEvent.getProject().getId());
        }

        return savedProjectEvent;
    }

    @Override
    public void deleteProjectEvent(Integer projectEventId) {
        var projectEvent = findOne(projectEventId);

        var event = projectEvent.getEvent();

        delete(projectEventId);

        if (Objects.nonNull(event)) {
            indexBulkUpdateService.setIdFieldForRecord("events", "databaseId",
                event.getId(), "project_id", null);
        }
    }

    private void setCommonFields(ProjectEvent projectEvent, ProjectEventDTO dto) {
        if (Objects.isNull(dto.getEventId()) &&
            (Objects.isNull(dto.getTextualDescription()) ||
                dto.getTextualDescription().isEmpty())) {
            throw new MissingDataException(
                "Either an event or a textual description has to be provided.");
        }

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
            projectEvent.getFundingParts().add(fundingPartRepository.save(part));
        });
    }

    private FundingPart buildFundingPart(FundingPartDTO dto, ProjectEvent parent) {
        var part = fundingPartFactory.buildFundingPart(dto);
        part.setProjectEvent(parent);

        return part;
    }

}
