package rs.teslaris.core.unit.project;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.model.document.OtherEvent;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.dto.project.ProjectEventDTO;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.model.project.ProjectEvent;
import rs.teslaris.project.model.project.ProjectEventType;
import rs.teslaris.project.repository.project.ProjectEventRepository;
import rs.teslaris.project.service.impl.project.ProjectEventServiceImpl;
import rs.teslaris.project.service.interfaces.project.ProjectService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ProjectEventServiceTest {

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private ProjectService projectService;

    @Mock
    private EventService eventService;

    @Mock
    private ProjectEventRepository projectEventRepository;

    @Mock
    private IndexBulkUpdateService indexBulkUpdateService;

    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private ProjectEventServiceImpl projectEventService;

    @Test
    public void shouldCreateProjectEventSuccessfully() {
        // given
        var dto = new ProjectEventDTO();
        dto.setProjectId(1);
        dto.setEventId(2);
        dto.setRelationType(ProjectEventType.MEETING);
        dto.setTextualDescription(List.of());
        dto.setFundingParts(List.of());

        var project = new Project();
        project.setId(1);

        // used OtherEvent class because the Event is abstract and could not be instantiated
        var event = new OtherEvent();
        event.setId(2);

        var savedEvent = new ProjectEvent();
        savedEvent.setId(10);
        savedEvent.setProject(project);
        savedEvent.setEvent(event);

        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(Set.of());
        when(projectService.findOne(1)).thenReturn(project);
        when(eventService.findOne(2)).thenReturn(event);
        when(projectEventRepository.save(any(ProjectEvent.class))).thenReturn(savedEvent);

        // when
        var result = projectEventService.createProjectEvent(dto);

        // then
        assertNotNull(result);
        assertEquals(10, result.getId());
        verify(projectService).findOne(1);
        verify(eventService).findOne(2);
        verify(projectEventRepository).save(any(ProjectEvent.class));
        verify(indexBulkUpdateService).setIdFieldForRecord("events", "databaseId", 2, "project_id", 1);
    }

    @Test
    public void shouldCreateProjectEventWithFundingParts() {
        // given
        var monetaryAmountDTO = new MonetaryAmountDTO();
        monetaryAmountDTO.setCurrencyId(1);
        monetaryAmountDTO.setAmount(5000.0);

        var fundingPartDTO = new FundingPartDTO();
        fundingPartDTO.setFundingId(100);
        fundingPartDTO.setDescription(List.of());
        fundingPartDTO.setAmount(monetaryAmountDTO);

        var dto = new ProjectEventDTO();
        dto.setProjectId(1);
        dto.setEventId(2);
        dto.setRelationType(ProjectEventType.MEETING);
        dto.setTextualDescription(List.of());
        dto.setFundingParts(List.of(fundingPartDTO));

        var project = new Project();
        project.setId(1);

        // used OtherEvent class because the Event is abstract and could not be instantiated
        var event = new OtherEvent();
        event.setId(2);

        var savedEvent = new ProjectEvent();
        savedEvent.setId(10);
        savedEvent.setProject(project);
        savedEvent.setEvent(event);

        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(Set.of());
        when(projectService.findOne(1)).thenReturn(project);
        when(eventService.findOne(2)).thenReturn(event);
        when(currencyService.findOne(1)).thenReturn(null);
        when(projectEventRepository.save(any(ProjectEvent.class))).thenReturn(savedEvent);

        // when
        var result = projectEventService.createProjectEvent(dto);

        // then
        assertNotNull(result);
        verify(currencyService).findOne(1);
        verify(multilingualContentService, times(2)).getMultilingualContent(anyList());
        verify(projectEventRepository).save(any(ProjectEvent.class));
    }

    @Test
    public void shouldThrowWhenProjectNotFoundForEvent() {
        // given
        var dto = new ProjectEventDTO();
        dto.setProjectId(999);
        dto.setEventId(2);
        dto.setRelationType(ProjectEventType.MEETING);
        dto.setTextualDescription(List.of());
        dto.setFundingParts(List.of());

        when(projectService.findOne(999)).thenThrow(NotFoundException.class);

        // when & then
        assertThrows(NotFoundException.class, () -> projectEventService.createProjectEvent(dto));

        verify(projectEventRepository, never()).save(any());
    }

    @Test
    public void shouldThrowWhenEventNotFound() {
        // given
        var dto = new ProjectEventDTO();
        dto.setProjectId(1);
        dto.setEventId(999);
        dto.setRelationType(ProjectEventType.MEETING);
        dto.setTextualDescription(List.of());
        dto.setFundingParts(List.of());

        var project = new Project();
        project.setId(1);

        when(projectService.findOne(1)).thenReturn(project);
        when(eventService.findOne(999)).thenThrow(NotFoundException.class);

        // when & then
        assertThrows(NotFoundException.class, () -> projectEventService.createProjectEvent(dto));

        verify(projectEventRepository, never()).save(any());
    }

    @Test
    public void shouldCallIndexBulkUpdateAfterEventSave() {
        // given
        var dto = new ProjectEventDTO();
        dto.setProjectId(1);
        dto.setEventId(2);
        dto.setRelationType(ProjectEventType.MEETING);
        dto.setTextualDescription(List.of());
        dto.setFundingParts(List.of());

        var project = new Project();
        project.setId(1);

        // used OtherEvent class because the Event is abstract and could not be instantiated
        var event = new OtherEvent();
        event.setId(2);

        var savedEvent = new ProjectEvent();
        savedEvent.setId(10);
        savedEvent.setProject(project);
        savedEvent.setEvent(event);

        when(multilingualContentService.getMultilingualContent(anyList())).thenReturn(Set.of());
        when(projectService.findOne(1)).thenReturn(project);
        when(eventService.findOne(2)).thenReturn(event);
        when(projectEventRepository.save(any(ProjectEvent.class))).thenReturn(savedEvent);

        // when
        projectEventService.createProjectEvent(dto);

        // then
        verify(indexBulkUpdateService).setIdFieldForRecord(
                "events", "databaseId", 2, "project_id", 1);
    }

    @Test
    public void shouldDeleteProjectEventSuccessfully() {
        // given
        var project = new Project();
        project.setId(1);

        var event = new OtherEvent();
        event.setId(2);

        var projectEvent = new ProjectEvent();
        projectEvent.setId(10);
        projectEvent.setProject(project);
        projectEvent.setEvent(event);

        when(projectEventRepository.findById(10)).thenReturn(Optional.of(projectEvent));

        // when
        projectEventService.deleteProjectEvent(10);

        // then
        verify(projectEventRepository).save(argThat(pe -> pe.getDeleted().equals(true)));
        verify(indexBulkUpdateService).removeIdFieldFromRecord(
                "events", "databaseId", 2, "project_id", 1);
    }

    @Test
    public void shouldThrowWhenProjectEventNotFound() {
        // given
        when(projectEventRepository.findById(999)).thenThrow(NotFoundException.class);

        // when & then
        assertThrows(NotFoundException.class,
                () -> projectEventService.deleteProjectEvent(999));

        verify(projectEventRepository, never()).save(any());
        verify(indexBulkUpdateService, never()).removeIdFieldFromRecord(any(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotCallElasticsearchWhenEventDeleteFails() {
        // given
        var project = new Project();
        project.setId(1);

        var event = new OtherEvent();
        event.setId(2);

        var projectEvent = new ProjectEvent();
        projectEvent.setId(10);
        projectEvent.setProject(project);
        projectEvent.setEvent(event);

        when(projectEventRepository.findById(10)).thenReturn(Optional.of(projectEvent));
        when(projectEventRepository.save(any(ProjectEvent.class))).thenThrow(RuntimeException.class);

        // when & then
        assertThrows(RuntimeException.class,
                () -> projectEventService.deleteProjectEvent(10));

        verify(indexBulkUpdateService, never()).removeIdFieldFromRecord(any(), any(), any(), any(), any());
    }

}
