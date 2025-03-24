package rs.teslaris.core.assessment.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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
import rs.teslaris.core.assessment.util.AssessmentRulesConfigurationLoader;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.model.document.EventsRelationType;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;

@Service
@Transactional
public class EventAssessmentClassificationServiceImpl
    extends EntityAssessmentClassificationServiceImpl
    implements EventAssessmentClassificationService {

    private final EventAssessmentClassificationJPAServiceImpl
        eventAssessmentClassificationJPAService;

    private final EventAssessmentClassificationRepository eventAssessmentClassificationRepository;

    private final EventService eventService;

    private final UserService userService;

    private final NotificationService notificationService;


    @Autowired
    public EventAssessmentClassificationServiceImpl(
        AssessmentClassificationService assessmentClassificationService,
        CommissionService commissionService,
        DocumentPublicationService documentPublicationService,
        ConferenceService conferenceService,
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        EventAssessmentClassificationJPAServiceImpl eventAssessmentClassificationJPAService,
        EventAssessmentClassificationRepository eventAssessmentClassificationRepository,
        EventService eventService, UserService userService,
        NotificationService notificationService) {
        super(assessmentClassificationService, commissionService, documentPublicationService,
            conferenceService, entityAssessmentClassificationRepository);
        this.eventAssessmentClassificationJPAService = eventAssessmentClassificationJPAService;
        this.eventAssessmentClassificationRepository = eventAssessmentClassificationRepository;
        this.eventService = eventService;
        this.userService = userService;
        this.notificationService = notificationService;
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

        var commission =
            commissionService.findOne(eventAssessmentClassificationDTO.getCommissionId());
        newAssessmentClassification.setCommission(commission);
        setCommonFields(newAssessmentClassification, eventAssessmentClassificationDTO);

        var assessmentClassification = assessmentClassificationService.findOne(
            eventAssessmentClassificationDTO.getAssessmentClassificationId());
        newAssessmentClassification.setClassificationReason(
            AssessmentRulesConfigurationLoader.getRuleDescription("eventClassificationRules",
                "manual", MultilingualContentConverter.getMultilingualContentDTO(
                    assessmentClassification.getTitle())));

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
                        instanceClassification.setCommission(commission);
                        instanceClassification.setClassificationReason(
                            AssessmentRulesConfigurationLoader.getRuleDescription(
                                "eventClassificationRules", "manual",
                                MultilingualContentConverter.getMultilingualContentDTO(
                                    assessmentClassification.getTitle())));

                        eventAssessmentClassificationJPAService.save(instanceClassification);
                        conferenceService.reindexVolatileConferenceInformation(
                            eventInstance.getId());
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

        var savedClassification =
            eventAssessmentClassificationJPAService.save(newAssessmentClassification);
        conferenceService.reindexVolatileConferenceInformation(event.getId());

        return savedClassification;
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
                        conferenceService.reindexVolatileConferenceInformation(
                            eventInstance.getId());
                    }
                });
        } else {
            var classificationYear = event.getDateFrom().getYear();
            eventAssessmentClassificationToUpdate.setClassificationYear(classificationYear);
        }

        eventAssessmentClassificationJPAService.save(eventAssessmentClassificationToUpdate);
        conferenceService.reindexVolatileConferenceInformation(
            eventAssessmentClassificationToUpdate.getEvent().getId());
    }

    @Scheduled(cron = "0 * * * * *")
    protected void sendNotificationsToCommissions() {
        userService.findAllCommissionUsers().forEach(user -> {
            var totalAndInstitutionCount = eventService.getEventCountsBelongingToInstitution(
                user.getOrganisationUnit().getId());

            var totalClassifiedAndInstitutionCount =
                eventService.getClassifiedEventCountsForCommission(
                    user.getOrganisationUnit().getId(), user.getCommission().getId());

            notificationService.createNotification(
                NotificationFactory.contructNewEventsForClassificationNotification(
                    Map.of("totalCount", String.valueOf(
                            longValue(totalAndInstitutionCount.a) -
                                longValue(totalClassifiedAndInstitutionCount.a)),
                        "fromMyInstitutionCount", String.valueOf(
                            longValue(totalAndInstitutionCount.b) -
                                longValue(totalClassifiedAndInstitutionCount.b))),
                    user)
            );
        });
    }

    private long longValue(Long value) {
        return Objects.requireNonNullElse(value, 0).longValue();
    }
}
