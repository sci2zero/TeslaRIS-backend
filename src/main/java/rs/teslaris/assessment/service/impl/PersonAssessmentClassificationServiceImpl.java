package rs.teslaris.assessment.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
import rs.teslaris.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.assessment.dto.EnrichedResearcherAssessmentResponseDTO;
import rs.teslaris.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.dto.ResearcherAssessmentResponseDTO;
import rs.teslaris.assessment.model.AssessmentMeasure;
import rs.teslaris.assessment.model.AssessmentResearchArea;
import rs.teslaris.assessment.model.Commission;
import rs.teslaris.assessment.model.EntityAssessmentClassification;
import rs.teslaris.assessment.repository.AssessmentResearchAreaRepository;
import rs.teslaris.assessment.repository.AssessmentRulebookRepository;
import rs.teslaris.assessment.repository.DocumentAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.DocumentIndicatorRepository;
import rs.teslaris.assessment.repository.EntityAssessmentClassificationRepository;
import rs.teslaris.assessment.repository.PersonAssessmentClassificationRepository;
import rs.teslaris.assessment.ruleengine.AssessmentPointsRuleEngine;
import rs.teslaris.assessment.ruleengine.AssessmentPointsScalingRuleEngine;
import rs.teslaris.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.assessment.service.interfaces.CommissionService;
import rs.teslaris.assessment.service.interfaces.PersonAssessmentClassificationService;
import rs.teslaris.assessment.util.ClassificationPriorityMapping;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.repository.person.OrganisationUnitsRelationRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.util.Triple;
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

    private final InvolvementRepository involvementRepository;

    private final OrganisationUnitsRelationRepository organisationUnitsRelationRepository;


    @Autowired
    public PersonAssessmentClassificationServiceImpl(
        AssessmentClassificationService assessmentClassificationService,
        CommissionService commissionService,
        DocumentPublicationService documentPublicationService,
        ConferenceService conferenceService,
        EntityAssessmentClassificationRepository entityAssessmentClassificationRepository,
        PersonAssessmentClassificationRepository personAssessmentClassificationRepository,
        SearchService<PersonIndex> searchService,
        DocumentPublicationIndexRepository documentPublicationIndexRepository,
        DocumentAssessmentClassificationRepository documentAssessmentClassificationRepository,
        DocumentIndicatorRepository documentIndicatorRepository,
        AssessmentResearchAreaRepository assessmentResearchAreaRepository,
        AssessmentRulebookRepository assessmentRulebookRepository,
        PersonIndexRepository personIndexRepository, UserRepository userRepository,
        CitationService citationService, InvolvementRepository involvementRepository,
        OrganisationUnitsRelationRepository organisationUnitsRelationRepository) {
        super(assessmentClassificationService, commissionService, documentPublicationService,
            conferenceService, entityAssessmentClassificationRepository);
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
        this.involvementRepository = involvementRepository;
        this.organisationUnitsRelationRepository = organisationUnitsRelationRepository;
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

        var pointsRuleEngine = new AssessmentPointsRuleEngine();
        var scalingRuleEngine = new AssessmentPointsScalingRuleEngine();

        var responses = new ArrayList<EnrichedResearcherAssessmentResponseDTO>();

        for (int pageNumber = 0; ; pageNumber++) {
            List<PersonIndex> chunk = searchService
                .runQuery(findAllPersonsByFilters(researcherIds, List.of(organisationUnit.getId())),
                    PageRequest.of(pageNumber, 10), PersonIndex.class, "person")
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

                var assessmentResult = new EnrichedResearcherAssessmentResponseDTO();
                assessmentResult.setCommissionId(commission.getId());
                assessmentResult.setCommissionDescription(
                    MultilingualContentConverter.getMultilingualContentDTO(
                        commission.getDescription()));

                processResearcher(index.get(), commission, assessmentMeasures,
                    pointsRuleEngine, scalingRuleEngine, assessmentResult, startDate.getYear(),
                    endDate.getYear(), List.of());
                assessmentResponse.add(assessmentResult);
            });
        });

        return assessmentResponse;
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
            areaCode -> {
                if (!commission.getRecognisedResearchAreas().contains(areaCode)) {
                    return;
                }

                assessResearcherPublication(personIndex, commission, assessmentMeasures,
                    pointsRuleEngine, scalingRuleEngine, areaCode, startYear, endYear,
                    assessmentResult, subOUsForTopLevelInstitution);
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
        EnrichedResearcherAssessmentResponseDTO assessmentResult,
        List<Integer> subOUsForTopLevelInstitution) {

        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;

        while (hasNextPage) {
            List<DocumentPublicationIndex> publications =
                fetchPublications(personIndex, pageNumber, chunkSize, startYear, endYear);
            hasNextPage = publications.size() == chunkSize;

            for (DocumentPublicationIndex publication : publications) {
                processPublication(publication, commission, measures, pointsRuleEngine,
                    scalingRuleEngine, researchAreaCode, personIndex, assessmentResult,
                    subOUsForTopLevelInstitution);
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
        String researchAreaCode, PersonIndex personIndex,
        EnrichedResearcherAssessmentResponseDTO assessmentResult,
        List<Integer> subOUsForTopLevelInstitution) {

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
            .add(new Triple<>(citation.getHarvard(), isUserLoggedIn() ? points : 0,
                publication.getDatabaseId()));

        if (!subOUsForTopLevelInstitution.isEmpty()) {
            populateTopLevelBelongingInformation(publication, assessmentResult,
                subOUsForTopLevelInstitution);
        }
    }

    private void populateTopLevelBelongingInformation(DocumentPublicationIndex publication,
                                                      EnrichedResearcherAssessmentResponseDTO assessmentResult,
                                                      List<Integer> subOUsForTopLevelInstitution) {
        if (publication.getAuthorIds().contains(-1)) {
            assessmentResult.getPublicationToInstitution()
                .put(publication.getDatabaseId(), List.of());
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
                .put(publication.getDatabaseId(), List.of());
        }
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
            scalingRuleEngine, publication.getAuthorIds().size(), classificationCode, basePoints);
    }

    public Query findAllPersonsByFilters(
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
