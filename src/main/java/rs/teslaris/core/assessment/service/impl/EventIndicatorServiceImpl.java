package rs.teslaris.core.assessment.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.converter.EntityIndicatorConverter;
import rs.teslaris.core.assessment.dto.EntityIndicatorResponseDTO;
import rs.teslaris.core.assessment.model.IndicatorAccessLevel;
import rs.teslaris.core.assessment.repository.EntityIndicatorRepository;
import rs.teslaris.core.assessment.repository.EventIndicatorRepository;
import rs.teslaris.core.assessment.service.interfaces.EventIndicatorService;
import rs.teslaris.core.assessment.service.interfaces.IndicatorService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;

@Service
public class EventIndicatorServiceImpl extends EntityIndicatorServiceImpl
    implements EventIndicatorService {

    private final EventIndicatorRepository eventIndicatorRepository;

    @Autowired
    public EventIndicatorServiceImpl(
        EntityIndicatorRepository entityIndicatorRepository,
        DocumentFileService documentFileService,
        IndicatorService indicatorService, EventIndicatorRepository eventIndicatorRepository) {
        super(entityIndicatorRepository, documentFileService, indicatorService);
        this.eventIndicatorRepository = eventIndicatorRepository;
    }

    @Override
    public List<EntityIndicatorResponseDTO> getIndicatorsForEvent(Integer eventId,
                                                                  IndicatorAccessLevel accessLevel) {
        return eventIndicatorRepository.findIndicatorsForEventAndIndicatorAccessLevel(eventId,
            accessLevel).stream().map(
            EntityIndicatorConverter::toDTO).collect(Collectors.toList());
    }
}
