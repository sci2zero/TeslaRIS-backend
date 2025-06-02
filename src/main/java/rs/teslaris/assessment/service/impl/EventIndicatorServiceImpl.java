package rs.teslaris.assessment.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.converter.EntityIndicatorConverter;
import rs.teslaris.assessment.dto.EntityIndicatorResponseDTO;
import rs.teslaris.assessment.dto.EventIndicatorDTO;
import rs.teslaris.assessment.model.EventIndicator;
import rs.teslaris.assessment.repository.EntityIndicatorRepository;
import rs.teslaris.assessment.repository.EventIndicatorRepository;
import rs.teslaris.assessment.service.impl.cruddelegate.EventIndicatorJPAServiceImpl;
import rs.teslaris.assessment.service.interfaces.EventIndicatorService;
import rs.teslaris.assessment.service.interfaces.IndicatorService;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.user.UserService;

@Service
@Transactional
@Traceable
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
        super(indicatorService, entityIndicatorRepository, eventFileService);
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
