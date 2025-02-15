package rs.teslaris.core.assessment.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import rs.teslaris.core.model.document.EventsRelationType;
import rs.teslaris.core.service.interfaces.document.EventService;

@Service
@Transactional
public class EventAssessmentClassificationServiceImpl
    extends EntityAssessmentClassificationServiceImpl
    implements EventAssessmentClassificationService {

    private final EventAssessmentClassificationJPAServiceImpl
        eventAssessmentClassificationJPAService;

    private final EventAssessmentClassificationRepository eventAssessmentClassificationRepository;

    private final EventService eventService;


    @Autowired
    public EventAssessmentClassificationServiceImpl(
        AssessmentClassificationService assessmentClassificationService,
        CommissionService commissionService,
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        EventAssessmentClassificationJPAServiceImpl eventAssessmentClassificationJPAService,
        EventAssessmentClassificationRepository eventAssessmentClassificationRepository,
        EventService eventService) {
        super(assessmentClassificationService, commissionService,
            entityAssessmentClassificationRepository);
        this.eventAssessmentClassificationJPAService = eventAssessmentClassificationJPAService;
        this.eventAssessmentClassificationRepository = eventAssessmentClassificationRepository;
        this.eventService = eventService;
    }

    @Override
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForEvent(
        Integer eventId) {
        return eventAssessmentClassificationRepository.findAssessmentClassificationsForEvent(
                eventId).stream().map(EntityAssessmentClassificationConverter::toDTO)
            .sorted((a, b) -> b.year().compareTo(a.year()))
            .collect(Collectors.toList());
    }

    @Override
    public EventAssessmentClassification createEventAssessmentClassification(
        EventAssessmentClassificationDTO eventAssessmentClassificationDTO) {
        var newAssessmentClassification = new EventAssessmentClassification();

        newAssessmentClassification.setCommission(
            commissionService.findOne(eventAssessmentClassificationDTO.getCommissionId()));
        setCommonFields(newAssessmentClassification, eventAssessmentClassificationDTO);

        var event = eventService.findOne(eventAssessmentClassificationDTO.getEventId());
        newAssessmentClassification.setEvent(event);
        if (event.getSerialEvent()) {
            newAssessmentClassification.setClassificationYear(null);

            eventService.readSerialEventRelations(eventAssessmentClassificationDTO.getEventId())
                .forEach((relation) -> {
                    if (relation.getEventsRelationType()
                        .equals(EventsRelationType.BELONGS_TO_SERIES)) {
                        var eventInstance = eventService.findOne(relation.getSourceId());
                        var instanceClassification = new EventAssessmentClassification();

                        var existingClassification =
                            eventAssessmentClassificationRepository
                                .findAssessmentClassificationsForEventAndCommissionAndYear(
                                    eventInstance.getId(),
                                    eventAssessmentClassificationDTO.getCommissionId(),
                                    eventInstance.getDateFrom().getYear());
                        existingClassification.ifPresent(
                            eventAssessmentClassificationRepository::delete);

                        setCommonFields(instanceClassification, eventAssessmentClassificationDTO);
                        instanceClassification.setClassificationYear(
                            eventInstance.getDateFrom().getYear());
                        instanceClassification.setEvent(eventInstance);
                        eventAssessmentClassificationJPAService.save(instanceClassification);
                    }
                });
        } else {
            var classificationYear = event.getDateFrom().getYear();
            newAssessmentClassification.setClassificationYear(classificationYear);
        }

        var existingClassification =
            eventAssessmentClassificationRepository.findAssessmentClassificationsForEventAndCommission(
                eventAssessmentClassificationDTO.getEventId(),
                eventAssessmentClassificationDTO.getCommissionId());
        existingClassification.ifPresent(eventAssessmentClassificationRepository::delete);

        return eventAssessmentClassificationJPAService.save(newAssessmentClassification);
    }

    @Override
    public void updateEventAssessmentClassification(Integer eventAssessmentClassificationId,
                                                    EventAssessmentClassificationDTO eventAssessmentClassificationDTO) {
        var eventAssessmentClassificationToUpdate =
            eventAssessmentClassificationJPAService.findOne(eventAssessmentClassificationId);
        var oldCommissionId = eventAssessmentClassificationToUpdate.getCommission().getId();

        setCommonFields(eventAssessmentClassificationToUpdate, eventAssessmentClassificationDTO);

        var event = eventService.findOne(eventAssessmentClassificationDTO.getEventId());
        eventAssessmentClassificationToUpdate.setEvent(event);
        if (event.getSerialEvent()) {
            eventAssessmentClassificationToUpdate.setClassificationYear(null);

            eventService.readSerialEventRelations(eventAssessmentClassificationDTO.getEventId())
                .forEach((relation) -> {
                    if (relation.getEventsRelationType()
                        .equals(EventsRelationType.BELONGS_TO_SERIES)) {
                        var eventInstance = eventService.findOne(relation.getSourceId());
                        var instanceClassification = new EventAssessmentClassification();

                        var existingClassification =
                            eventAssessmentClassificationRepository
                                .findAssessmentClassificationsForEventAndCommissionAndYear(
                                    eventInstance.getId(), oldCommissionId,
                                    eventInstance.getDateFrom().getYear());
                        existingClassification.ifPresent(
                            eventAssessmentClassificationRepository::delete);

                        setCommonFields(instanceClassification, eventAssessmentClassificationDTO);
                        instanceClassification.setClassificationYear(
                            eventInstance.getDateFrom().getYear());
                        instanceClassification.setEvent(eventInstance);
                        eventAssessmentClassificationJPAService.save(instanceClassification);
                    }
                });
        } else {
            var classificationYear = event.getDateFrom().getYear();
            eventAssessmentClassificationToUpdate.setClassificationYear(classificationYear);
        }

        eventAssessmentClassificationJPAService.save(eventAssessmentClassificationToUpdate);
    }
}
