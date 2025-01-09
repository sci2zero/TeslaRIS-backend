package rs.teslaris.core.assessment.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.dto.PublicationSeriesAssessmentClassificationDTO;
import rs.teslaris.core.assessment.model.PublicationSeriesAssessmentClassification;
import rs.teslaris.core.assessment.repository.EntityAssessmentClassificationRepository;
import rs.teslaris.core.assessment.repository.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.core.assessment.repository.PublicationSeriesIndicatorRepository;
import rs.teslaris.core.assessment.ruleengine.JournalClassificationRuleEngine;
import rs.teslaris.core.assessment.service.impl.cruddelegate.PublicationSeriesAssessmentClassificationJPAServiceImpl;
import rs.teslaris.core.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.core.assessment.service.interfaces.CommissionService;
import rs.teslaris.core.assessment.service.interfaces.PublicationSeriesAssessmentClassificationService;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesService;

@Service
public class PublicationSeriesAssessmentClassificationServiceImpl
    extends EntityAssessmentClassificationServiceImpl
    implements PublicationSeriesAssessmentClassificationService {

    private final PublicationSeriesAssessmentClassificationJPAServiceImpl
        publicationSeriesAssessmentClassificationJPAService;

    private final PublicationSeriesAssessmentClassificationRepository
        publicationSeriesAssessmentClassificationRepository;

    private final PublicationSeriesService publicationSeriesService;

    private final PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository;

    private final JournalRepository journalRepository;

    private final JournalIndexRepository journalIndexRepository;

    private final CommissionService commissionService;

    private final TaskManagerService taskManagerService;

    private final AssessmentClassificationService assessmentClassificationService;

    private final String RULE_ENGINE_BASE_PACKAGE =
        "rs.teslaris.core.assessment.ruleengine.";


    @Autowired
    public PublicationSeriesAssessmentClassificationServiceImpl(
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        CommissionService commissionService,
        AssessmentClassificationService assessmentClassificationService,
        PublicationSeriesAssessmentClassificationJPAServiceImpl publicationSeriesAssessmentClassificationJPAService,
        PublicationSeriesAssessmentClassificationRepository publicationSeriesAssessmentClassificationRepository,
        PublicationSeriesService publicationSeriesService,
        PublicationSeriesIndicatorRepository publicationSeriesIndicatorRepository,
        JournalRepository journalRepository, JournalIndexRepository journalIndexRepository,
        CommissionService commissionService1, TaskManagerService taskManagerService,
        AssessmentClassificationService assessmentClassificationService1) {
        super(entityAssessmentClassificationRepository, commissionService,
            assessmentClassificationService);
        this.publicationSeriesAssessmentClassificationJPAService =
            publicationSeriesAssessmentClassificationJPAService;
        this.publicationSeriesAssessmentClassificationRepository =
            publicationSeriesAssessmentClassificationRepository;
        this.publicationSeriesService = publicationSeriesService;
        this.publicationSeriesIndicatorRepository = publicationSeriesIndicatorRepository;
        this.journalRepository = journalRepository;
        this.journalIndexRepository = journalIndexRepository;
        this.commissionService = commissionService1;
        this.taskManagerService = taskManagerService;
        this.assessmentClassificationService = assessmentClassificationService1;
    }

    @Override
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPublicationSeries(
        Integer publicationSeriesId) {
        return publicationSeriesAssessmentClassificationRepository.findAssessmentClassificationsForPublicationSeries(
                publicationSeriesId).stream().map(EntityAssessmentClassificationConverter::toDTO)
            .collect(Collectors.toList());
    }

    @Override
    public PublicationSeriesAssessmentClassification createPublicationSeriesAssessmentClassification(
        PublicationSeriesAssessmentClassificationDTO publicationSeriesAssessmentClassificationDTO) {
        var newAssessmentClassification = new PublicationSeriesAssessmentClassification();

        setCommonFields(newAssessmentClassification, publicationSeriesAssessmentClassificationDTO);
        newAssessmentClassification.setPublicationSeries(
            publicationSeriesService.findOne(
                publicationSeriesAssessmentClassificationDTO.getPublicationSeriesId()));

        return publicationSeriesAssessmentClassificationJPAService.save(
            newAssessmentClassification);
    }

    @Override
    public void updatePublicationSeriesAssessmentClassification(
        Integer publicationSeriesAssessmentClassificationId,
        PublicationSeriesAssessmentClassificationDTO publicationSeriesAssessmentClassificationDTO) {
        var publicationSeriesAssessmentClassificationToUpdate =
            publicationSeriesAssessmentClassificationJPAService.findOne(
                publicationSeriesAssessmentClassificationId);

        setCommonFields(publicationSeriesAssessmentClassificationToUpdate,
            publicationSeriesAssessmentClassificationDTO);
        publicationSeriesAssessmentClassificationToUpdate.setPublicationSeries(
            publicationSeriesService.findOne(
                publicationSeriesAssessmentClassificationDTO.getPublicationSeriesId()));

        publicationSeriesAssessmentClassificationJPAService.save(
            publicationSeriesAssessmentClassificationToUpdate);
    }

    @Override
    public void performJournalClassification(Integer commissionId,
                                             List<Integer> classificationYears) {
        var commission = commissionService.findOne(commissionId);
        var className = commission.getFormalDescriptionOfRule();
        JournalClassificationRuleEngine ruleEngine;
        try {
            Class<?> clazz = Class.forName(RULE_ENGINE_BASE_PACKAGE + className);

            ruleEngine =
                (JournalClassificationRuleEngine) clazz.getDeclaredConstructor().newInstance();
            ruleEngine.initialize(publicationSeriesIndicatorRepository, journalRepository,
                journalIndexRepository, publicationSeriesAssessmentClassificationRepository,
                assessmentClassificationService);

            classificationYears.forEach((classificationYear) -> {
                ruleEngine.startClassification(classificationYear, commission);
            });
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + className);
        } catch (NoSuchMethodException e) {
            System.err.println("No default constructor found for: " + className);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            System.err.println("Error instantiating class: " + className);
        }
    }

    @Override
    public void scheduleClassification(LocalDateTime timeToRun, Integer commissionId,
                                       Integer userId, List<Integer> classificationYears) {
        var commission = commissionService.findOne(commissionId);
        taskManagerService.scheduleTask(
            "Publication_Series_Classification-" + commission.getFormalDescriptionOfRule() +
                "-" + UUID.randomUUID(), timeToRun,
            () -> performJournalClassification(commissionId, classificationYears), userId);
    }
}
