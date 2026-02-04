package rs.teslaris.assessment.service.impl.classification;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.dto.classification.EventAssessmentClassificationDTO;
import rs.teslaris.assessment.model.classification.EventAssessmentClassification;
import rs.teslaris.assessment.model.indicator.ApplicableEntityType;
import rs.teslaris.assessment.repository.classification.EntityAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.classification.EventAssessmentClassificationRepository;
import rs.teslaris.assessment.service.impl.cruddelegate.EventAssessmentClassificationJPAServiceImpl;
import rs.teslaris.assessment.service.interfaces.CommissionService;
import rs.teslaris.assessment.service.interfaces.classification.AssessmentClassificationService;
import rs.teslaris.assessment.service.interfaces.classification.EventAssessmentClassificationService;
import rs.teslaris.assessment.util.AssessmentRulesConfigurationLoader;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.applicationevent.EntityAssessmentChanged;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.model.document.EventsRelationType;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.EventLookupService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.ExhibitionService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;

@Service
@Transactional
@Traceable
public class EventAssessmentClassificationServiceImpl
    extends EntityAssessmentClassificationServiceImpl
    implements EventAssessmentClassificationService {

    private final EventAssessmentClassificationJPAServiceImpl
        eventAssessmentClassificationJPAService;

    private final EventAssessmentClassificationRepository eventAssessmentClassificationRepository;

    private final EventService eventService;

    private final EventLookupService eventLookupService;

    private final UserService userService;

    private final NotificationService notificationService;


    @Autowired
    public EventAssessmentClassificationServiceImpl(
        AssessmentClassificationService assessmentClassificationService,
        CommissionService commissionService, DocumentPublicationService documentPublicationService,
        ConferenceService conferenceService, ExhibitionService exhibitionService,
        ApplicationEventPublisher applicationEventPublisher,
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        EventAssessmentClassificationJPAServiceImpl eventAssessmentClassificationJPAService,
        EventAssessmentClassificationRepository eventAssessmentClassificationRepository,
        EventService eventService, EventLookupService eventLookupService, UserService userService,
        NotificationService notificationService) {
        super(assessmentClassificationService, commissionService, documentPublicationService,
            conferenceService, exhibitionService, applicationEventPublisher,
            entityAssessmentClassificationRepository);
        this.eventAssessmentClassificationJPAService = eventAssessmentClassificationJPAService;
        this.eventAssessmentClassificationRepository = eventAssessmentClassificationRepository;
        this.eventService = eventService;
        this.eventLookupService = eventLookupService;
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

        var event =
            eventLookupService.fastEventLookup(eventAssessmentClassificationDTO.getEventId());
        newAssessmentClassification.setEvent(event);
        if (event.getSerialEvent()) {
            newAssessmentClassification.setClassificationYear(null);

            eventService.readSerialEventRelations(eventAssessmentClassificationDTO.getEventId())
                .forEach((relation) -> {
                    if (relation.getEventsRelationType()
                        .equals(EventsRelationType.BELONGS_TO_SERIES)) {
                        var eventInstance =
                            eventLookupService.fastEventLookup(relation.getSourceId());
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
                        reindexVolatileInformation(eventInstance);

                        applicationEventPublisher.publishEvent(
                            new EntityAssessmentChanged(ApplicableEntityType.EVENT,
                                eventInstance.getId(),
                                eventAssessmentClassificationDTO.getCommissionId(), false));
                    }
                });
        } else {
            var classificationYear = event.getDateFrom().getYear();
            newAssessmentClassification.setClassificationYear(classificationYear);
        }

        eventAssessmentClassificationRepository
            .deleteAssessmentClassificationsForEventAndCommission(
                eventAssessmentClassificationDTO.getEventId(),
                eventAssessmentClassificationDTO.getCommissionId());

        var savedClassification =
            eventAssessmentClassificationJPAService.save(newAssessmentClassification);
        reindexVolatileInformation(event);

        applicationEventPublisher.publishEvent(
            new EntityAssessmentChanged(ApplicableEntityType.EVENT,
                eventAssessmentClassificationDTO.getEventId(),
                eventAssessmentClassificationDTO.getCommissionId(), false));

        return savedClassification;
    }

    @Override
    public void updateEventAssessmentClassification(Integer eventAssessmentClassificationId,
                                                    EventAssessmentClassificationDTO eventAssessmentClassificationDTO) {
        var eventAssessmentClassificationToUpdate =
            eventAssessmentClassificationJPAService.findOne(eventAssessmentClassificationId);
        var oldCommissionId = eventAssessmentClassificationToUpdate.getCommission().getId();

        setCommonFields(eventAssessmentClassificationToUpdate, eventAssessmentClassificationDTO);

        var event =
            eventLookupService.fastEventLookup(eventAssessmentClassificationDTO.getEventId());
        eventAssessmentClassificationToUpdate.setEvent(event);
        if (event.getSerialEvent()) {
            eventAssessmentClassificationToUpdate.setClassificationYear(null);

            eventService.readSerialEventRelations(eventAssessmentClassificationDTO.getEventId())
                .forEach((relation) -> {
                    if (relation.getEventsRelationType()
                        .equals(EventsRelationType.BELONGS_TO_SERIES)) {
                        var eventInstance =
                            eventLookupService.fastEventLookup(relation.getSourceId());
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

                        instanceClassification.setClassificationReason(
                            AssessmentRulesConfigurationLoader.getRuleDescription(
                                "eventClassificationRules",
                                "manual", MultilingualContentConverter.getMultilingualContentDTO(
                                    instanceClassification.getAssessmentClassification()
                                        .getTitle())));

                        eventAssessmentClassificationJPAService.save(instanceClassification);
                        reindexVolatileInformation(
                            eventLookupService.fastEventLookup(eventInstance.getId()));

                        applicationEventPublisher.publishEvent(
                            new EntityAssessmentChanged(ApplicableEntityType.EVENT,
                                eventInstance.getId(),
                                eventAssessmentClassificationDTO.getCommissionId(), false));
                    }
                });
        } else {
            var classificationYear = event.getDateFrom().getYear();
            eventAssessmentClassificationToUpdate.setClassificationYear(classificationYear);
        }

        eventAssessmentClassificationToUpdate.setClassificationReason(
            AssessmentRulesConfigurationLoader.getRuleDescription(
                "eventClassificationRules",
                "manual", MultilingualContentConverter.getMultilingualContentDTO(
                    eventAssessmentClassificationToUpdate.getAssessmentClassification()
                        .getTitle())));
        eventAssessmentClassificationJPAService.save(eventAssessmentClassificationToUpdate);
        reindexVolatileInformation(eventLookupService.fastEventLookup(
            eventAssessmentClassificationToUpdate.getEvent().getId())
        );

        applicationEventPublisher.publishEvent(
            new EntityAssessmentChanged(ApplicableEntityType.EVENT,
                eventAssessmentClassificationDTO.getEventId(),
                eventAssessmentClassificationDTO.getCommissionId(), false));
    }

    @Scheduled(cron = "${assessment.event.notify-period}")
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

    private void reindexVolatileInformation(Event event) {
        if (event instanceof Conference) {
            conferenceService.reindexVolatileConferenceInformation(event.getId());
            return;
        }

        exhibitionService.reindexVolatileExhibitionInformation(event.getId());
    }

    private long longValue(Long value) {
        return Objects.requireNonNullElse(value, 0).longValue();
    }
}
