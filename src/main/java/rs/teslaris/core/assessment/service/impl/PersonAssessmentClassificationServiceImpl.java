package rs.teslaris.core.assessment.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.json.JsonData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.dto.ResearcherAssessmentResponseDTO;
import rs.teslaris.core.assessment.model.AssessmentMeasure;
import rs.teslaris.core.assessment.model.AssessmentResearchArea;
import rs.teslaris.core.assessment.model.Commission;
import rs.teslaris.core.assessment.model.EntityAssessmentClassification;
import rs.teslaris.core.assessment.repository.AssessmentResearchAreaRepository;
import rs.teslaris.core.assessment.repository.AssessmentRulebookRepository;
import rs.teslaris.core.assessment.repository.DocumentAssessmentClassificationRepository;
import rs.teslaris.core.assessment.repository.DocumentIndicatorRepository;
import rs.teslaris.core.assessment.repository.EntityAssessmentClassificationRepository;
import rs.teslaris.core.assessment.repository.PersonAssessmentClassificationRepository;
import rs.teslaris.core.assessment.ruleengine.AssessmentPointsRuleEngine;
import rs.teslaris.core.assessment.ruleengine.AssessmentPointsScalingRuleEngine;
import rs.teslaris.core.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.core.assessment.service.interfaces.CommissionService;
import rs.teslaris.core.assessment.service.interfaces.PersonAssessmentClassificationService;
import rs.teslaris.core.assessment.util.ClassificationPriorityMapping;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@Transactional
@Slf4j
public class PersonAssessmentClassificationServiceImpl
    extends EntityAssessmentClassificationServiceImpl implements
    PersonAssessmentClassificationService {

    private final PersonAssessmentClassificationRepository personAssessmentClassificationRepository;

    private final SearchService<PersonIndex> searchService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final DocumentAssessmentClassificationRepository
        documentAssessmentClassificationRepository;

    private final DocumentIndicatorRepository documentIndicatorRepository;

    private final AssessmentResearchAreaRepository assessmentResearchAreaRepository;

    private final AssessmentRulebookRepository assessmentRulebookRepository;

    private final PersonIndexRepository personIndexRepository;

    private final UserRepository userRepository;

    private final CitationService citationService;


    @Autowired
    public PersonAssessmentClassificationServiceImpl(
        AssessmentClassificationService assessmentClassificationService,
        CommissionService commissionService,
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        PersonAssessmentClassificationRepository personAssessmentClassificationRepository,
        SearchService<PersonIndex> searchService,
        DocumentPublicationIndexRepository documentPublicationIndexRepository,
        DocumentAssessmentClassificationRepository documentAssessmentClassificationRepository,
        DocumentIndicatorRepository documentIndicatorRepository,
        AssessmentResearchAreaRepository assessmentResearchAreaRepository,
        AssessmentRulebookRepository assessmentRulebookRepository,
        PersonIndexRepository personIndexRepository, UserRepository userRepository,
        CitationService citationService) {
        super(assessmentClassificationService, commissionService,
            entityAssessmentClassificationRepository);
        this.personAssessmentClassificationRepository = personAssessmentClassificationRepository;
        this.searchService = searchService;
        this.documentPublicationIndexRepository = documentPublicationIndexRepository;
        this.documentAssessmentClassificationRepository =
            documentAssessmentClassificationRepository;
        this.documentIndicatorRepository = documentIndicatorRepository;
        this.assessmentResearchAreaRepository = assessmentResearchAreaRepository;
        this.assessmentRulebookRepository = assessmentRulebookRepository;
        this.personIndexRepository = personIndexRepository;
        this.userRepository = userRepository;
        this.citationService = citationService;
    }

    @Override
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPerson(
        Integer personId) {
        return personAssessmentClassificationRepository.findAssessmentClassificationsForPerson(
                personId).stream().map(EntityAssessmentClassificationConverter::toDTO)
            .sorted((a, b) -> b.year().compareTo(a.year()))
            .collect(Collectors.toList());
    }

    @Override
    public void assessResearchers(LocalDate fromDate, Integer commissionId,
                                  List<Integer> researcherIds, List<Integer> orgUnitIds,
                                  LocalDate startDate, LocalDate endDate) {
        var commission = commissionService.findOneWithFetchedRelations(commissionId);
        var assessmentMeasures = assessmentRulebookRepository
            .readAssessmentMeasuresForRulebook(Pageable.unpaged(), findDefaultRulebookId())
            .getContent();

        var assessmentResult = new ResearcherAssessmentResponseDTO();
        assessmentResult.setCommissionId(commission.getId());
        assessmentResult.setCommissionDescription(
            MultilingualContentConverter.getMultilingualContentDTO(commission.getDescription()));

        var pointsRuleEngine = new AssessmentPointsRuleEngine();
        var scalingRuleEngine = new AssessmentPointsScalingRuleEngine();

        int pageNumber = 0;

        while (true) {
            List<PersonIndex> chunk = searchService
                .runQuery(findAllPersonsByFilters(fromDate.toString(), researcherIds, orgUnitIds),
                    PageRequest.of(pageNumber, 10), PersonIndex.class, "person")
                .getContent();

            if (chunk.isEmpty()) {
                break;
            }

            chunk.forEach(
                personIndex -> processResearcher(personIndex, commission, assessmentMeasures,
                    pointsRuleEngine, scalingRuleEngine, assessmentResult, startDate.getYear(),
                    endDate.getYear()));

            pageNumber++;
        }
    }

    @Override
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

        index.get().getEmploymentInstitutionsIdHierarchy().forEach(institutionId -> {
            var commissions = userRepository.findUserCommissionForOrganisationUnit(institutionId);
            commissions.forEach(commission -> {
                var pointsRuleEngine = new AssessmentPointsRuleEngine();
                var scalingRuleEngine = new AssessmentPointsScalingRuleEngine();

                var assessmentResult = new ResearcherAssessmentResponseDTO();
                assessmentResult.setCommissionId(commission.getId());
                assessmentResult.setCommissionDescription(
                    MultilingualContentConverter.getMultilingualContentDTO(
                        commission.getDescription()));

                processResearcher(index.get(), commission, assessmentMeasures,
                    pointsRuleEngine, scalingRuleEngine, assessmentResult, startDate.getYear(),
                    endDate.getYear());
                assessmentResponse.add(assessmentResult);
            });
        });

        return assessmentResponse;
    }

    private void processResearcher(PersonIndex personIndex, Commission commission,
                                   List<AssessmentMeasure> assessmentMeasures,
                                   AssessmentPointsRuleEngine pointsRuleEngine,
                                   AssessmentPointsScalingRuleEngine scalingRuleEngine,
                                   ResearcherAssessmentResponseDTO assessmentResult,
                                   int startYear, int endYear) {
        var researchArea = getResearchArea(personIndex.getDatabaseId(), commission);

        researchArea.ifPresent(
            areaCode -> {
                if (!commission.getRecognisedResearchAreas().contains(areaCode)) {
                    return;
                }

                assessResearcherPublication(personIndex, commission, assessmentMeasures,
                    pointsRuleEngine, scalingRuleEngine, areaCode, startYear, endYear,
                    assessmentResult);
            });
    }

    private Optional<String> getResearchArea(Integer personId, Commission commission) {
        return assessmentResearchAreaRepository
            .findForPersonIdAndCommissionId(personId, commission.getId())
            .map(AssessmentResearchArea::getResearchAreaCode)
            .or(() -> assessmentResearchAreaRepository.findForPersonId(personId)
                .map(AssessmentResearchArea::getResearchAreaCode));
    }

    private void assessResearcherPublication(
        PersonIndex personIndex,
        Commission commission,
        List<AssessmentMeasure> measures,
        AssessmentPointsRuleEngine pointsRuleEngine,
        AssessmentPointsScalingRuleEngine scalingRuleEngine,
        String researchAreaCode, int startYear, int endYear,
        ResearcherAssessmentResponseDTO assessmentResult) {

        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<DocumentPublicationIndex> publications =
                fetchPublications(personIndex, pageNumber, chunkSize, startYear, endYear);
            hasNextPage = publications.size() == chunkSize;

            for (DocumentPublicationIndex publication : publications) {
                processPublication(publication, commission, measures, pointsRuleEngine,
                    scalingRuleEngine, researchAreaCode, personIndex, assessmentResult);
            }

            pageNumber++;
        }
    }

    private List<DocumentPublicationIndex> fetchPublications(PersonIndex personIndex,
                                                             int pageNumber, int chunkSize,
                                                             int startYear, int endYear) {
        return documentPublicationIndexRepository.findByAuthorIdsAndYearBetween(
            personIndex.getDatabaseId(),
            startYear, endYear, PageRequest.of(pageNumber, chunkSize)).getContent();
    }

    private void processPublication(
        DocumentPublicationIndex publication,
        Commission commission,
        List<AssessmentMeasure> measures,
        AssessmentPointsRuleEngine pointsRuleEngine,
        AssessmentPointsScalingRuleEngine scalingRuleEngine,
        String researchAreaCode,
        PersonIndex personIndex,
        ResearcherAssessmentResponseDTO assessmentResult) {

        var classifications = documentAssessmentClassificationRepository
            .findAssessmentClassificationsForDocumentAndCommission(publication.getDatabaseId(),
                commission.getId());

        if (classifications.isEmpty()) {
            return;
        }

        var classification = classifications.stream().filter(
                EntityAssessmentClassification::getManual).findFirst()
            .orElseGet(() -> classifications.stream().findFirst().get());
        var classificationCode = classification.getAssessmentClassification().getCode();

        var applicableMeasureOpt = measures.stream()
            .filter(measure -> ClassificationPriorityMapping.existsInGroup(measure.getCode(),
                classificationCode))
            .findFirst();

        if (applicableMeasureOpt.isEmpty()) {
            return;
        }

        var applicableMeasure = applicableMeasureOpt.get();
        var indicators =
            documentIndicatorRepository.findIndicatorsForDocumentAndIndicatorAccessLevel(
                publication.getDatabaseId(), AccessLevel.ADMIN_ONLY);

        scalingRuleEngine.setCurrentEntityIndicators(indicators);
        double points = calculatePoints(applicableMeasure, pointsRuleEngine, scalingRuleEngine,
            researchAreaCode, classificationCode, publication);

        log.info("{} more points for {}", points, personIndex.getName());

        var citation = citationService.craftCitations(publication, "EN");

        assessmentResult.getPublicationsPerCategory()
            .computeIfAbsent(ClassificationPriorityMapping.getCodeDisplayValue(classificationCode),
                k -> new ArrayList<>())
            .add(new Pair<>(citation.getHarvard(), isUserLoggedIn() ? points : 0));
    }

    private double calculatePoints(
        AssessmentMeasure measure,
        AssessmentPointsRuleEngine pointsRuleEngine,
        AssessmentPointsScalingRuleEngine scalingRuleEngine,
        String researchAreaCode,
        String classificationCode,
        DocumentPublicationIndex publication) {

        double basePoints = invokeRule(AssessmentPointsRuleEngine.class, measure.getPointRule(),
            pointsRuleEngine, researchAreaCode, classificationCode);

        return invokeRule(AssessmentPointsScalingRuleEngine.class, measure.getScalingRule(),
            scalingRuleEngine, publication, classificationCode, basePoints);
    }

    public Query findAllPersonsByFilters(
        String date,
        List<Integer> researcherIds,
        List<Integer> organisationUnitIds) {

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(sb -> sb.range(r -> r.field("last_edited").gt(JsonData.of(date))));

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

    public Double invokeRule(Class<?> clazz, String methodName, Object instance, Object... args) {
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

    private boolean isUserLoggedIn() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return !(Objects.isNull(authentication) || !authentication.isAuthenticated() ||
            (authentication.getPrincipal() instanceof String &&
                authentication.getPrincipal().equals("anonymousUser")));
    }

    private Integer findDefaultRulebookId() {
        return assessmentRulebookRepository.findDefaultRulebook().orElseGet(
            () -> assessmentRulebookRepository.findById(1)
                .orElseThrow(() -> new NotFoundException("noRulebooksDefinedMessage"))).getId();
    }
}
