package rs.teslaris.assessment.service.impl.classification;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.json.JsonData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.assessment.dto.ImaginaryPublicationAssessmentResponseDTO;
import rs.teslaris.assessment.dto.classification.DocumentAssessmentClassificationDTO;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.model.classification.AssessmentClassification;
import rs.teslaris.assessment.model.classification.DocumentAssessmentClassification;
import rs.teslaris.assessment.model.classification.EntityAssessmentClassification;
import rs.teslaris.assessment.model.indicator.DocumentIndicator;
import rs.teslaris.assessment.repository.classification.DocumentAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.classification.EntityAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.classification.EventAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.classification.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.indicator.IndicatorRepository;
import rs.teslaris.assessment.ruleengine.AssessmentPointsRuleEngine;
import rs.teslaris.assessment.ruleengine.AssessmentPointsScalingRuleEngine;
import rs.teslaris.assessment.service.impl.cruddelegate.DocumentClassificationJPAServiceImpl;
import rs.teslaris.assessment.service.interfaces.CommissionService;
import rs.teslaris.assessment.service.interfaces.classification.AssessmentClassificationService;
import rs.teslaris.assessment.service.interfaces.classification.DocumentAssessmentClassificationService;
import rs.teslaris.assessment.service.interfaces.classification.PersonAssessmentClassificationService;
import rs.teslaris.assessment.util.AssessmentRulesConfigurationLoader;
import rs.teslaris.assessment.util.ClassificationPriorityMapping;
import rs.teslaris.assessment.util.ResearchAreasConfigurationLoader;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.applicationevent.ResearcherPointsReindexingEvent;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.NotificationType;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.JournalPublicationType;
import rs.teslaris.core.model.document.ProceedingsPublicationType;
import rs.teslaris.core.model.document.PublicationType;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.model.institution.CommissionRelation;
import rs.teslaris.core.model.institution.ResultCalculationMethod;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.OrganisationUnitsRelationRepository;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.CantEditException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.ThesisException;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.core.util.functional.QuadConsumer;
import rs.teslaris.core.util.functional.Triple;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;
import rs.teslaris.core.util.search.CollectionOperations;

@Service
@Transactional
@Slf4j
@Traceable
public class DocumentAssessmentClassificationServiceImpl
    extends EntityAssessmentClassificationServiceImpl implements
    DocumentAssessmentClassificationService {

    private final DocumentAssessmentClassificationRepository
        documentAssessmentClassificationRepository;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final UserService userService;

    private final OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    private final PublicationSeriesAssessmentClassificationRepository
        publicationSeriesAssessmentClassificationRepository;

    private final DocumentRepository documentRepository;

    private final TaskManagerService taskManagerService;

    private final SearchService<DocumentPublicationIndex> searchService;

    private final EventAssessmentClassificationRepository eventAssessmentClassificationRepository;

    private final IndicatorRepository indicatorRepository;

    private final EventIndexRepository eventIndexRepository;

    private final DocumentClassificationJPAServiceImpl documentClassificationJPAService;

    private final NotificationService notificationService;

    private final PersonAssessmentClassificationService personAssessmentClassificationService;


    @Autowired
    public DocumentAssessmentClassificationServiceImpl(
        AssessmentClassificationService assessmentClassificationService,
        CommissionService commissionService, DocumentPublicationService documentPublicationService,
        ConferenceService conferenceService, ApplicationEventPublisher applicationEventPublisher,
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        DocumentAssessmentClassificationRepository documentAssessmentClassificationRepository,
        DocumentPublicationIndexRepository documentPublicationIndexRepository,
        UserService userService,
        OrganisationUnitsRelationRepository organisationUnitsRelationRepository,
        PublicationSeriesAssessmentClassificationRepository publicationSeriesAssessmentClassificationRepository,
        DocumentRepository documentRepository, TaskManagerService taskManagerService,
        SearchService<DocumentPublicationIndex> searchService,
        EventAssessmentClassificationRepository eventAssessmentClassificationRepository,
        IndicatorRepository indicatorRepository, EventIndexRepository eventIndexRepository,
        DocumentClassificationJPAServiceImpl documentClassificationJPAService,
        NotificationService notificationService,
        PersonAssessmentClassificationService personAssessmentClassificationService) {
        super(assessmentClassificationService, commissionService, documentPublicationService,
            conferenceService, applicationEventPublisher, entityAssessmentClassificationRepository);
        this.documentAssessmentClassificationRepository =
            documentAssessmentClassificationRepository;
        this.documentPublicationIndexRepository = documentPublicationIndexRepository;
        this.userService = userService;
        this.organisationUnitsRelationRepository = organisationUnitsRelationRepository;
        this.publicationSeriesAssessmentClassificationRepository =
            publicationSeriesAssessmentClassificationRepository;
        this.documentRepository = documentRepository;
        this.taskManagerService = taskManagerService;
        this.searchService = searchService;
        this.eventAssessmentClassificationRepository = eventAssessmentClassificationRepository;
        this.indicatorRepository = indicatorRepository;
        this.eventIndexRepository = eventIndexRepository;
        this.documentClassificationJPAService = documentClassificationJPAService;
        this.notificationService = notificationService;
        this.personAssessmentClassificationService = personAssessmentClassificationService;
    }

    @Override
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForDocument(
        Integer documentId) {
        return documentAssessmentClassificationRepository.findAssessmentClassificationsForDocument(
                documentId).stream().map(EntityAssessmentClassificationConverter::toDTO)
            .sorted((a, b) -> b.year().compareTo(a.year()))
            .collect(Collectors.toList());
    }

    @Override
    public EntityAssessmentClassificationResponseDTO createDocumentAssessmentClassification(
        DocumentAssessmentClassificationDTO documentAssessmentClassificationDTO) {
        var newDocumentClassification = new DocumentAssessmentClassification();

        setCommonFields(newDocumentClassification, documentAssessmentClassificationDTO);

        newDocumentClassification.setCommission(
            commissionService.findOne(documentAssessmentClassificationDTO.getCommissionId()));
        var document =
            documentRepository.findById(documentAssessmentClassificationDTO.getDocumentId())
                .orElseThrow(() -> new NotFoundException(
                    "Document with ID " + documentAssessmentClassificationDTO.getDocumentId() +
                        " does not exist."));
        checkIfDocumentIsAThesis(document);

        if (Objects.isNull(document.getDocumentDate()) || document.getDocumentDate().isEmpty()) {
            throw new CantEditException("Document does not have publication date.");
        }

        documentAssessmentClassificationRepository.deleteByDocumentIdAndCommissionId(
            documentAssessmentClassificationDTO.getDocumentId(),
            documentAssessmentClassificationDTO.getCommissionId(), true);

        newDocumentClassification.setClassificationYear(
            Integer.parseInt(document.getDocumentDate().split("-")[0]));
        newDocumentClassification.setDocument(document);

        var savedDocumentClassification =
            documentAssessmentClassificationRepository.save(newDocumentClassification);
        documentPublicationService.reindexDocumentVolatileInformation(document.getId());

        applicationEventPublisher.publishEvent(new ResearcherPointsReindexingEvent(
            document.getContributors().stream().filter(c -> Objects.nonNull(c.getPerson()))
                .map(c -> c.getPerson().getId()).toList()));

        return EntityAssessmentClassificationConverter.toDTO(savedDocumentClassification);
    }

    private void checkIfDocumentIsAThesis(Document document) {
        if (document instanceof Thesis && ((Thesis) document).getIsOnPublicReview()) {
            throw new ThesisException("Thesis is on public review, can't edit classifications.");
        }
    }

    @Override
    public void editDocumentAssessmentClassification(Integer classificationId,
                                                     DocumentAssessmentClassificationDTO documentAssessmentClassificationDTO) {
        var documentClassification = documentClassificationJPAService.findOne(classificationId);

        checkIfDocumentIsAThesis(documentClassification.getDocument());
        setCommonFields(documentClassification, documentAssessmentClassificationDTO);

        save(documentClassification);
        documentPublicationService.reindexDocumentVolatileInformation(
            documentClassification.getDocument().getId());
        applicationEventPublisher.publishEvent(new ResearcherPointsReindexingEvent(
            documentClassification.getDocument().getContributors().stream()
                .filter(c -> Objects.nonNull(c.getPerson())).map(c -> c.getPerson().getId())
                .toList()));
    }

    @Override
    public void schedulePublicationClassification(LocalDateTime timeToRun,
                                                  Integer userId, LocalDate fromDate,
                                                  DocumentPublicationType documentPublicationType,
                                                  Integer commissionId,
                                                  List<Integer> authorIds,
                                                  List<Integer> orgUnitIds,
                                                  List<Integer> publishedInIds) {
        var taskId = taskManagerService.scheduleTask(
            documentPublicationType.name() + "_Assessment-From-" + fromDate + "-" +
                UUID.randomUUID(), timeToRun,
            () -> {
                switch (documentPublicationType) {
                    case JOURNAL_PUBLICATION ->
                        classifyJournalPublications(fromDate, commissionId, authorIds, orgUnitIds,
                            publishedInIds);
                    case PROCEEDINGS_PUBLICATION ->
                        classifyProceedingsPublications(fromDate, commissionId, authorIds,
                            orgUnitIds, publishedInIds);
                    case THESIS -> classifyTheses(fromDate, commissionId, authorIds, orgUnitIds,
                        publishedInIds);
                }
            }, userId, RecurrenceType.ONCE);

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timeToRun,
                ScheduledTaskType.PUBLICATION_CLASSIFICATION, new HashMap<>() {{
                put("fromDate", fromDate);
                put("documentPublicationType", documentPublicationType);
                put("commissionId", commissionId);
                put("authorIds", authorIds);
                put("orgUnitIds", orgUnitIds);
                put("userId", userId);
                put("publishedInIds", publishedInIds);
            }}, RecurrenceType.ONCE));
    }

    @Override
    public void classifyJournalPublication(Integer journalPublicationId) {
        var journalPublicationIndex =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                journalPublicationId);

        if (journalPublicationIndex.isEmpty()) {
            throw new NotFoundException(
                "Journal publication with ID " + journalPublicationId + " does not exist");
        }

        journalPublicationIndex.get().getOrganisationUnitIds().forEach(organisationUnitId ->
            assessJournalPublication(journalPublicationIndex.get(), organisationUnitId, null,
                null));
    }

    @Override
    public void classifyProceedingsPublication(Integer proceedingsPublicationId) {
        var proceedingsPublicationIndex =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                proceedingsPublicationId);

        if (proceedingsPublicationIndex.isEmpty()) {
            throw new NotFoundException(
                "Journal publication with ID " + proceedingsPublicationId + " does not exist");
        }

        proceedingsPublicationIndex.get().getOrganisationUnitIds().forEach(organisationUnitId ->
            assessProceedingsPublication(proceedingsPublicationIndex.get(), organisationUnitId,
                null, null));
    }

    private void classifyJournalPublications(LocalDate fromDate, Integer commissionId,
                                             List<Integer> authorIds, List<Integer> orgUnitIds,
                                             List<Integer> journalIds) {
        classifyPublications(fromDate, commissionId, authorIds, orgUnitIds, journalIds,
            List.of(DocumentPublicationType.JOURNAL_PUBLICATION), this::assessJournalPublication);
    }

    private void classifyProceedingsPublications(LocalDate fromDate, Integer commissionId,
                                                 List<Integer> authorIds, List<Integer> orgUnitIds,
                                                 List<Integer> eventIds) {
        classifyPublications(fromDate, commissionId, authorIds, orgUnitIds, eventIds,
            List.of(DocumentPublicationType.PROCEEDINGS_PUBLICATION),
            (publicationIndex, organisationUnitId,
             presetCommission, batchClassifications) -> {
                Runnable assessment = Objects.nonNull(publicationIndex.getJournalId()) ?
                    () -> this.assessJournalPublication(publicationIndex, organisationUnitId,
                        presetCommission, batchClassifications) :
                    () -> this.assessProceedingsPublication(publicationIndex, organisationUnitId,
                        presetCommission, batchClassifications);
                assessment.run();
            });
    }

    private void classifyTheses(LocalDate fromDate, Integer commissionId,
                                List<Integer> authorIds, List<Integer> orgUnitIds,
                                List<Integer> eventIds) {
        classifyPublications(fromDate, commissionId, authorIds, orgUnitIds, eventIds,
            List.of(DocumentPublicationType.THESIS),
            this::assessThesis);
    }

    private void classifyPublications(LocalDate fromDate, Integer commissionId,
                                      List<Integer> authorIds, List<Integer> orgUnitIds,
                                      List<Integer> entityIds,
                                      List<DocumentPublicationType> publicationTypes,
                                      QuadConsumer<DocumentPublicationIndex, Integer, Commission, ArrayList<DocumentAssessmentClassification>> assessFunction) {
        int pageNumber = 0;
        int chunkSize = 500;
        boolean hasNextPage = true;

        Commission presetCommission = Objects.nonNull(commissionId)
            ? commissionService.findOneWithFetchedRelations(commissionId)
            : null;

        var batchClassifications = new ArrayList<DocumentAssessmentClassification>();

        while (hasNextPage) {
            List<DocumentPublicationIndex> chunk = searchService
                .runQuery(findAllDocumentPublicationsByFilters(fromDate.toString(),
                        publicationTypes.stream().map(Enum::name).toList(),
                        authorIds, orgUnitIds, entityIds),
                    PageRequest.of(pageNumber, chunkSize), DocumentPublicationIndex.class,
                    "document_publication").getContent();

            chunk.forEach(publicationIndex -> {
                if (publicationIndex.getType().equals(DocumentPublicationType.THESIS.name())) {
                    assessFunction.accept(publicationIndex,
                        publicationIndex.getThesisInstitutionId(), null, batchClassifications);
                } else if (Objects.nonNull(presetCommission)) {
                    assessFunction.accept(publicationIndex, null, presetCommission,
                        batchClassifications);
                } else {
                    publicationIndex.getOrganisationUnitIds().forEach(organisationUnitId ->
                        assessFunction.accept(publicationIndex, organisationUnitId, null,
                            batchClassifications));
                }
            });

            documentAssessmentClassificationRepository.saveAll(batchClassifications);
            batchClassifications.clear();

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }

        personAssessmentClassificationService.reindexPublicationPointsForAllResearchers();
    }

    private void assessThesis(DocumentPublicationIndex thesisIndex,
                              Integer defenceOrganisationUnitId, Commission presetCommission,
                              ArrayList<DocumentAssessmentClassification> batchedClassifications) {
        var commissions = new HashSet<Commission>();
        if (Objects.nonNull(defenceOrganisationUnitId)) {
            commissions.addAll(findCommissionInHierarchy(defenceOrganisationUnitId));
        }

        collectCommissionsIfPresent(
            thesisIndex.getOrganisationUnitIdsSpecified(),
            commissions
        );

        if (commissions.isEmpty()) {
            collectCommissionsIfPresent(
                thesisIndex.getOrganisationUnitIdsYearOfPublication(),
                commissions
            );
        }

        if (commissions.isEmpty()) {
            collectCommissionsIfPresent(
                thesisIndex.getOrganisationUnitIdsActive(),
                commissions
            );
        }

        if (commissions.isEmpty()) {
            log.info("No commission found for thesis with ID {}.",
                thesisIndex.getDatabaseId());
            return;
        }

        var assessmentCode = ClassificationPriorityMapping.getAssessmentCodeForThesisType(
            ThesisType.valueOf(thesisIndex.getPublicationType()));
        if (Objects.isNull(assessmentCode)) {
            log.info("No assessment code configured for thesis type {}.",
                thesisIndex.getPublicationType());
            return;
        }

        var assessmentClassification =
            assessmentClassificationService.readAssessmentClassificationByCode(assessmentCode);

        commissions.forEach(commission -> {
            documentAssessmentClassificationRepository.deleteByDocumentIdAndCommissionId(
                thesisIndex.getDatabaseId(), commission.getId(), false);


            handleClassification(
                assessmentClassification, commission,
                thesisIndex.getDatabaseId(), thesisIndex.getPublicationType(),
                thesisIndex.getYear(), batchedClassifications
            );
        });
    }

    private void collectCommissionsIfPresent(Collection<Integer> institutionIds,
                                             Set<Commission> commissions
    ) {
        if (CollectionOperations.containsValues(institutionIds)) {
            institutionIds.forEach(
                id -> commissions.addAll(findCommissionInHierarchy(id))
            );
        }
    }


    private void assessJournalPublication(DocumentPublicationIndex publicationIndex,
                                          Integer organisationUnitId, Commission presetCommission,
                                          ArrayList<DocumentAssessmentClassification> batchedClassifications) {
        if (Objects.isNull(publicationIndex.getJournalId()) ||
            !ClassificationPriorityMapping.meetsPageRequirements(publicationIndex)) {
            return;
        }

        List<Commission> commissions = Objects.nonNull(presetCommission)
            ? List.of(presetCommission)
            : findCommissionInHierarchy(organisationUnitId);

        if (commissions.isEmpty()) {
            log.info("No commission found for organisation unit {} or its hierarchy.",
                organisationUnitId);
            return;
        }

        commissions.forEach(commission -> {
            documentAssessmentClassificationRepository.deleteByDocumentIdAndCommissionId(
                publicationIndex.getDatabaseId(), commission.getId(), false);

            performPublicationAssessment(
                (year, classifications, commissionObj) -> {
                    var classificationList = publicationSeriesAssessmentClassificationRepository
                        .findAssessmentClassificationsForPublicationSeriesAndCommissionAndYear(
                            publicationIndex.getJournalId(), commission.getId(), year);

                    var manualClassification = classificationList.stream()
                        .filter(EntityAssessmentClassification::getManual)
                        .findFirst();

                    if (manualClassification.isPresent()) {
                        classifications.add(
                            new Pair<>(manualClassification.get().getAssessmentClassification(),
                                manualClassification.get().getClassificationReason()));
                    } else if (!classificationList.isEmpty()) {
                        ClassificationPriorityMapping.getBestJournalClassification(
                            classificationList.stream()
                                .map(EntityAssessmentClassification::getAssessmentClassification)
                                .toList()).ifPresent(bestClassification ->
                            classifications.add(
                                new Pair<>(bestClassification,
                                    classificationList.getFirst().getClassificationReason()))
                        );
                    } else {
                        handleRelationAssessments(commission,
                            (targetCommissionId) -> {
                                var relatedClassifications =
                                    publicationSeriesAssessmentClassificationRepository
                                        .findAssessmentClassificationsForPublicationSeriesAndCommissionAndYear(
                                            publicationIndex.getJournalId(),
                                            targetCommissionId, year
                                        );

                                return relatedClassifications.stream()
                                    .filter(EntityAssessmentClassification::getManual)
                                    .findFirst()
                                    .or(() -> relatedClassifications.stream().findFirst());
                            }
                        ).ifPresent(classifications::add);
                    }
                },
                publicationIndex.getYear(), publicationIndex.getDatabaseId(),
                publicationIndex.getPublicationType(), commission,
                List.of(
                    publicationIndex.getYear(),
                    publicationIndex.getYear() - 1,
                    publicationIndex.getYear() - 2
                ),
                batchedClassifications
            );
        });
    }

    private void assessProceedingsPublication(DocumentPublicationIndex proceedingsPublicationIndex,
                                              Integer organisationUnitId,
                                              Commission presetCommission,
                                              ArrayList<DocumentAssessmentClassification> batchedClassifications) {
        List<Commission> commissions = (presetCommission != null)
            ? List.of(presetCommission)
            : findCommissionInHierarchy(organisationUnitId);

        if (commissions.isEmpty()) {
            log.info("No commission found for organisation unit {} or its hierarchy.",
                organisationUnitId);
            return;
        }

        commissions.forEach(commission -> {
            documentAssessmentClassificationRepository.deleteByDocumentIdAndCommissionId(
                proceedingsPublicationIndex.getDatabaseId(), commission.getId(), false);

            performPublicationAssessment((year, classifications, commissionObj) ->
                {
                    var classification = eventAssessmentClassificationRepository
                        .findAssessmentClassificationsForEventAndCommissionAndYear(
                            proceedingsPublicationIndex.getEventId(), commission.getId(), year);

                    if (classification.isPresent()) {
                        classifications.add(
                            new Pair<>(classification.get().getAssessmentClassification(),
                                classification.get().getClassificationReason()));
                    } else {
                        handleRelationAssessments(commission,
                            (targetCommissionId) -> {
                                var assessmentClassification = eventAssessmentClassificationRepository
                                    .findAssessmentClassificationsForEventAndCommissionAndYear(
                                        proceedingsPublicationIndex.getEventId(), targetCommissionId,
                                        year)
                                    .orElse(null);
                                return Objects.nonNull(assessmentClassification) ? Optional.of(
                                    assessmentClassification) : Optional.empty();
                            }
                        ).ifPresent(classifications::add);
                    }
                },
                proceedingsPublicationIndex.getYear(), proceedingsPublicationIndex.getDatabaseId(),
                null, commission,
                List.of(proceedingsPublicationIndex.getYear()),
                batchedClassifications
            );
        });
    }

    @Override
    public ImaginaryPublicationAssessmentResponseDTO assessImaginaryJournalPublication(
        Integer journalId, Integer commissionId, Integer classificationYear, String researchArea,
        Integer authorCount, boolean isExperimental, boolean isTheoretical, boolean isSimulation,
        PublicationType publicationType) {
        if (Objects.isNull(publicationType)) {
            publicationType = JournalPublicationType.RESEARCH_ARTICLE;
        }

        var commission = commissionService.findOne(commissionId);

        return performPublicationAssessmentForImaginaryDocument(
            (year, classifications, commissionObj) -> {
                var classificationList = publicationSeriesAssessmentClassificationRepository
                    .findAssessmentClassificationsForPublicationSeriesAndCommissionAndYear(
                        journalId, commission.getId(), year);

                var manualClassification = classificationList.stream()
                    .filter(EntityAssessmentClassification::getManual)
                    .findFirst();

                if (manualClassification.isPresent()) {
                    classifications.add(
                        new Pair<>(manualClassification.get().getAssessmentClassification(),
                            manualClassification.get().getClassificationReason()));
                } else if (!classificationList.isEmpty()) {
                    ClassificationPriorityMapping.getBestJournalClassification(
                        classificationList.stream()
                            .map(EntityAssessmentClassification::getAssessmentClassification)
                            .toList()).ifPresent(bestClassification ->
                        classifications.add(
                            new Pair<>(bestClassification,
                                classificationList.getFirst().getClassificationReason()))
                    );
                } else {
                    handleRelationAssessments(commission,
                        (targetCommissionId) -> {
                            var relatedClassifications =
                                publicationSeriesAssessmentClassificationRepository
                                    .findAssessmentClassificationsForPublicationSeriesAndCommissionAndYear(
                                        journalId, targetCommissionId,
                                        year);

                            return relatedClassifications.stream()
                                .filter(EntityAssessmentClassification::getManual)
                                .findFirst()
                                .or(() -> relatedClassifications.stream().findFirst());
                        }
                    ).ifPresent(classifications::add);
                }
            },
            commission,
            List.of(classificationYear, classificationYear - 1, classificationYear - 2),
            researchArea, publicationType, authorCount, isExperimental, isTheoretical,
            isSimulation);
    }

    @Override
    public ImaginaryPublicationAssessmentResponseDTO assessImaginaryProceedingsPublication(
        Integer conferenceId, Integer commissionId, String researchArea, Integer authorCount,
        boolean isExperimental, boolean isTheoretical, boolean isSimulation,
        PublicationType publicationType) {
        if (Objects.isNull(publicationType)) {
            publicationType = ProceedingsPublicationType.REGULAR_FULL_ARTICLE;
        }

        var commission = commissionService.findOne(commissionId);
        var eventIndex = eventIndexRepository.findByDatabaseId(conferenceId);

        if (eventIndex.isEmpty()) {
            return new ImaginaryPublicationAssessmentResponseDTO();
        }

        return performPublicationAssessmentForImaginaryDocument(
            (year, classifications, commissionObj) -> {
                var classification = eventAssessmentClassificationRepository
                    .findAssessmentClassificationsForEventAndCommissionAndYear(
                        conferenceId, commission.getId(), year);

                if (classification.isPresent()) {
                    classifications.add(
                        new Pair<>(classification.get().getAssessmentClassification(),
                            classification.get().getClassificationReason()));
                } else {
                    handleRelationAssessments(commission,
                        (targetCommissionId) -> {
                            var assessmentClassification = eventAssessmentClassificationRepository
                                .findAssessmentClassificationsForEventAndCommissionAndYear(
                                    conferenceId, targetCommissionId,
                                    year)
                                .orElse(null);
                            return Objects.nonNull(assessmentClassification) ? Optional.of(
                                assessmentClassification) : Optional.empty();
                        }
                    ).ifPresent(classifications::add);
                }
            },
            commission,
            List.of(eventIndex.get().getDateSortable().getYear()),
            researchArea, publicationType, authorCount, isExperimental, isTheoretical,
            isSimulation);
    }

    private void performPublicationAssessment(
        TriConsumer<Integer, ArrayList<Pair<AssessmentClassification, Set<MultiLingualContent>>>, Commission> yearHandler,
        Integer classificationYear, Integer documentId, String documentPublicationType,
        Commission commission, List<Integer> yearsToConsider,
        ArrayList<DocumentAssessmentClassification> batchedClassifications) {
        var classifications =
            new ArrayList<Pair<AssessmentClassification, Set<MultiLingualContent>>>();

        yearsToConsider.forEach(year ->
            yearHandler.accept(year, classifications, commission)
        );

        if (!classifications.isEmpty()) {
            var bestClassification =
                ClassificationPriorityMapping.getClassificationBasedOnCriteria(classifications,
                    ResultCalculationMethod.BEST_VALUE);
            bestClassification.ifPresent((documentClassification) -> {
                handleClassification(
                    documentClassification.a, commission,
                    documentId, documentPublicationType,
                    classificationYear, batchedClassifications
                );
            });
        }
    }

    private ImaginaryPublicationAssessmentResponseDTO performPublicationAssessmentForImaginaryDocument(
        TriConsumer<Integer, List<Pair<AssessmentClassification, Set<MultiLingualContent>>>, Commission> yearHandler,
        Commission commission, List<Integer> yearsToConsider, String researchArea,
        PublicationType publicationType, Integer authorCount, boolean isExperimental,
        boolean isTheoretical, boolean isSimulation) {

        var assessmentResponse = new ImaginaryPublicationAssessmentResponseDTO();
        var classifications =
            new ArrayList<Pair<AssessmentClassification, Set<MultiLingualContent>>>();

        yearsToConsider.forEach(year -> yearHandler.accept(year, classifications, commission));

        if (classifications.isEmpty()) {
            return assessmentResponse;
        }

        ClassificationPriorityMapping
            .getClassificationBasedOnCriteria(classifications, ResultCalculationMethod.BEST_VALUE)
            .ifPresent(classification -> processClassification(
                classification, assessmentResponse, researchArea, authorCount, publicationType,
                isExperimental, isTheoretical, isSimulation
            ));

        return assessmentResponse;
    }

    private void processClassification(
        Pair<AssessmentClassification, Set<MultiLingualContent>> classification,
        ImaginaryPublicationAssessmentResponseDTO response,
        String researchArea, Integer authorCount, PublicationType publicationType,
        boolean isExperimental, boolean isTheoretical, boolean isSimulation) {

        var mappedCode = ClassificationPriorityMapping.getImaginaryDocClassificationCodeBasedOnCode(
            classification.a.getCode(), publicationType);

        if (Objects.isNull(mappedCode)) {
            return;
        }

        response.setAssessmentCode(mappedCode);
        response.setAssessmentReason(
            MultilingualContentConverter.getMultilingualContentDTO(classification.b));

        var pointsRuleEngine = new AssessmentPointsRuleEngine();
        var scalingRuleEngine = new AssessmentPointsScalingRuleEngine();

        applyIndicatorScalingRule(scalingRuleEngine, isExperimental, isTheoretical, isSimulation,
            publicationType);

        var rawPoints = pointsRuleEngine.serbianPointsRulebook2025(researchArea, mappedCode);
        response.setRawPoints(rawPoints);
        response.setRawPointsReason(getPointsReason(mappedCode, researchArea, rawPoints));

        var scaledPoints =
            scalingRuleEngine.serbianScalingRulebook2025(authorCount, mappedCode, rawPoints);
        response.setScaledPoints(scaledPoints);
        response.setScaledPointsReason(
            MultilingualContentConverter.getMultilingualContentDTO(
                scalingRuleEngine.getReasoningProcess()));
    }

    private void applyIndicatorScalingRule(AssessmentPointsScalingRuleEngine scalingRuleEngine,
                                           boolean isExperimental, boolean isTheoretical,
                                           boolean isSimulation, PublicationType publicationType) {

        String indicatorCode = null;

        if (isExperimental) {
            indicatorCode = "isExperimental";
        } else if (isTheoretical) {
            indicatorCode = "isTheoretical";
        } else if (isSimulation) {
            indicatorCode = "isSimulation";
        }

        scalingRuleEngine.setPublicationType(((Enum<?>) publicationType).name());

        if (Objects.nonNull(indicatorCode)) {
            var indicator = new DocumentIndicator();
            indicator.setIndicator(indicatorRepository.findByCode(indicatorCode));
            scalingRuleEngine.setCurrentEntityIndicators(List.of(indicator));
        } else {
            scalingRuleEngine.setCurrentEntityIndicators(new ArrayList<>());
        }
    }

    private List<MultilingualContentDTO> getPointsReason(String mappedCode, String researchArea,
                                                         double points) {
        return MultilingualContentConverter.getMultilingualContentDTO(
            AssessmentRulesConfigurationLoader.getRuleDescription(
                "pointRules", "generalPointRule", mappedCode,
                ResearchAreasConfigurationLoader.fetchAssessmentResearchAreaNameByCode(
                    researchArea), points));
    }

    private List<Commission> findCommissionInHierarchy(Integer organisationUnitId) {
        List<Commission> commission;
        do {
            commission = userService.findCommissionForOrganisationUnitId(organisationUnitId);
            if (commission.isEmpty()) {
                var superOU = organisationUnitsRelationRepository.getSuperOU(organisationUnitId);
                if (superOU.isPresent()) {
                    organisationUnitId = superOU.get().getId();
                } else {
                    break;
                }
            }
        } while (commission.isEmpty());
        return commission;
    }

    private void handleClassification(AssessmentClassification classification,
                                      Commission commission, Integer documentId,
                                      String documentPublicationType, Integer classificationYear,
                                      ArrayList<DocumentAssessmentClassification> batchedClassifications) {
        var mappedCode = ClassificationPriorityMapping.getDocClassificationCodeBasedOnCode(
            classification.getCode(), documentId);
        if (mappedCode.isEmpty()) {
            return;
        }

        if (Objects.nonNull(documentPublicationType) &&
            !ClassificationPriorityMapping.canPublicationTypeBeClassifiedAsCode(
                documentPublicationType, mappedCode.get())) {
            return;
        }

        var documentClassification = assessmentClassificationService
            .readAssessmentClassificationByCode(mappedCode.get());
        saveDocumentClassification(documentClassification, commission, documentId,
            classificationYear, batchedClassifications);
    }

    private Optional<Pair<AssessmentClassification, Set<MultiLingualContent>>> handleRelationAssessments(
        Commission commission,
        Function<Integer, Optional<EntityAssessmentClassification>> classificationFinder) {
        var sortedRelations = commission.getRelations().stream()
            .sorted(Comparator.comparingInt(CommissionRelation::getPriority))
            .toList();

        for (var relation : sortedRelations) {
            var respectedClassification =
                respectRelationAssessment(relation, classificationFinder);
            if (respectedClassification.isPresent()) {
                return Optional.of(
                    new Pair<>(respectedClassification.get().a, respectedClassification.get().b));
            }
        }

        return Optional.empty();
    }

    private Optional<Pair<AssessmentClassification, Set<MultiLingualContent>>> respectRelationAssessment(
        CommissionRelation commissionRelation,
        Function<Integer, Optional<EntityAssessmentClassification>> classificationFinder) {
        var classifications =
            new ArrayList<Pair<AssessmentClassification, Set<MultiLingualContent>>>();

        for (var targetCommission : commissionRelation.getTargetCommissions()) {
            var foundClassification = classificationFinder.apply(targetCommission.getId());
            if (foundClassification.isPresent()) {
                if (foundClassification.get().getManual()) {
                    return Optional.of(
                        new Pair<>(foundClassification.get().getAssessmentClassification(),
                            foundClassification.get().getClassificationReason()));
                }
                classifications.add(
                    new Pair<>(foundClassification.get().getAssessmentClassification(),
                        foundClassification.get().getClassificationReason()));
            }
        }

        if (classifications.isEmpty()) {
            return Optional.empty();
        }

        return ClassificationPriorityMapping.getClassificationBasedOnCriteria(classifications,
            commissionRelation.getResultCalculationMethod());
    }

    public Query findAllDocumentPublicationsByFilters(
        String date, List<String> types,
        List<Integer> authorIds,
        List<Integer> organisationUnitIds,
        List<Integer> publishedInIds) {

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(sb -> sb.range(r -> r.field("last_edited").gt(JsonData.of(date))));
            b.must(sb -> sb.terms(t -> t.field("type")
                .terms(terms -> terms.value(types.stream().map(FieldValue::of).toList()))));
            b.must(sb -> sb.terms(t -> t
                .field("publication_type")
                .terms(terms -> terms
                    .value(types.contains(DocumentPublicationType.JOURNAL_PUBLICATION.name()) ?
                        List.of(
                            FieldValue.of("REVIEW_ARTICLE"),
                            FieldValue.of("RESEARCH_ARTICLE"),
                            FieldValue.of("COMMENT")
                        ) :
                        List.of(
                            FieldValue.of("REGULAR_FULL_ARTICLE"),
                            FieldValue.of("INVITED_FULL_ARTICLE"),
                            FieldValue.of("REGULAR_ABSTRACT_ARTICLE"),
                            FieldValue.of("INVITED_ABSTRACT_ARTICLE"),
                            FieldValue.of("SCIENTIFIC_CRITIC"),
                            FieldValue.of("POLEMICS")
                        )
                    )
                )
            ));

            if (!authorIds.isEmpty()) {
                var authorIdTerms = new TermsQueryField.Builder()
                    .value(authorIds.stream().map(FieldValue::of).toList())
                    .build();
                b.must(sb -> sb.terms(t -> t.field("author_ids").terms(authorIdTerms)));
            }

            if (!organisationUnitIds.isEmpty()) {
                var orgUnitIdTerms = new TermsQueryField.Builder()
                    .value(organisationUnitIds.stream().map(FieldValue::of).toList())
                    .build();
                b.must(sb -> sb.terms(t -> t.field("organisation_unit_ids").terms(orgUnitIdTerms)));
            }

            if (!publishedInIds.isEmpty()) {
                var publishedInIdTerms = new TermsQueryField.Builder()
                    .value(publishedInIds.stream().map(FieldValue::of).toList())
                    .build();
                b.must(sb -> sb.terms(t -> t.field(
                    types.contains(DocumentPublicationType.JOURNAL_PUBLICATION.name()) ?
                        "journal_id" : "event_id").terms(publishedInIdTerms)));
            }

            return b;
        })))._toQuery();
    }

    private void saveDocumentClassification(AssessmentClassification assessmentClassification,
                                            Commission commission, Integer documentId,
                                            Integer classificationYear,
                                            ArrayList<DocumentAssessmentClassification> batchedClassifications) {
        var documentClassification = new DocumentAssessmentClassification();
        documentClassification.setTimestamp(LocalDateTime.now());
        documentClassification.setCommission(commission);
        documentClassification.setAssessmentClassification(assessmentClassification);
        documentClassification.setClassificationYear(classificationYear);
        documentClassification.setDocument(documentRepository.getReferenceById(documentId));

        if (Objects.nonNull(batchedClassifications)) {
            batchedClassifications.add(documentClassification);
        } else {
            documentAssessmentClassificationRepository.save(documentClassification);
        }

        documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId)
            .ifPresent(documentIndex -> {
                if (!documentIndex.getAssessedBy().contains(commission.getId())) {
                    documentIndex.getAssessedBy().add(commission.getId());
                }

                var assessmentTriple = new Triple<>(commission.getId(),
                    documentClassification.getAssessmentClassification().getCode(), false);

                if (!documentIndex.getCommissionAssessments().contains(assessmentTriple)) {
                    documentIndex.getCommissionAssessments().add(assessmentTriple);
                }

                assessmentTriple = new Triple<>(commission.getId(),
                    documentClassification.getAssessmentClassification().getCode().substring(0, 2) +
                        "0", false);

                if (!documentIndex.getCommissionAssessmentGroups().contains(assessmentTriple)) {
                    documentIndex.getCommissionAssessmentGroups().add(assessmentTriple);
                }

                documentPublicationIndexRepository.save(documentIndex);
            });
    }

    private void setCommonFields(DocumentAssessmentClassification documentClassification,
                                 DocumentAssessmentClassificationDTO dto) {
        documentClassification.setTimestamp(LocalDateTime.now());
        documentClassification.setManual(true);
        documentClassification.setAssessmentClassification(
            assessmentClassificationService.findOne(dto.getAssessmentClassificationId()));
    }

    @Scheduled(cron = "${assessment.document.notify-period}")
    protected void sendNotificationsToCommissions() {
        userService.findAllCommissionUsers().forEach(user -> {
            var totalAndInstitutionCount =
                documentPublicationService.getDocumentCountsBelongingToInstitution(
                    user.getOrganisationUnit().getId());

            var totalClassifiedAndInstitutionCount =
                documentPublicationService.getAssessedDocumentCountsForCommission(
                    user.getOrganisationUnit().getId(), user.getCommission().getId());

            notificationService.cleanPastNotificationsOfType(user.getId(),
                NotificationType.NEW_PUBLICATIONS_TO_ASSESS);
            notificationService.createNotification(
                NotificationFactory.contructNewPublicationsForAssessmentNotification(
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
