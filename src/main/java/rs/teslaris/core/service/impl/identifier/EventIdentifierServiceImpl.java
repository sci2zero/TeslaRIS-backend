package rs.teslaris.core.service.impl.identifier;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.identifier.EntityIdentifierConverter;
import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.dto.identifier.EventIdentifierDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.EventIdentifier;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.repository.identifier.EventIdentifierRepository;
import rs.teslaris.core.service.impl.identifier.cruddelegate.EventIdentifierJPAServiceImpl;
import rs.teslaris.core.service.interfaces.document.EventLookupService;
import rs.teslaris.core.service.interfaces.identifier.EventIdentifierService;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;

@Service
@Traceable
public class EventIdentifierServiceImpl extends EntityIdentifierServiceImpl
    implements EventIdentifierService {

    private final EventIdentifierRepository eventIdentifierRepository;

    private final EventIdentifierJPAServiceImpl eventIdentifierJPAService;

    private final EventLookupService eventLookupService;


    @Autowired
    public EventIdentifierServiceImpl(EntityIdentifierRepository entityIdentifierRepository,
                                      IdentifierService identifierService,
                                      EventIdentifierRepository eventIdentifierRepository,
                                      EventIdentifierJPAServiceImpl eventIdentifierJPAService,
                                      EventLookupService eventLookupService) {
        super(entityIdentifierRepository, identifierService);
        this.eventIdentifierRepository = eventIdentifierRepository;
        this.eventIdentifierJPAService = eventIdentifierJPAService;
        this.eventLookupService = eventLookupService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityIdentifierResponseDTO> getIdentifiersForEvent(Integer eventId,
                                                                    AccessLevel accessLevel) {
        return eventIdentifierRepository.findIdentifiersForEventAndIdentifierAccessLevel(eventId,
            accessLevel).stream().map(
            EntityIdentifierConverter::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventIdentifier createEventIdentifier(EventIdentifierDTO eventIdentifierDTO,
                                                 Integer userId) {
        var newEventIdentifier = new EventIdentifier();

        setCommonFields(newEventIdentifier, eventIdentifierDTO);

        newEventIdentifier.setEvent(
            eventLookupService.fastEventLookup(eventIdentifierDTO.getEventId()));

        return eventIdentifierJPAService.save(newEventIdentifier);
    }

    @Override
    @Transactional
    public void updateEventIdentifier(Integer eventIdentifierId,
                                      EventIdentifierDTO eventIdentifierDTO) {
        var eventIdentifierToUpdate = eventIdentifierJPAService.findOne(eventIdentifierId);

        setCommonFields(eventIdentifierToUpdate, eventIdentifierDTO);

        eventIdentifierToUpdate.setEvent(
            eventLookupService.fastEventLookup(eventIdentifierDTO.getEventId()));

        eventIdentifierJPAService.save(eventIdentifierToUpdate);
    }
}
