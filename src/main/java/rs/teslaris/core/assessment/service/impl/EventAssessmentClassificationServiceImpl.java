package rs.teslaris.core.assessment.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.dto.EventAssessmentClassificationDTO;
import rs.teslaris.core.assessment.model.EventAssessmentClassification;
import rs.teslaris.core.assessment.repository.EntityAssessmentClassificationRepository;
import rs.teslaris.core.assessment.repository.EventAssessmentClassificationRepository;
import rs.teslaris.core.assessment.service.impl.cruddelegate.EventAssessmentClassificationJPAServiceImpl;
import rs.teslaris.core.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.core.assessment.service.interfaces.CommissionService;
import rs.teslaris.core.assessment.service.interfaces.EventAssessmentClassificationService;
import rs.teslaris.core.service.interfaces.document.EventService;

@Service
public class EventAssessmentClassificationServiceImpl
    extends EntityAssessmentClassificationServiceImpl
    implements EventAssessmentClassificationService {

    private final EventAssessmentClassificationJPAServiceImpl
        eventAssessmentClassificationJPAService;

    private final EventAssessmentClassificationRepository eventAssessmentClassificationRepository;

    private final EventService eventService;

    @Autowired
    public EventAssessmentClassificationServiceImpl(
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        CommissionService commissionService,
        AssessmentClassificationService assessmentClassificationService,
        EventAssessmentClassificationJPAServiceImpl eventAssessmentClassificationJPAService,
        EventAssessmentClassificationRepository eventAssessmentClassificationRepository,
        EventService eventService) {
        super(entityAssessmentClassificationRepository, commissionService,
            assessmentClassificationService);
        this.eventAssessmentClassificationJPAService = eventAssessmentClassificationJPAService;
        this.eventAssessmentClassificationRepository = eventAssessmentClassificationRepository;
        this.eventService = eventService;
    }


    @Override
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForEvent(
        Integer eventId) {
        return eventAssessmentClassificationRepository.findAssessmentClassificationsForEvent(
                eventId).stream().map(EntityAssessmentClassificationConverter::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public EventAssessmentClassification createEventAssessmentClassification(
        EventAssessmentClassificationDTO eventAssessmentClassificationDTO) {
        var newAssessmentClassification = new EventAssessmentClassification();

        setCommonFields(newAssessmentClassification, eventAssessmentClassificationDTO);
        newAssessmentClassification.setEvent(
            eventService.findOne(eventAssessmentClassificationDTO.getEventId()));

        return eventAssessmentClassificationJPAService.save(newAssessmentClassification);
    }

    @Override
    public void updateEventAssessmentClassification(Integer eventAssessmentClassificationId,
                                                    EventAssessmentClassificationDTO eventAssessmentClassificationDTO) {
        var eventAssessmentClassificationToUpdate =
            eventAssessmentClassificationJPAService.findOne(eventAssessmentClassificationId);

        setCommonFields(eventAssessmentClassificationToUpdate, eventAssessmentClassificationDTO);
        eventAssessmentClassificationToUpdate.setEvent(
            eventService.findOne(eventAssessmentClassificationDTO.getEventId()));

        eventAssessmentClassificationJPAService.save(eventAssessmentClassificationToUpdate);
    }
}
