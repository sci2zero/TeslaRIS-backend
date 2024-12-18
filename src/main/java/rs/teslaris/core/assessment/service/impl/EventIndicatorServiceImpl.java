package rs.teslaris.core.assessment.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.converter.EntityIndicatorConverter;
import rs.teslaris.core.assessment.dto.EntityIndicatorResponseDTO;
import rs.teslaris.core.assessment.dto.EventIndicatorDTO;
import rs.teslaris.core.assessment.model.EventIndicator;
import rs.teslaris.core.assessment.repository.EntityIndicatorRepository;
import rs.teslaris.core.assessment.repository.EventIndicatorRepository;
import rs.teslaris.core.assessment.service.impl.cruddelegate.EventIndicatorJPAServiceImpl;
import rs.teslaris.core.assessment.service.interfaces.EventIndicatorService;
import rs.teslaris.core.assessment.service.interfaces.IndicatorService;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.user.UserService;

@Service
public class EventIndicatorServiceImpl extends EntityIndicatorServiceImpl
    implements EventIndicatorService {

    private final EventIndicatorRepository eventIndicatorRepository;

    private final EventIndicatorJPAServiceImpl eventIndicatorJPAService;

    private final UserService userService;

    private final EventService eventService;


    @Autowired
    public EventIndicatorServiceImpl(
        EntityIndicatorRepository entityIndicatorRepository,
        DocumentFileService eventFileService,
        IndicatorService indicatorService, EventIndicatorRepository eventIndicatorRepository,
        EventIndicatorJPAServiceImpl eventIndicatorJPAService,
        UserService userService, EventService eventService) {
        super(entityIndicatorRepository, eventFileService, indicatorService);
        this.eventIndicatorRepository = eventIndicatorRepository;
        this.eventIndicatorJPAService = eventIndicatorJPAService;
        this.userService = userService;
        this.eventService = eventService;
    }

    @Override
    public List<EntityIndicatorResponseDTO> getIndicatorsForEvent(Integer eventId,
                                                                  AccessLevel accessLevel) {
        return eventIndicatorRepository.findIndicatorsForEventAndIndicatorAccessLevel(eventId,
            accessLevel).stream().map(
            EntityIndicatorConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    public EventIndicator createEventIndicator(EventIndicatorDTO eventIndicatorDTO,
                                               Integer userId) {
        var newEventIndicator = new EventIndicator();

        setCommonFields(newEventIndicator, eventIndicatorDTO);
        newEventIndicator.setUser(userService.findOne(userId));

        newEventIndicator.setEvent(
            eventService.findOne(eventIndicatorDTO.getEventId()));

        return eventIndicatorJPAService.save(newEventIndicator);
    }

    @Override
    public void updateEventIndicator(Integer eventIndicatorId,
                                     EventIndicatorDTO eventIndicatorDTO) {
        var eventIndicatorToUpdate = eventIndicatorJPAService.findOne(eventIndicatorId);

        setCommonFields(eventIndicatorToUpdate, eventIndicatorDTO);

        eventIndicatorToUpdate.setEvent(eventService.findOne(eventIndicatorDTO.getEventId()));

        eventIndicatorJPAService.save(eventIndicatorToUpdate);
    }
}
