package rs.teslaris.core.assessment.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.json.JsonData;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.model.AssessmentMeasure;
import rs.teslaris.core.assessment.model.AssessmentResearchArea;
import rs.teslaris.core.assessment.model.Commission;
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
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
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


    @Autowired
    public PersonAssessmentClassificationServiceImpl(
        AssessmentClassificationService assessmentClassificationService,
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        CommissionService commissionService,
        PersonAssessmentClassificationRepository personAssessmentClassificationRepository,
        SearchService<PersonIndex> searchService,
        DocumentPublicationIndexRepository documentPublicationIndexRepository,
        DocumentAssessmentClassificationRepository documentAssessmentClassificationRepository,
        DocumentIndicatorRepository documentIndicatorRepository,
        AssessmentResearchAreaRepository assessmentResearchAreaRepository,
        AssessmentRulebookRepository assessmentRulebookRepository) {
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
    public void assessResearchers(LocalDate fromDate, Integer commissionId, Integer rulebookId,
                                  List<Integer> researcherIds, List<Integer> orgUnitIds) {
        var commission = commissionService.findOneWithFetchedRelations(commissionId);
        var assessmentMeasures = assessmentRulebookRepository
            .readAssessmentMeasuresForRulebook(Pageable.unpaged(), rulebookId)
            .getContent();

        var pointsRuleEngine = new AssessmentPointsRuleEngine();
        var scalingRuleEngine = new AssessmentPointsScalingRuleEngine();

        int pageNumber = 0;
        final int chunkSize = 10;

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
                    pointsRuleEngine, scalingRuleEngine));

            pageNumber++;
        }
    }

    private void processResearcher(PersonIndex personIndex, Commission commission,
                                   List<AssessmentMeasure> assessmentMeasures,
                                   AssessmentPointsRuleEngine pointsRuleEngine,
                                   AssessmentPointsScalingRuleEngine scalingRuleEngine) {
        var researchArea = getResearchArea(personIndex.getDatabaseId(), commission);

        researchArea.ifPresent(
            areaCode -> assessResearcherPublication(personIndex, commission, assessmentMeasures,
                pointsRuleEngine, scalingRuleEngine, areaCode));
    }

    private Optional<String> getResearchArea(Integer personId, Commission commission) {
        return assessmentResearchAreaRepository
            .findForPersonIdAndCommissionId(personId, commission.getId())
            .map(AssessmentResearchArea::getResearchAreaCode)
            .or(() -> assessmentResearchAreaRepository.findForPersonId(personId)
                .map(AssessmentResearchArea::getResearchAreaCode));
    }

    private void assessResearcherPublication(PersonIndex personIndex, Commission commission,
                                             List<AssessmentMeasure> measures,
                                             AssessmentPointsRuleEngine pointsRuleEngine,
                                             AssessmentPointsScalingRuleEngine scalingRuleEngine,
                                             String researchAreaCode) {
        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<DocumentPublicationIndex> chunk =
                documentPublicationIndexRepository.findByAuthorIds(personIndex.getDatabaseId(),
                    PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach(publicationIndex -> {
                var assessmentClassification =
                    documentAssessmentClassificationRepository.findAssessmentClassificationsForDocumentAndCommission(
                        publicationIndex.getDatabaseId(), commission.getId());
                var indicators =
                    documentIndicatorRepository.findIndicatorsForDocumentAndIndicatorAccessLevel(
                        publicationIndex.getDatabaseId(), AccessLevel.ADMIN_ONLY);
                scalingRuleEngine.setCurrentEntityIndicators(indicators);


                assessmentClassification.ifPresent(classification -> {
                    var applicableMeasure = measures.stream().filter(measure -> measure.getCode()
                            .equals(classification.getAssessmentClassification().getCode()))
                        .findFirst();
                    if (applicableMeasure.isEmpty()) {
                        return;
                    }

                    double points = invokeRule(AssessmentPointsRuleEngine.class,
                        applicableMeasure.get().getPointRule(), pointsRuleEngine, researchAreaCode,
                        applicableMeasure.get().getCode());
                    points = invokeRule(AssessmentPointsScalingRuleEngine.class,
                        applicableMeasure.get().getScalingRule(), scalingRuleEngine, personIndex,
                        applicableMeasure.get().getCode(), points);

                    System.out.println(points + " more points for " + personIndex.getName());
                });
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
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
}
