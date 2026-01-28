package rs.teslaris.assessment.service.impl.classification;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.assessment.dto.EnrichedResearcherAssessmentResponseDTO;
import rs.teslaris.assessment.dto.ResearcherAssessmentResponseDTO;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.model.AssessmentMeasure;
import rs.teslaris.assessment.model.AssessmentResearchArea;
import rs.teslaris.assessment.model.indicator.DocumentIndicator;
import rs.teslaris.assessment.repository.AssessmentResearchAreaRepository;
import rs.teslaris.assessment.repository.AssessmentRulebookRepository;
import rs.teslaris.assessment.repository.classification.EntityAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.classification.PersonAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.indicator.DocumentIndicatorRepository;
import rs.teslaris.assessment.ruleengine.AssessmentPointsRuleEngine;
import rs.teslaris.assessment.ruleengine.AssessmentPointsScalingRuleEngine;
import rs.teslaris.assessment.service.interfaces.CommissionService;
import rs.teslaris.assessment.service.interfaces.classification.AssessmentClassificationService;
import rs.teslaris.assessment.service.interfaces.classification.PersonAssessmentClassificationService;
import rs.teslaris.assessment.util.ClassificationPriorityMapping;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexmodel.PrizeIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.indexrepository.PrizeIndexRepository;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.repository.commontypes.ResearchAreaRepository;
import rs.teslaris.core.repository.institution.OrganisationUnitsRelationRepository;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.core.util.functional.Triple;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.CollectionOperations;
import rs.teslaris.core.util.session.SessionUtil;

@Service
@Slf4j
@Traceable
public class PersonAssessmentClassificationServiceImpl
    extends EntityAssessmentClassificationServiceImpl implements
    PersonAssessmentClassificationService {

    private final PersonAssessmentClassificationRepository personAssessmentClassificationRepository;

    private final SearchService<PersonIndex> searchService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final DocumentIndicatorRepository documentIndicatorRepository;

    private final AssessmentResearchAreaRepository assessmentResearchAreaRepository;

    private final AssessmentRulebookRepository assessmentRulebookRepository;

    private final PersonIndexRepository personIndexRepository;

    private final UserRepository userRepository;

    private final CitationService citationService;

    private final InvolvementRepository involvementRepository;

    private final OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    private final ResearchAreaRepository researchAreaRepository;

    private final PrizeIndexRepository prizeIndexRepository;

    private final int CHUNK_SIZE = 1000;


    @Autowired
    public PersonAssessmentClassificationServiceImpl(
        AssessmentClassificationService assessmentClassificationService,
        CommissionService commissionService, DocumentPublicationService documentPublicationService,
        ConferenceService conferenceService,
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        ApplicationEventPublisher applicationEventPublisher,
        PersonAssessmentClassificationRepository personAssessmentClassificationRepository,
        SearchService<PersonIndex> searchService,
        DocumentPublicationIndexRepository documentPublicationIndexRepository,
        DocumentIndicatorRepository documentIndicatorRepository,
        AssessmentResearchAreaRepository assessmentResearchAreaRepository,
        AssessmentRulebookRepository assessmentRulebookRepository,
        PersonIndexRepository personIndexRepository, UserRepository userRepository,
        CitationService citationService, InvolvementRepository involvementRepository,
        OrganisationUnitsRelationRepository organisationUnitsRelationRepository,
        ResearchAreaRepository researchAreaRepository, PrizeIndexRepository prizeIndexRepository) {
        super(assessmentClassificationService, commissionService, documentPublicationService,
            conferenceService, applicationEventPublisher, entityAssessmentClassificationRepository);
        this.personAssessmentClassificationRepository = personAssessmentClassificationRepository;
        this.searchService = searchService;
        this.documentPublicationIndexRepository = documentPublicationIndexRepository;
        this.documentIndicatorRepository = documentIndicatorRepository;
        this.assessmentResearchAreaRepository = assessmentResearchAreaRepository;
        this.assessmentRulebookRepository = assessmentRulebookRepository;
        this.personIndexRepository = personIndexRepository;
        this.userRepository = userRepository;
        this.citationService = citationService;
        this.involvementRepository = involvementRepository;
        this.organisationUnitsRelationRepository = organisationUnitsRelationRepository;
        this.researchAreaRepository = researchAreaRepository;
        this.prizeIndexRepository = prizeIndexRepository;
    }

    @Override
    @Transactional
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPerson(
        Integer personId) {
        return personAssessmentClassificationRepository.findAssessmentClassificationsForPerson(
                personId).stream().map(EntityAssessmentClassificationConverter::toDTO)
            .sorted((a, b) -> b.year().compareTo(a.year()))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrichedResearcherAssessmentResponseDTO> assessResearchers(Integer commissionId,
                                                                           List<Integer> researcherIds,
                                                                           Integer startYear,
                                                                           Integer endYear,
                                                                           Integer topLevelInstitutionId) {
        var commission = commissionService.findOneWithFetchedRelations(commissionId);
        var organisationUnit = userRepository.findOUForCommission(commissionId)
            .orElseThrow(() -> new NotFoundException("commissionNotBoundToOUMessage"));
        var subOUs =
            organisationUnitsRelationRepository.getSubOUsRecursive(organisationUnit.getId());
        subOUs.add(organisationUnit.getId());

        var subOUsForTopLevelInstitution = new ArrayList<Integer>();
        if (Objects.nonNull(topLevelInstitutionId)) {
            subOUsForTopLevelInstitution.addAll(
                organisationUnitsRelationRepository.getSubOUsRecursive(topLevelInstitutionId));
            subOUsForTopLevelInstitution.add(topLevelInstitutionId);
        }

        var assessmentMeasures = assessmentRulebookRepository
            .readAssessmentMeasuresForRulebook(Pageable.unpaged(), findDefaultRulebookId())
            .getContent();

        var pointsRuleEngine = new AssessmentPointsRuleEngine(researchAreaRepository);
        var scalingRuleEngine = new AssessmentPointsScalingRuleEngine();

        var responses = new ArrayList<EnrichedResearcherAssessmentResponseDTO>();

        for (int pageNumber = 0; ; pageNumber++) {
            List<PersonIndex> chunk = searchService
                .runQuery(findAllPersonsByFilters(researcherIds, List.of(organisationUnit.getId())),
                    PageRequest.of(pageNumber, CHUNK_SIZE), PersonIndex.class, "person")
                .getContent();

            if (chunk.isEmpty()) {
                break;
            }

            chunk.forEach(personIndex -> processPerson(
                personIndex, commission, organisationUnit, assessmentMeasures, pointsRuleEngine,
                scalingRuleEngine, responses, startYear, endYear, subOUs,
                subOUsForTopLevelInstitution
            ));
        }

        return responses;
    }

    private void processPerson(PersonIndex personIndex,
                               Commission commission,
                               OrganisationUnit organisationUnit,
                               List<AssessmentMeasure> assessmentMeasures,
                               AssessmentPointsRuleEngine pointsRuleEngine,
                               AssessmentPointsScalingRuleEngine scalingRuleEngine,
                               List<EnrichedResearcherAssessmentResponseDTO> responses,
                               Integer startYear, Integer endYear, List<Integer> subOUs,
                               List<Integer> subOUsForTopLevelInstitution) {
        var assessmentResult = new EnrichedResearcherAssessmentResponseDTO();
        assessmentResult.setCommissionId(commission.getId());
        assessmentResult.setCommissionDescription(
            MultilingualContentConverter.getMultilingualContentDTO(commission.getDescription()));
        assessmentResult.setPersonName(personIndex.getName());
        assessmentResult.setInstitutionName(
            MultilingualContentConverter.getMultilingualContentDTO(organisationUnit.getName()));
        assessmentResult.setFromYear(startYear);
        assessmentResult.setToYear(endYear);

        var employments =
            involvementRepository.findActiveEmploymentsForPersonAndInstitutions(subOUs,
                personIndex.getDatabaseId());
        if (employments.isEmpty()) {
            log.info("Person with ID {} is just a candidate, skipping...",
                personIndex.getDatabaseId());
            return;
        }

        assessmentResult.setPersonPosition(employments.getFirst().getEmploymentPosition());

        processResearcher(personIndex, commission, assessmentMeasures, pointsRuleEngine,
            scalingRuleEngine, assessmentResult, startYear, endYear, subOUsForTopLevelInstitution);

        responses.add(assessmentResult);
    }

    @Override
    @Transactional
    public List<ResearcherAssessmentResponseDTO> assessSingleResearcher(Integer researcherId,
                                                                        LocalDate startDate,
                                                                        LocalDate endDate) {
        var assessmentResponse = new ArrayList<ResearcherAssessmentResponseDTO>();

        var index = personIndexRepository.findByDatabaseId(researcherId);
        if (index.isEmpty()) {
            return assessmentResponse;
        }

        var assessmentMeasures = assessmentRulebookRepository
            .readAssessmentMeasuresForRulebook(Pageable.unpaged(), findDefaultRulebookId())
            .getContent();

        var institutionIds = index.get().getEmploymentInstitutionsIdHierarchy();
        var commissions = commissionService.findCommissionsWithRelations(
            userRepository.findUserCommissionForOrganisationUnits(institutionIds));

        commissions.forEach(commission -> {
            var pointsRuleEngine = new AssessmentPointsRuleEngine(researchAreaRepository);
            var scalingRuleEngine = new AssessmentPointsScalingRuleEngine();

            var assessmentResult = new EnrichedResearcherAssessmentResponseDTO();
            assessmentResult.setCommissionId(commission.getId());
            assessmentResult.setCommissionDescription(
                MultilingualContentConverter.getMultilingualContentDTO(
                    commission.getDescription()));

            processResearcher(index.get(), commission, assessmentMeasures,
                pointsRuleEngine, scalingRuleEngine, assessmentResult, startDate.getYear(),
                endDate.getYear(), Collections.emptyList());
            assessmentResponse.add(assessmentResult);
        });

        return assessmentResponse;
    }

    @Override
    @Transactional
    public synchronized void reindexPublicationPointsForAllResearchers() {
        var assessmentMeasures = loadAssessmentMeasures();

        int pageNumber = 0;
        boolean hasNextPage = true;

        while (hasNextPage) {
            var persons =
                personIndexRepository.findAll(PageRequest.of(pageNumber, CHUNK_SIZE)).getContent();

            persons.forEach(personIndex ->
                reindexPublicationPointsForResearcher(personIndex, assessmentMeasures)
            );

            pageNumber++;
            hasNextPage = persons.size() == CHUNK_SIZE;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void reindexPublicationPointsForResearcher(PersonIndex index,
                                                      List<AssessmentMeasure> assessmentMeasures) {
        var commissionResearchAreas = resolveCommissionResearchAreas(index);

        processPublicationsInChunks(index, commissionResearchAreas, assessmentMeasures);
    }

    private List<AssessmentMeasure> loadAssessmentMeasures() {
        return assessmentRulebookRepository
            .readAssessmentMeasuresForRulebook(Pageable.unpaged(), findDefaultRulebookId())
            .getContent();
    }

    private List<Pair<Integer, AssessmentResearchArea>> resolveCommissionResearchAreas(
        PersonIndex index) {
        var institutionIds = index.getEmploymentInstitutionsIdHierarchy();
        var commissions = commissionService.findCommissionsWithRelations(
            userRepository.findUserCommissionForOrganisationUnits(institutionIds));

        var commissionResearchArea = new ArrayList<Pair<Integer, AssessmentResearchArea>>();
        commissions.forEach(commission -> {
            var researchArea = getResearchArea(index.getDatabaseId(), commission);
            researchArea.ifPresent(assessmentResearchArea -> {
                if (commission.getRecognisedResearchAreas()
                    .contains(assessmentResearchArea.getResearchAreaCode())) {
                    commissionResearchArea.add(
                        new Pair<>(commission.getId(), assessmentResearchArea));
                }
            });
        });

        return commissionResearchArea;
    }

    private void processPublicationsInChunks(
        PersonIndex index,
        List<Pair<Integer, AssessmentResearchArea>> commissionResearchAreas,
        List<AssessmentMeasure> assessmentMeasures
    ) {
        int pageNumber = 0;
        boolean hasNextPage = true;

        List<Integer> publicationIdsBatch = new ArrayList<>();
        var pointsRuleEngine = new AssessmentPointsRuleEngine(researchAreaRepository);
        var scalingRuleEngine = new AssessmentPointsScalingRuleEngine();

        while (hasNextPage) {
            var publications = documentPublicationIndexRepository
                .findAssessedByAuthorIds(index.getDatabaseId(),
                    PageRequest.of(pageNumber, CHUNK_SIZE))
                .getContent();

            publications.forEach(pub ->
                publicationIdsBatch.add(pub.getDatabaseId())
            );

            Map<Integer, List<DocumentIndicator>> indicatorsByDocumentId =
                documentIndicatorRepository.findIndicatorsForDocumentsAndIndicatorAccessLevel(
                        publicationIdsBatch, AccessLevel.ADMIN_ONLY).stream()
                    .collect(Collectors.groupingBy(
                        di -> di.getDocument().getId()
                    ));

            publications.forEach(
                publication -> updatePublicationAssessments(
                    publication, index, commissionResearchAreas, assessmentMeasures,
                    indicatorsByDocumentId.getOrDefault(publication.getDatabaseId(),
                        Collections.emptyList()), pointsRuleEngine, scalingRuleEngine
                )
            );

            pageNumber++;
            hasNextPage = publications.size() == CHUNK_SIZE;
        }
    }

    private void updatePublicationAssessments(
        DocumentPublicationIndex publication,
        PersonIndex index,
        List<Pair<Integer, AssessmentResearchArea>> commissionResearchAreas,
        List<AssessmentMeasure> assessmentMeasures,
        List<DocumentIndicator> indicators,
        AssessmentPointsRuleEngine pointsRuleEngine,
        AssessmentPointsScalingRuleEngine scalingRuleEngine
    ) {
        if (commissionResearchAreas.isEmpty()) {
            publication.getAssessmentPoints().removeIf(
                triple -> triple.a.equals(index.getDatabaseId())
            );
        }

        commissionResearchAreas.forEach(commissionResearchArea -> {
            publication.getCommissionAssessments().stream()
                .filter(assessment -> assessment.a.equals(commissionResearchArea.a))
                .findFirst()
                .ifPresent(assessment -> {
                    var applicableMeasure = findApplicableMeasure(assessmentMeasures, assessment.b);
                    if (applicableMeasure.isEmpty()) {
                        return;
                    }

                    var points = getPointsForPublication(
                        publication, pointsRuleEngine, scalingRuleEngine,
                        commissionResearchArea.b, assessment.b, applicableMeasure.get(),
                        indicators);

                    updatePublicationPoints(publication, index.getDatabaseId(),
                        commissionResearchArea.a, points);
                });
        });

        documentPublicationIndexRepository.save(publication);
    }

    private synchronized void updatePublicationPoints(
        DocumentPublicationIndex publication, Integer researcherId,
        Integer commissionId, Double points
    ) {
        publication.getAssessmentPoints().removeIf(
            triple -> triple.a.equals(researcherId) && triple.b.equals(commissionId));
        publication.getAssessmentPoints().add(
            new Triple<>(researcherId, commissionId, points));
    }

    private void processResearcher(PersonIndex personIndex, Commission commission,
                                   List<AssessmentMeasure> assessmentMeasures,
                                   AssessmentPointsRuleEngine pointsRuleEngine,
                                   AssessmentPointsScalingRuleEngine scalingRuleEngine,
                                   EnrichedResearcherAssessmentResponseDTO assessmentResult,
                                   int startYear, int endYear,
                                   List<Integer> subOUsForTopLevelInstitution) {
        var researchArea = getResearchArea(personIndex.getDatabaseId(), commission);

        researchArea.ifPresent(
            assessmentResearchArea -> {
                if (!commission.getRecognisedResearchAreas()
                    .contains(assessmentResearchArea.getResearchAreaCode())) {
                    return;
                }

                assessResearcherPublications(personIndex, commission, assessmentMeasures,
                    pointsRuleEngine, scalingRuleEngine,
                    assessmentResearchArea, startYear, endYear,
                    assessmentResult, subOUsForTopLevelInstitution);

                if (!CollectionOperations.containsValues(subOUsForTopLevelInstitution)) {
                    assessResearcherPrizes(personIndex, commission, assessmentMeasures,
                        pointsRuleEngine, scalingRuleEngine, assessmentResearchArea,
                        startYear, endYear, assessmentResult);
                }
            });
    }

    private Optional<AssessmentResearchArea> getResearchArea(Integer personId,
                                                             Commission commission) {
        return assessmentResearchAreaRepository
            .findForPersonIdAndCommissionId(personId, commission.getId())
            .or(() -> assessmentResearchAreaRepository.findForPersonId(personId));
    }

    private void assessResearcherPublications(
        PersonIndex personIndex,
        Commission commission,
        List<AssessmentMeasure> measures,
        AssessmentPointsRuleEngine pointsRuleEngine,
        AssessmentPointsScalingRuleEngine scalingRuleEngine,
        AssessmentResearchArea assessmentResearchArea,
        int startYear, int endYear,
        EnrichedResearcherAssessmentResponseDTO assessmentResult,
        List<Integer> subOUsForTopLevelInstitution) {

        var isUserLoggedIn = SessionUtil.isUserLoggedIn();
        List<Integer> publicationIdsBatch = new ArrayList<>();

        int pageNumber = 0;
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<DocumentPublicationIndex> publications =
                fetchPublications(personIndex, pageNumber, CHUNK_SIZE, startYear, endYear);

            publications.forEach(pub ->
                publicationIdsBatch.add(pub.getDatabaseId())
            );

            Map<Integer, List<DocumentIndicator>> indicatorsByDocumentId =
                documentIndicatorRepository.findIndicatorsForDocumentsAndIndicatorAccessLevel(
                        publicationIdsBatch, AccessLevel.ADMIN_ONLY).stream()
                    .collect(Collectors.groupingBy(
                        di -> di.getDocument().getId()
                    ));

            publications.forEach(publication ->
                processPublication(publication, commission, measures, pointsRuleEngine,
                    scalingRuleEngine, assessmentResearchArea, personIndex, assessmentResult,
                    subOUsForTopLevelInstitution,
                    indicatorsByDocumentId.getOrDefault(publication.getDatabaseId(),
                        Collections.emptyList()), isUserLoggedIn));

            pageNumber++;
            hasNextPage = publications.size() == CHUNK_SIZE;
        }
    }

    private void assessResearcherPrizes(
        PersonIndex personIndex,
        Commission commission,
        List<AssessmentMeasure> measures,
        AssessmentPointsRuleEngine pointsRuleEngine,
        AssessmentPointsScalingRuleEngine scalingRuleEngine,
        AssessmentResearchArea assessmentResearchArea,
        int startYear, int endYear,
        EnrichedResearcherAssessmentResponseDTO assessmentResult) {

        var isUserLoggedIn = SessionUtil.isUserLoggedIn();

        int pageNumber = 0;
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<PrizeIndex> prizes =
                fetchPrizes(personIndex, pageNumber, CHUNK_SIZE, startYear, endYear);

            prizes.forEach(prize ->
                processPrize(prize, commission, measures, pointsRuleEngine,
                    scalingRuleEngine, assessmentResearchArea, personIndex, assessmentResult,
                    isUserLoggedIn));

            pageNumber++;
            hasNextPage = prizes.size() == CHUNK_SIZE;
        }
    }

    private List<DocumentPublicationIndex> fetchPublications(PersonIndex personIndex,
                                                             int pageNumber, int chunkSize,
                                                             int startYear, int endYear) {
        return documentPublicationIndexRepository.findByAuthorIdsAndYearBetween(
            personIndex.getDatabaseId(),
            startYear, endYear, PageRequest.of(pageNumber, chunkSize)).getContent();
    }

    private List<PrizeIndex> fetchPrizes(PersonIndex personIndex,
                                         int pageNumber, int chunkSize,
                                         int startYear, int endYear) {
        return prizeIndexRepository.findByPersonIdAndDateBetween(
            personIndex.getDatabaseId(),
            LocalDate.of(startYear, 1, 1), LocalDate.of(endYear, 12, 31),
            PageRequest.of(pageNumber, chunkSize)).getContent();
    }

    private void processPublication(
        DocumentPublicationIndex publication,
        Commission commission,
        List<AssessmentMeasure> measures,
        AssessmentPointsRuleEngine pointsRuleEngine,
        AssessmentPointsScalingRuleEngine scalingRuleEngine,
        AssessmentResearchArea researchArea, PersonIndex personIndex,
        EnrichedResearcherAssessmentResponseDTO assessmentResult,
        List<Integer> subOUsForTopLevelInstitution,
        List<DocumentIndicator> indicators,
        boolean isUserLoggedIn) {

        var assessment = publication.getCommissionAssessments().stream()
            .filter(ca -> ca.a.equals(commission.getId())).findFirst();

        if (assessment.isEmpty()) {
            return;
        }

        var applicableMeasureOpt = findApplicableMeasure(measures, assessment.get().b);
        if (applicableMeasureOpt.isEmpty()) {
            return;
        }

        var applicableMeasure = applicableMeasureOpt.get();

        var points = getPointsForPublication(publication, pointsRuleEngine, scalingRuleEngine,
            researchArea, assessment.get().b, applicableMeasure, indicators);

        log.info("{} more points for {}", points, personIndex.getName());

        assessmentResult.getPublicationsPerCategory()
            .computeIfAbsent(ClassificationPriorityMapping.getCodeDisplayValue(assessment.get().b),
                k -> new ArrayList<>())
            .add(new Triple<>(
                citationService.craftCitationInGivenStyle("apa", publication,
                    LanguageAbbreviations.ENGLISH),
                isUserLoggedIn ? points : 0,
                publication.getDatabaseId()));

        if (!subOUsForTopLevelInstitution.isEmpty()) {
            populateTopLevelBelongingInformation(publication, assessmentResult,
                subOUsForTopLevelInstitution);
        }
    }

    private void processPrize(
        PrizeIndex prize,
        Commission commission,
        List<AssessmentMeasure> measures,
        AssessmentPointsRuleEngine pointsRuleEngine,
        AssessmentPointsScalingRuleEngine scalingRuleEngine,
        AssessmentResearchArea researchArea, PersonIndex personIndex,
        EnrichedResearcherAssessmentResponseDTO assessmentResult,
        boolean isUserLoggedIn) {

        var assessment = prize.getCommissionAssessments().stream()
            .filter(ca -> ca.a.equals(commission.getId())).findFirst();

        if (assessment.isEmpty()) {
            return;
        }

        var applicableMeasureOpt = findApplicableMeasure(measures, assessment.get().b);
        if (applicableMeasureOpt.isEmpty()) {
            return;
        }

        var applicableMeasure = applicableMeasureOpt.get();

        double basePoints = invokeRule(AssessmentPointsRuleEngine.class,
            applicableMeasure.getPointRule(), pointsRuleEngine, researchArea.getResearchAreaCode(),
            Objects.nonNull(researchArea.getResearchSubAreaIds()) ?
                researchArea.getResearchSubAreaIds() : Collections.emptySet(), assessment.get().b);

        double scaledPoints = invokeRule(AssessmentPointsScalingRuleEngine.class,
            applicableMeasure.getScalingRule(), scalingRuleEngine,
            1, assessment.get().b, basePoints);

        log.info("{} more points for {}", scaledPoints, personIndex.getName());

        assessmentResult.getPublicationsPerCategory()
            .computeIfAbsent(ClassificationPriorityMapping.getCodeDisplayValue(assessment.get().b),
                k -> new ArrayList<>())
            .add(new Triple<>(
                prize.getTitleOther(),
                isUserLoggedIn ? scaledPoints : 0,
                prize.getDatabaseId()));
    }

    private double getPointsForPublication(DocumentPublicationIndex publication,
                                           AssessmentPointsRuleEngine pointsRuleEngine,
                                           AssessmentPointsScalingRuleEngine scalingRuleEngine,
                                           AssessmentResearchArea researchArea,
                                           String classificationCode,
                                           AssessmentMeasure applicableMeasure,
                                           List<DocumentIndicator> indicators) {
        pointsRuleEngine.setPublicationType(publication.getPublicationType());
        scalingRuleEngine.setCurrentEntityIndicators(indicators);
        scalingRuleEngine.setPublicationType(publication.getPublicationType());

        return calculatePoints(applicableMeasure, pointsRuleEngine, scalingRuleEngine,
            researchArea, classificationCode, publication);
    }

    private double calculatePoints(
        AssessmentMeasure measure,
        AssessmentPointsRuleEngine pointsRuleEngine,
        AssessmentPointsScalingRuleEngine scalingRuleEngine,
        AssessmentResearchArea researchArea,
        String classificationCode,
        DocumentPublicationIndex publication) {

        double basePoints = invokeRule(AssessmentPointsRuleEngine.class, measure.getPointRule(),
            pointsRuleEngine, researchArea.getResearchAreaCode(),
            Objects.nonNull(researchArea.getResearchSubAreaIds()) ?
                researchArea.getResearchSubAreaIds() : Collections.emptySet(), classificationCode);

        return invokeRule(AssessmentPointsScalingRuleEngine.class, measure.getScalingRule(),
            scalingRuleEngine, publication.getAuthorIds().size(), classificationCode, basePoints);
    }

    private Double invokeRule(Class<?> clazz, String methodName, Object instance,
                              Object... args) {
        try {
            Class<?>[] paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }

            var method = clazz.getMethod(methodName, paramTypes);

            return (Double) method.invoke(instance, args);

        } catch (NoSuchMethodException e) {
            throw new NotFoundException(
                "Method not found: " + methodName + ". Reason: " + e.getMessage());
        } catch (Exception e) {
            throw new LoadingException(
                "Error invoking method: " + methodName + ". Reason: " + e.getMessage());
        }
    }

    private void populateTopLevelBelongingInformation(DocumentPublicationIndex publication,
                                                      EnrichedResearcherAssessmentResponseDTO assessmentResult,
                                                      List<Integer> subOUsForTopLevelInstitution) {
        if (publication.getAuthorIds().contains(-1)) {
            assessmentResult.getPublicationToInstitution()
                .put(publication.getDatabaseId(), Collections.emptyList());
        }

        var knownAuthorIds = new HashSet<>(publication.getAuthorIds());
        var sameTopLevelEmploymentInstitutions = new ArrayList<Integer>();
        knownAuthorIds.forEach(authorId -> {
            var topLevelInstitutionEmployments =
                involvementRepository.findActiveEmploymentsForPersonAndInstitutions(
                        subOUsForTopLevelInstitution, authorId).stream()
                    .map(employment -> employment.getOrganisationUnit().getId()).toList();
            if (topLevelInstitutionEmployments.isEmpty() ||
                Collections.disjoint(topLevelInstitutionEmployments,
                    publication.getOrganisationUnitIds())) {
                return;
            }

            if (topLevelInstitutionEmployments.contains(subOUsForTopLevelInstitution.getLast())) {
                sameTopLevelEmploymentInstitutions.add(subOUsForTopLevelInstitution.getLast());
            } else {
                sameTopLevelEmploymentInstitutions.add(
                    organisationUnitsRelationRepository.getOneLevelBelowTopOU(
                        topLevelInstitutionEmployments.getFirst(),
                        subOUsForTopLevelInstitution.getLast()));
            }
        });

        if (knownAuthorIds.size() == sameTopLevelEmploymentInstitutions.size()) {
            assessmentResult.getPublicationToInstitution()
                .put(publication.getDatabaseId(),
                    (new HashSet<>(
                        sameTopLevelEmploymentInstitutions.stream().filter(Objects::nonNull)
                            .toList())).stream()
                        .toList());
        } else {
            assessmentResult.getPublicationToInstitution()
                .put(publication.getDatabaseId(), Collections.emptyList());
        }
    }

    private Query findAllPersonsByFilters(
        List<Integer> researcherIds,
        List<Integer> organisationUnitIds) {

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            if (!researcherIds.isEmpty()) {
                var personIdTerms = new TermsQueryField.Builder()
                    .value(researcherIds.stream().map(FieldValue::of).toList())
                    .build();
                b.must(sb -> sb.terms(t -> t.field("databaseId").terms(personIdTerms)));
            }

            if (!organisationUnitIds.isEmpty()) {
                var orgUnitIdTerms = new TermsQueryField.Builder()
                    .value(organisationUnitIds.stream().map(FieldValue::of).toList())
                    .build();
                b.must(sb -> sb.terms(
                    t -> t.field("employment_institutions_id_hierarchy").terms(orgUnitIdTerms)));
            }

            return b;
        })))._toQuery();
    }

    private Integer findDefaultRulebookId() {
        return assessmentRulebookRepository.findDefaultRulebook().orElseGet(
            () -> assessmentRulebookRepository.findById(1)
                .orElseThrow(() -> new NotFoundException("noRulebooksDefinedMessage"))).getId();
    }

    private Optional<AssessmentMeasure> findApplicableMeasure(
        List<AssessmentMeasure> measures, String assessmentArea
    ) {
        return measures.stream()
            .filter(measure -> ClassificationPriorityMapping.existsInGroup(
                measure.getCode(), assessmentArea))
            .findFirst();
    }
}
