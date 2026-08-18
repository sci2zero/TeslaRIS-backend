package rs.teslaris.revisioner.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import rs.teslaris.core.converter.commontypes.FlexibleDateConverter;
import rs.teslaris.core.dto.commontypes.CountryDTO;
import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.dto.commontypes.LanguageResponseDTO;
import rs.teslaris.core.dto.commontypes.ResearchAreaHierarchyDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.document.GeneticMaterialDTO;
import rs.teslaris.core.dto.document.IntangibleProductDTO;
import rs.teslaris.core.dto.document.IntellectualPropertyDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.dto.document.MaterialProductDTO;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.dto.document.OtherEventDTO;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.dto.document.PersonContributionDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.document.PersonEventContributionDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.dto.identifier.IdentifierResponseDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.institution.ResearchAreaDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.dto.person.involvement.InvolvementDTO;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.model.commontypes.FlexibleDate;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.EventContributionType;
import rs.teslaris.core.model.document.OtherEventType;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.repository.commontypes.CountryRepository;
import rs.teslaris.core.repository.commontypes.LanguageRepository;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.PublisherRepository;
import rs.teslaris.core.repository.institution.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.util.search.CollectionOperations;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.session.RestTemplateProvider;
import rs.teslaris.revisioner.model.qualityassessment.ConstraintEvaluationResult;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.model.qualityassessment.DimensionScore;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;
import rs.teslaris.revisioner.repository.DataQualityAssessmentRepository;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentIndexer;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataQualityCalculator {

    /**
     * Targets whose issues are reported but never scored - their rules do not add to the total and
     * their failures do not deduct. Remove an entry to start scoring that target again.
     */
    private static final Set<String> NON_SCORING_TARGETS = Set.of("Activity");
    private final Map<String, Pattern> compiledPatternCache = new ConcurrentHashMap<>();
    private final RevisionHydratorRegistry revisionHydratorRegistry;
    private final DocumentRepository documentRepository;
    private final PersonRepository personRepository;
    private final OrganisationUnitRepository organisationUnitRepository;
    private final CountryRepository countryRepository;
    private final LanguageRepository languageRepository;
    private final PublisherRepository publisherRepository;
    private final RestTemplateProvider restTemplateProvider;
    private final DataQualityAssessmentIndexer dataQualityAssessmentIndexer;
    private final Map<Class<?>, BiConsumer<Object, DataQualityAssessment>> assessors =
        Map.ofEntries(
            Map.entry(ThesisResponseDTO.class,
                (dto, assessment) -> assessEntity((ThesisResponseDTO) dto, assessment)),
            Map.entry(IntellectualPropertyDTO.class,
                (dto, assessment) -> assessEntity((IntellectualPropertyDTO) dto, assessment)),
            Map.entry(JournalPublicationResponseDTO.class,
                (dto, assessment) -> assessEntity((JournalPublicationResponseDTO) dto, assessment)),
            Map.entry(MonographDTO.class,
                (dto, assessment) -> assessEntity((MonographDTO) dto, assessment)),
            Map.entry(MonographPublicationDTO.class,
                (dto, assessment) -> assessEntity((MonographPublicationDTO) dto, assessment)),
            Map.entry(ProceedingsResponseDTO.class,
                (dto, assessment) -> assessEntity((ProceedingsResponseDTO) dto, assessment)),
            Map.entry(ProceedingsPublicationDTO.class,
                (dto, assessment) -> assessEntity((ProceedingsPublicationDTO) dto, assessment)),
            Map.entry(GeneticMaterialDTO.class,
                (dto, assessment) -> assessEntity((GeneticMaterialDTO) dto, assessment)),
            Map.entry(MaterialProductDTO.class,
                (dto, assessment) -> assessEntity((MaterialProductDTO) dto, assessment)),
            Map.entry(IntangibleProductDTO.class,
                (dto, assessment) -> assessEntity((IntangibleProductDTO) dto, assessment)),
            Map.entry(PerformanceRelatedOutputDTO.class,
                (dto, assessment) -> assessEntity((PerformanceRelatedOutputDTO) dto, assessment)),
            Map.entry(PersonResponseDTO.class,
                (dto, assessment) -> assessEntity((PersonResponseDTO) dto, assessment)),
            Map.entry(EventDTO.class,
                (dto, assessment) -> assessEntity((EventDTO) dto, assessment)),
            Map.entry(PublicationSeriesDTO.class,
                (dto, assessment) -> assessEntity((PublicationSeriesDTO) dto, assessment)),
            Map.entry(OrganisationUnitDTO.class,
                (dto, assessment) -> assessEntity((OrganisationUnitDTO) dto, assessment)),
            Map.entry(CountryDTO.class,
                (dto, assessment) -> assessEntity((CountryDTO) dto, assessment)),
            Map.entry(ContactDTO.class,
                (dto, assessment) -> assessEntity((ContactDTO) dto, assessment)),
            Map.entry(LanguageResponseDTO.class,
                (dto, assessment) -> assessEntity((LanguageResponseDTO) dto, assessment)),
            Map.entry(ResearchAreaDTO.class,
                (dto, assessment) -> assessEntity((ResearchAreaDTO) dto, assessment)),
            Map.entry(IdentifierResponseDTO.class,
                (dto, assessment) -> assessEntity((IdentifierResponseDTO) dto, assessment)),
            Map.entry(InvolvementDTO.class,
                (dto, assessment) -> assessEntity((InvolvementDTO) dto, assessment)),
            Map.entry(PublisherDTO.class,
                (dto, assessment) -> assessEntity((PublisherDTO) dto, assessment))
        );


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void assessDataQuality(DataQualityAssessment assessment, String json,
                                  ObjectMapper objectMapper,
                                  DataQualityAssessmentRepository repository,
                                  List<String> targetTypes) {
        Class<?> dtoClass =
            revisionHydratorRegistry.getDtoClass(assessment.getRevision().getEntityType());

        try {
            Object dto = objectMapper.treeToValue(objectMapper.readTree(json), dtoClass);

            assessEntity(dto, assessment, targetTypes);

            repository.save(assessment);

            dataQualityAssessmentIndexer.index(assessment, targetTypes, dto);

            log.info(
                "Successfully completed data quality assessment that lasted {}s. revisionId={}, entityType={}, score={}, remarks={}",
                Duration.between(assessment.getStartedAt(), assessment.getFinishedAt())
                    .getSeconds(),
                assessment.getId(),
                assessment.getRevision().getEntityType(),
                assessment.getQualityScore(),
                assessment.getIssues().size()
            );
        } catch (JsonProcessingException e) {
            log.error(
                "Failed to deserialize revision {} of type {} into DTO {}.",
                assessment.getId(),
                assessment.getRevision().getEntityType(),
                dtoClass.getName(),
                e
            );
        } catch (Exception e) {
            log.error(
                "Unexpected error while assessing data quality. revisionId={}, entityType={}, dtoClass={}",
                assessment.getId(),
                assessment.getRevision().getEntityType(),
                dtoClass.getName(),
                e
            );
        }
    }

    private void assessEntity(Object dto, DataQualityAssessment assessment,
                              List<String> targetTypes) {
        BiConsumer<Object, DataQualityAssessment> assessor = resolveAssessor(dto.getClass());

        if (Objects.isNull(assessor)) {
            log.warn(
                "No data quality assessor registered for DTO class {} (entityType={}, revisionId={}).",
                dto.getClass().getName(),
                assessment.getRevision().getEntityType(),
                assessment.getRevision().getId()
            );
            return;
        }

        assessor.accept(dto, assessment);
        finishUpAssessment(assessment, targetTypes);
    }

    @Nullable
    private BiConsumer<Object, DataQualityAssessment> resolveAssessor(Class<?> dtoClass) {
        for (var type = dtoClass; Objects.nonNull(type) && !Object.class.equals(type);
             type = type.getSuperclass()) {
            var assessor = assessors.get(type);

            if (Objects.nonNull(assessor)) {
                if (!type.equals(dtoClass)) {
                    log.debug("Assessing {} with the assessor registered for {}.",
                        dtoClass.getSimpleName(), type.getSimpleName()
                    );
                }

                return assessor;
            }
        }

        return null;
    }

    private void assessEntity(DocumentDTO dto, DataQualityAssessment assessment) {
        assessment.setQualityScore(0.0); // TODO: Update score

        if (!CollectionOperations.containsValues(dto.getTitle())) {
            reportIssue(assessment, "titleMissing");
        } else {
            dto.getTitle().forEach(title -> {
                var value = title.getContent();

                if (!StringUtil.valueExists(value)) {
                    reportIssue(assessment, "invalidTitleFormat", value);
                    return;
                }

                var titleMaxLength = getIntConstraint(assessment, "titleTooLong", "maxLength");
                if (Objects.nonNull(titleMaxLength) && value.length() > titleMaxLength) {
                    reportIssue(assessment, "titleTooLong", value, titleMaxLength);
                }

                var titlePattern = getPatternConstraint(assessment, "invalidTitleFormat");
                if (Objects.nonNull(titlePattern) && !titlePattern.matcher(value).matches()) {
                    reportIssue(assessment, "invalidTitleFormat", value);
                }
            });
        }

        if (!CollectionOperations.containsValues(dto.getDescription())) {
            reportIssue(assessment, "descriptionMissing");
        }

        var documentDate = FlexibleDateConverter.fromDTO(dto.getDocumentDate());

        if (!CollectionOperations.containsValues(dto.getContributions())) {
            reportIssue(assessment, "contributorsMissing");
        } else {
            boolean hasManagedPerson =
                dto.getContributions()
                    .stream()
                    .anyMatch(c -> Objects.nonNull(c.getPersonId()));

            if (!hasManagedPerson) {
                reportIssue(assessment, "noManagedContributor");
            }

            dto.getContributions().forEach(
                contribution ->
                    assessEntity(contribution, assessment,
                        null, null,
                        StringUtil.parseDocumentDate(FlexibleDate.toISOString(documentDate)))
            );
        }

        if (!FlexibleDate.isDatePresentAndValid(documentDate)) {
            reportIssue(assessment, "documentDateMissing");
        } else {
            try {
                var date = StringUtil.parseDocumentDate(FlexibleDate.toISOString(documentDate));

                var documentDateMinYear =
                    getIntConstraint(assessment, "documentDateBefore", "minYear");
                if (Objects.nonNull(documentDateMinYear) &&
                    date.isBefore(LocalDate.of(documentDateMinYear, 1, 1))) {
                    reportIssue(assessment, "documentDateBefore", dto.getDocumentDate(),
                        documentDateMinYear);
                }

                var documentDateMaxFutureYears =
                    getIntConstraint(assessment, "documentDateTooFarInFuture", "maxFutureYears");
                if (Objects.nonNull(documentDateMaxFutureYears) &&
                    date.isAfter(LocalDate.now().plusYears(documentDateMaxFutureYears))) {
                    reportIssue(assessment, "documentDateTooFarInFuture",
                        dto.getDocumentDate(), documentDateMaxFutureYears);
                }
            } catch (Exception e) {
                reportIssue(assessment, "invalidDocumentDateFormat", dto.getDocumentDate());
            }
        }

        if (!StringUtil.valueExists(dto.getDoi())) {
            reportIssue(assessment, "noDoiPresent");
        } else {
            var doi = dto.getDoi();

            var doiMinLength = getIntConstraint(assessment, "doiTooShort", "minLength");
            if (Objects.nonNull(doiMinLength) && doi.length() < doiMinLength) {
                reportIssue(assessment, "doiTooShort", doi, doiMinLength);
            }

            var doiMaxLength = getIntConstraint(assessment, "doiTooLong", "maxLength");
            if (Objects.nonNull(doiMaxLength) && doi.length() > doiMaxLength) {
                reportIssue(assessment, "doiTooLong", doi, doiMaxLength);
            }

            var doiPattern = getPatternConstraint(assessment, "invalidDoiFormat");
            if (Objects.nonNull(doiPattern) && !doiPattern.matcher(doi).matches()) {
                reportIssue(assessment, "invalidDoiFormat", doi);
            }

            if (documentRepository.existsByDoi(doi, dto.getId())) {
                reportIssue(assessment, "duplicateDoi", doi);
            }

            if (!isResolvableDoi(doi)) {
                reportIssue(assessment, "doiNotResolvable", doi);
            }
        }

        if (!StringUtil.valueExists(dto.getHandleId())) {
            reportIssue(assessment, "noHandlePresent");
        } else {
            var handle = dto.getHandleId();

            var handleMinLength = getIntConstraint(assessment, "handleTooShort", "minLength");
            if (Objects.nonNull(handleMinLength) && handle.length() < handleMinLength) {
                reportIssue(assessment, "handleTooShort", handle, handleMinLength);
            }

            var handleMaxLength = getIntConstraint(assessment, "handleTooLong", "maxLength");
            if (Objects.nonNull(handleMaxLength) && handle.length() > handleMaxLength) {
                reportIssue(assessment, "handleTooLong", handle, handleMaxLength);
            }

            var handlePattern = getPatternConstraint(assessment, "invalidHandleFormat");
            if (Objects.nonNull(handlePattern) && !handlePattern.matcher(handle).matches()) {
                reportIssue(assessment, "invalidHandleFormat", handle);
            }

            if (documentRepository.existsByHandleId(handle, dto.getId())) {
                reportIssue(assessment, "duplicateHandle", handle);
            }

            if (!isResolvableHandle(handle)) {
                reportIssue(assessment, "handleNotResolvable", handle);
            }
        }

        boolean hasIdentifier =
            StringUtil.valueExists(dto.getDoi()) ||
                StringUtil.valueExists(dto.getHandleId()) ||
                StringUtil.valueExists(dto.getScopusId()) ||
                StringUtil.valueExists(dto.getOpenAlexId()) ||
                StringUtil.valueExists(dto.getWebOfScienceId()) ||
                StringUtil.valueExists(dto.getArxivId()) ||
                StringUtil.valueExists(dto.getPubmedId()) ||
                StringUtil.valueExists(dto.getSsrnId());

        if (!hasIdentifier) {
            reportIssue(assessment, "noIdentifierPresent");
        }

        if (Objects.isNull(dto.getOpenAccess())) {
            reportIssue(assessment, "openAccessMissing");
        }

        if ((dto instanceof IntangibleProductDTO intangibleProduct &&
            !CollectionOperations.containsValues(intangibleProduct.getResearchAreasId())) ||
            (dto instanceof MaterialProductDTO materialProduct &&
                !CollectionOperations.containsValues(materialProduct.getResearchAreasId()))) {
            reportIssue(assessment, "researchAreasMissing");
        }

        if (dto instanceof IntangibleProductDTO intangibleProduct) {
            assessResearchAreas(intangibleProduct.getResearchAreas(), assessment);
        } else if (dto instanceof MaterialProductDTO materialProduct) {
            assessResearchAreas(materialProduct.getResearchAreas(), assessment);
        }

        if (dto instanceof ThesisResponseDTO thesis) {
            validateRange(thesis.getNumberOfPages(), "numberOfPages", assessment);

            validateRange(thesis.getNumberOfChapters(), "numberOfChapters", assessment);

            validateRange(thesis.getNumberOfReferences(), "numberOfReferences", assessment);

            validateRange(thesis.getNumberOfTables(), "numberOfTables", assessment);

            validateRange(thesis.getNumberOfIllustrations(), "numberOfIllustrations", assessment);

            validateRange(thesis.getNumberOfGraphs(), "numberOfGraphs", assessment);

            validateRange(thesis.getNumberOfAppendices(), "numberOfAppendices", assessment);

            if (Objects.isNull(thesis.getTopicAcceptanceDate())) {
                reportIssue(assessment, "topicAcceptanceDateMissing");
            } else {
                var topicAcceptanceDateMinYear =
                    getIntConstraint(assessment, "topicAcceptanceDateBefore", "minYear");
                if (Objects.nonNull(topicAcceptanceDateMinYear) &&
                    thesis.getTopicAcceptanceDate()
                        .isBefore(LocalDate.of(topicAcceptanceDateMinYear, 1, 1))) {
                    reportIssue(
                        assessment,
                        "topicAcceptanceDateBefore",
                        thesis.getTopicAcceptanceDate(),
                        topicAcceptanceDateMinYear);
                }

                if (thesis.getTopicAcceptanceDate().isAfter(LocalDate.now())) {
                    reportIssue(
                        assessment,
                        "topicAcceptanceDateFuture",
                        thesis.getTopicAcceptanceDate());
                }
            }

            if (Objects.isNull(thesis.getThesisDefenceDate())) {
                reportIssue(assessment, "thesisDefenceDateMissing");
            } else {
                if (Objects.nonNull(thesis.getTopicAcceptanceDate()) &&
                    thesis.getThesisDefenceDate().isBefore(thesis.getTopicAcceptanceDate())) {
                    reportIssue(
                        assessment,
                        "defenceBeforeAcceptance",
                        thesis.getTopicAcceptanceDate(),
                        thesis.getThesisDefenceDate());
                }

                var defenceMaxFutureYears =
                    getIntConstraint(assessment, "defenceTooFarInFuture", "maxFutureYears");
                if (Objects.nonNull(defenceMaxFutureYears) &&
                    thesis.getThesisDefenceDate()
                        .isAfter(LocalDate.now().plusYears(defenceMaxFutureYears))) {
                    reportIssue(
                        assessment,
                        "defenceTooFarInFuture",
                        thesis.getThesisDefenceDate(),
                        defenceMaxFutureYears);
                }
            }
        }

        if (dto instanceof IntellectualPropertyDTO intellectualProperty) {
            var dateRequested =
                FlexibleDateConverter.fromDTO(intellectualProperty.getDateRequested());

            if (!FlexibleDate.isDatePresentAndValid(dateRequested)) {
                reportIssue(assessment, "dateRequestedMissing");
            } else {
                var localDate = FlexibleDate.toLocalDate(dateRequested);

                if (Objects.isNull(localDate)) {
                    reportIssue(assessment, "dateRequestedInvalid");
                }

                var dateRequestedMinYear =
                    getIntConstraint(assessment, "dateRequestedBefore", "minYear");
                if (Objects.nonNull(dateRequestedMinYear) && Objects.nonNull(localDate) &&
                    localDate.isBefore(LocalDate.of(dateRequestedMinYear, 1, 1))) {
                    reportIssue(
                        assessment,
                        "dateRequestedBefore",
                        FlexibleDate.toISOString(dateRequested),
                        dateRequestedMinYear);
                }

                if (Objects.nonNull(localDate) && localDate.isAfter(LocalDate.now())) {
                    reportIssue(
                        assessment,
                        "dateRequestedAfterCurrentDate",
                        FlexibleDate.toISOString(dateRequested));
                }
            }

            var dateFilingPriority =
                FlexibleDateConverter.fromDTO(intellectualProperty.getDateFilingPriority());

            if (!FlexibleDate.isDatePresentAndValid(dateFilingPriority)) {
                reportIssue(assessment, "dateFilingPriorityMissing");
            } else {
                var localDate = FlexibleDate.toLocalDate(dateFilingPriority);

                if (Objects.isNull(localDate)) {
                    reportIssue(assessment, "dateFilingPriorityInvalid");
                }

                var dateFilingPriorityMinYear =
                    getIntConstraint(assessment, "dateFilingPriorityBefore", "minYear");
                if (Objects.nonNull(dateFilingPriorityMinYear) && Objects.nonNull(localDate) &&
                    localDate.isBefore(LocalDate.of(dateFilingPriorityMinYear, 1, 1))) {
                    reportIssue(
                        assessment,
                        "dateFilingPriorityBefore",
                        FlexibleDate.toISOString(dateFilingPriority),
                        dateFilingPriorityMinYear);
                }

                if (Objects.nonNull(localDate)) {
                    var dateRequestedLocalDate = FlexibleDate.toLocalDate(dateRequested);
                    if (Objects.nonNull(dateRequestedLocalDate) &&
                        localDate.isAfter(dateRequestedLocalDate)) {
                        reportIssue(
                            assessment,
                            "dateFilingPriorityAfterRequestDate",
                            FlexibleDate.toISOString(dateFilingPriority),
                            FlexibleDate.toISOString(dateRequested));
                    }
                }
            }

            var dateEndTerm = FlexibleDateConverter.fromDTO(intellectualProperty.getDateTo());

            if (!FlexibleDate.isDatePresentAndValid(dateEndTerm)) {
                reportIssue(assessment, "dateEndTermMissing");
            } else {
                var localDate = FlexibleDate.toLocalDate(dateEndTerm);

                if (Objects.isNull(localDate)) {
                    reportIssue(assessment, "dateEndTermInvalid");
                }

                var dateEndTermMinYear =
                    getIntConstraint(assessment, "dateEndTermBefore", "minYear");
                if (Objects.nonNull(dateEndTermMinYear) && Objects.nonNull(localDate) &&
                    localDate.isBefore(LocalDate.of(dateEndTermMinYear, 1, 1))) {
                    reportIssue(
                        assessment,
                        "dateEndTermBefore",
                        FlexibleDate.toISOString(dateEndTerm),
                        dateEndTermMinYear);
                }
            }
        }
    }

    private void assessEntity(EventDTO dto, DataQualityAssessment assessment) {
        dto.getContributions().forEach(
            contribution ->
                assessEntity(contribution, assessment,
                    dto.getEventType(), dto.getEventType().equals(EventType.OTHER_EVENT) ?
                        ((OtherEventDTO) dto).getType() : null,
                    null)
        );

        assessResearchAreas(dto.getResearchAreas(), assessment);
    }

    private void assessEntity(PublicationSeriesDTO dto, DataQualityAssessment assessment) {
        dto.getContributions().forEach(
            contribution ->
                assessEntity(
                    contribution, assessment,
                    null, null,
                    null
                )
        );
    }

    private void assessEntity(PublisherDTO dto, DataQualityAssessment assessment) {
        // TODO: To be implemented
    }

    private void assessEntity(PersonResponseDTO dto, DataQualityAssessment assessment) {
        var personalInfoDTO = dto.getPersonalInfo();

        if (Objects.isNull(personalInfoDTO.getLocalBirthDate())) {
            reportIssue(assessment, "birthDateMissing");
        } else {
            var birthDate = personalInfoDTO.getLocalBirthDate();

            var birthDateMinYear = getIntConstraint(assessment, "birthDateBefore", "minYear");
            if (Objects.nonNull(birthDateMinYear) &&
                birthDate.isBefore(LocalDate.of(birthDateMinYear, 1, 1))) {
                reportIssue(assessment, "birthDateBefore", birthDate, birthDateMinYear);
            }

            if (birthDate.isAfter(LocalDate.now())) {
                reportIssue(assessment, "birthDateInFuture", birthDate);
            }
        }

        if (!StringUtil.valueExists(personalInfoDTO.getOrcid())) {
            reportIssue(assessment, "noOrcidPresent");
        } else {
            var orcidPattern = getPatternConstraint(assessment, "invalidOrcidFormat");
            if (Objects.nonNull(orcidPattern) &&
                !orcidPattern.matcher(personalInfoDTO.getOrcid()).matches()) {
                reportIssue(assessment, "invalidOrcidFormat", personalInfoDTO.getOrcid());
            }

            if (personRepository.existsByOrcid(personalInfoDTO.getOrcid(), dto.getId())) {
                reportIssue(assessment, "duplicateOrcid", personalInfoDTO.getOrcid());
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getWebOfScienceResearcherId())) {
            var rid = personalInfoDTO.getWebOfScienceResearcherId();

            var wosMinLength =
                getIntConstraint(assessment, "webOfScienceResearcherIdTooShort", "minLength");
            if (Objects.nonNull(wosMinLength) && rid.length() < wosMinLength) {
                reportIssue(assessment, "webOfScienceResearcherIdTooShort", rid, wosMinLength);
            }

            var wosMaxLength =
                getIntConstraint(assessment, "webOfScienceResearcherIdTooLong", "maxLength");
            if (Objects.nonNull(wosMaxLength) && rid.length() > wosMaxLength) {
                reportIssue(assessment, "webOfScienceResearcherIdTooLong", rid, wosMaxLength);
            }

            var wosPattern =
                getPatternConstraint(assessment, "invalidWebOfScienceResearcherIdFormat");
            if (Objects.nonNull(wosPattern) && !wosPattern.matcher(rid).matches()) {
                reportIssue(assessment, "invalidWebOfScienceResearcherIdFormat", rid);
            }

            if (personRepository.existsByWebOfScienceId(rid, dto.getId())) {
                reportIssue(assessment, "duplicateWebOfScienceResearcherId", rid);
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getScopusAuthorId())) {
            var id = personalInfoDTO.getScopusAuthorId();

            var scopusAuthorIdPattern =
                getPatternConstraint(assessment, "invalidScopusAuthorIdFormat");
            if (Objects.nonNull(scopusAuthorIdPattern) &&
                !scopusAuthorIdPattern.matcher(id).matches()) {
                reportIssue(assessment, "invalidScopusAuthorIdFormat", id);
            }

            if (personRepository.existsByScopusAuthorId(id, dto.getId())) {
                reportIssue(assessment, "duplicateScopusAuthorId", id);
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getOpenAlexId())) {
            var openAlexPattern = getPatternConstraint(assessment, "invalidOpenAlexIdFormat");
            if (Objects.nonNull(openAlexPattern) &&
                !openAlexPattern.matcher(personalInfoDTO.getOpenAlexId()).matches()) {
                reportIssue(assessment, "invalidOpenAlexIdFormat",
                    personalInfoDTO.getOpenAlexId());
            }

            if (personRepository.existsByOpenAlexId(personalInfoDTO.getOpenAlexId(), dto.getId())) {
                reportIssue(assessment, "duplicateOpenAlexId", personalInfoDTO.getOpenAlexId());
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getScholarId())) {
            var scholarPattern = getPatternConstraint(assessment, "invalidGoogleScholarIdFormat");
            if (Objects.nonNull(scholarPattern) &&
                !scholarPattern.matcher(personalInfoDTO.getScholarId()).matches()) {
                reportIssue(assessment, "invalidGoogleScholarIdFormat",
                    personalInfoDTO.getScholarId());
            }

            if (personRepository.existsByScholarId(personalInfoDTO.getScholarId(), dto.getId())) {
                reportIssue(assessment, "duplicateGoogleScholarId",
                    personalInfoDTO.getScholarId());
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getLattesId())) {
            var lattesPattern = getPatternConstraint(assessment, "invalidLattesIdFormat");
            if (Objects.nonNull(lattesPattern) &&
                !lattesPattern.matcher(personalInfoDTO.getLattesId()).matches()) {
                reportIssue(assessment, "invalidLattesIdFormat", personalInfoDTO.getLattesId());
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getNationalScienceId())) {
            var cienciaPattern = getPatternConstraint(assessment, "invalidCienciaIdFormat");
            if (Objects.nonNull(cienciaPattern) &&
                !cienciaPattern.matcher(personalInfoDTO.getNationalScienceId()).matches()) {
                reportIssue(assessment, "invalidCienciaIdFormat",
                    personalInfoDTO.getNationalScienceId());
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getAuthenticusId())) {
            if (personRepository.existsByAuthenticusId(personalInfoDTO.getAuthenticusId(),
                dto.getId())) {
                reportIssue(assessment, "duplicateAuthenticusId",
                    personalInfoDTO.getAuthenticusId());
            }
        }

        var personNames = new ArrayList<>(List.of(dto.getPersonName()));
        personNames.addAll(dto.getPersonOtherNames());

        if (!CollectionOperations.containsValues(personNames)) {
            reportIssue(assessment, "nameMissing");
        } else {
            var firstNameMaxLength = getIntConstraint(assessment, "firstNameTooLong", "maxLength");
            var lastNameMaxLength = getIntConstraint(assessment, "lastNameTooLong", "maxLength");
            var firstNamePattern = getPatternConstraint(assessment, "invalidFirstNameFormat");
            var lastNamePattern = getPatternConstraint(assessment, "invalidLastNameFormat");

            personNames.forEach(name -> {
                if (!StringUtil.valueExists(name.getFirstname())) {
                    reportIssue(assessment, "firstNameMissing");
                }

                if (!StringUtil.valueExists(name.getLastname())) {
                    reportIssue(assessment, "lastNameMissing");
                }

                if (StringUtil.valueExists(name.getFirstname()) &&
                    Objects.nonNull(firstNameMaxLength) &&
                    name.getFirstname().length() > firstNameMaxLength) {
                    reportIssue(assessment, "firstNameTooLong", name.getFirstname(),
                        firstNameMaxLength);
                }

                if (StringUtil.valueExists(name.getLastname()) &&
                    Objects.nonNull(lastNameMaxLength) &&
                    name.getLastname().length() > lastNameMaxLength) {
                    reportIssue(assessment, "lastNameTooLong", name.getLastname(),
                        lastNameMaxLength);
                }

                if (StringUtil.valueExists(name.getFirstname()) &&
                    Objects.nonNull(firstNamePattern) &&
                    !firstNamePattern.matcher(name.getFirstname()).matches()) {
                    reportIssue(assessment, "invalidFirstNameFormat", name.getFirstname());
                }

                if (StringUtil.valueExists(name.getLastname()) &&
                    Objects.nonNull(lastNamePattern) &&
                    !lastNamePattern.matcher(name.getLastname()).matches()) {
                    reportIssue(assessment, "invalidLastNameFormat", name.getLastname());
                }
            });
        }

        if (!CollectionOperations.containsValues(dto.getBiography())) {
            reportIssue(assessment, "biographyMissing");
        }

        assessEntity(personalInfoDTO.getContact(), assessment);
        assessEntity(personalInfoDTO.getPrivateContact(), assessment);

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(OrganisationUnitDTO dto, DataQualityAssessment assessment) {
        if (!CollectionOperations.containsValues(dto.getName())) {
            reportIssue(assessment, "organisationUnitNameMissing");
        } else {
            var nameMaxLength =
                getIntConstraint(assessment, "organisationUnitNameTooLong", "maxLength");
            var namePattern =
                getPatternConstraint(assessment, "invalidOrganisationUnitNameFormat");

            dto.getName().forEach(name -> {

                String value = name.getContent();

                if (!StringUtil.valueExists(value)) {
                    reportIssue(assessment, "organisationUnitNameMissing");
                    return;
                }

                if (Objects.nonNull(nameMaxLength) && value.length() > nameMaxLength) {
                    reportIssue(assessment, "organisationUnitNameTooLong", value, nameMaxLength);
                }

                if (Objects.nonNull(namePattern) && !namePattern.matcher(value).matches()) {
                    reportIssue(assessment, "invalidOrganisationUnitNameFormat", value);
                }
            });
        }

        if (!CollectionOperations.containsValues(dto.getDescription())) {
            reportIssue(assessment, "organisationUnitDescriptionMissing");
        }

        if (StringUtil.valueExists(dto.getRor())) {

            var rorPattern = getPatternConstraint(assessment, "invalidRorFormat");
            if (Objects.nonNull(rorPattern) && !rorPattern.matcher(dto.getRor()).matches()) {
                reportIssue(assessment, "invalidRorFormat", dto.getRor());
            }

            if (organisationUnitRepository.existsByROR(dto.getRor(), dto.getId())) {
                reportIssue(assessment, "duplicateRor", dto.getRor());
            }
        }

        if (StringUtil.valueExists(dto.getIsni())) {

            var isniPattern = getPatternConstraint(assessment, "invalidIsniFormat");
            if (Objects.nonNull(isniPattern) && !isniPattern.matcher(dto.getIsni()).matches()) {
                reportIssue(assessment, "invalidIsniFormat", dto.getIsni());
            }

            if (organisationUnitRepository.existsByIsni(dto.getIsni(), dto.getId())) {
                reportIssue(assessment, "duplicateIsni", dto.getIsni());
            }
        }

        if (StringUtil.valueExists(dto.getScopusAfid())) {

            var scopusAfidPattern = getPatternConstraint(assessment, "invalidScopusAfidFormat");
            if (Objects.nonNull(scopusAfidPattern) &&
                !scopusAfidPattern.matcher(dto.getScopusAfid()).matches()) {
                reportIssue(assessment, "invalidScopusAfidFormat", dto.getScopusAfid());
            }

            if (organisationUnitRepository.existsByScopusAfid(dto.getScopusAfid(), dto.getId())) {
                reportIssue(assessment, "duplicateScopusAfid", dto.getScopusAfid());
            }
        }

        if (StringUtil.valueExists(dto.getGrid())) {
            var gridPattern = getPatternConstraint(assessment, "invalidGridFormat");
            if (Objects.nonNull(gridPattern) && !gridPattern.matcher(dto.getGrid()).matches()) {
                reportIssue(assessment, "invalidGridFormat", dto.getGrid());
            }

            if (organisationUnitRepository.existsByGrid(dto.getGrid(), dto.getId())) {
                reportIssue(assessment, "duplicateGrid", dto.getGrid());
            }
        }

        if (StringUtil.valueExists(dto.getRinggold())) {
            var ringgoldPattern = getPatternConstraint(assessment, "invalidRinggoldFormat");
            if (Objects.nonNull(ringgoldPattern) &&
                !ringgoldPattern.matcher(dto.getRinggold()).matches()) {
                reportIssue(assessment, "invalidRinggoldFormat", dto.getRinggold());
            }

            if (organisationUnitRepository.existsByRinggold(dto.getRinggold(), dto.getId())) {
                reportIssue(assessment, "duplicateRinggold", dto.getRinggold());
            }
        }

        if (StringUtil.valueExists(dto.getFundref())) {
            var fundrefPattern = getPatternConstraint(assessment, "invalidFundrefFormat");
            if (Objects.nonNull(fundrefPattern) &&
                !fundrefPattern.matcher(dto.getFundref()).matches()) {
                reportIssue(assessment, "invalidFundrefFormat", dto.getFundref());
            }
        }

        if (Objects.nonNull(dto.getDateEstablished()) &&
            Objects.nonNull(dto.getDateDissolved()) &&
            dto.getDateDissolved().isBefore(dto.getDateEstablished())) {

            reportIssue(
                assessment,
                "dateDissolvedBeforeEstablished",
                dto.getDateEstablished(),
                dto.getDateDissolved()
            );
        }

        assessEntity(dto.getContact(), assessment);
        assessEntity(dto.getLocation(), assessment);
        assessResearchAreas(dto.getResearchAreas(), assessment);

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(CountryDTO dto, DataQualityAssessment assessment) {
        if (!StringUtil.valueExists(dto.getCode())) {
            reportIssue(assessment, "countryCodeMissing");
        } else {
            var codeLength = getIntConstraint(assessment, "countryCodeInvalidLength", "length");
            if (Objects.nonNull(codeLength) && dto.getCode().length() != codeLength) {
                reportIssue(assessment, "countryCodeInvalidLength", dto.getCode(), codeLength);
            }

            var codePattern = getPatternConstraint(assessment, "invalidCountryCodeFormat");
            if (Objects.nonNull(codePattern) && !codePattern.matcher(dto.getCode()).matches()) {
                reportIssue(assessment, "invalidCountryCodeFormat", dto.getCode());
            }

            if (countryRepository.existsByCode(dto.getCode(), dto.getId())) {
                reportIssue(assessment, "duplicateCountryCode", dto.getCode());
            }
        }

        if (!CollectionOperations.containsValues(dto.getName())) {
            reportIssue(assessment, "countryNameMissing");
        } else {
            var nameMaxLength = getIntConstraint(assessment, "countryNameTooLong", "maxLength");
            var namePattern = getPatternConstraint(assessment, "invalidCountryNameFormat");

            dto.getName().forEach(name -> {
                var value = name.getContent();

                if (!StringUtil.valueExists(value)) {
                    reportIssue(assessment, "countryNameMissing");
                    return;
                }

                if (Objects.nonNull(nameMaxLength) && value.length() > nameMaxLength) {
                    reportIssue(assessment, "countryNameTooLong", value, nameMaxLength);
                }

                if (Objects.nonNull(namePattern) && !namePattern.matcher(value).matches()) {
                    reportIssue(assessment, "invalidCountryNameFormat", value);
                }
            });
        }

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(ContactDTO dto, DataQualityAssessment assessment) {
        if (Objects.isNull(dto)) {
            return;
        }

        if (StringUtil.valueExists(dto.getContactEmail())) {
            var emailMaxLength = getIntConstraint(assessment, "contactEmailTooLong", "maxLength");
            if (Objects.nonNull(emailMaxLength) &&
                dto.getContactEmail().length() > emailMaxLength) {
                reportIssue(assessment, "contactEmailTooLong", dto.getContactEmail(),
                    emailMaxLength);
            }

            var emailPattern = getPatternConstraint(assessment, "invalidContactEmailFormat");
            if (Objects.nonNull(emailPattern) &&
                !emailPattern.matcher(dto.getContactEmail()).matches()) {
                reportIssue(assessment, "invalidContactEmailFormat", dto.getContactEmail());
            }
        }

        if (StringUtil.valueExists(dto.getPhoneNumber())) {
            var phoneMaxLength = getIntConstraint(assessment, "phoneNumberTooLong", "maxLength");
            if (Objects.nonNull(phoneMaxLength) && dto.getPhoneNumber().length() > phoneMaxLength) {
                reportIssue(assessment, "phoneNumberTooLong", dto.getPhoneNumber(),
                    phoneMaxLength);
            }

            var phonePattern = getPatternConstraint(assessment, "invalidPhoneNumberFormat");
            if (Objects.nonNull(phonePattern) &&
                !phonePattern.matcher(dto.getPhoneNumber()).matches()) {
                reportIssue(assessment, "invalidPhoneNumberFormat", dto.getPhoneNumber());
            }
        }

        if (StringUtil.valueExists(dto.getMobilePhoneNumber())) {
            var mobileMaxLength =
                getIntConstraint(assessment, "mobilePhoneNumberTooLong", "maxLength");
            if (Objects.nonNull(mobileMaxLength) &&
                dto.getMobilePhoneNumber().length() > mobileMaxLength) {
                reportIssue(assessment, "mobilePhoneNumberTooLong", dto.getMobilePhoneNumber(),
                    mobileMaxLength);
            }

            var mobilePattern = getPatternConstraint(assessment, "invalidMobilePhoneNumberFormat");
            if (Objects.nonNull(mobilePattern) &&
                !mobilePattern.matcher(dto.getMobilePhoneNumber()).matches()) {
                reportIssue(assessment, "invalidMobilePhoneNumberFormat",
                    dto.getMobilePhoneNumber());
            }
        }

        if (StringUtil.valueExists(dto.getFaxNumber())) {
            var faxMaxLength = getIntConstraint(assessment, "faxNumberTooLong", "maxLength");
            if (Objects.nonNull(faxMaxLength) && dto.getFaxNumber().length() > faxMaxLength) {
                reportIssue(assessment, "faxNumberTooLong", dto.getFaxNumber(), faxMaxLength);
            }

            var faxPattern = getPatternConstraint(assessment, "invalidFaxNumberFormat");
            if (Objects.nonNull(faxPattern) &&
                !faxPattern.matcher(dto.getFaxNumber()).matches()) {
                reportIssue(assessment, "invalidFaxNumberFormat", dto.getFaxNumber());
            }
        }

        // TODO: what website?
//    if (StringUtil.valueExists(dto.getWebsite())) {
//        if (dto.getWebsite().length() > 255) {
//            reportIssue(assessment, "contactWebsiteTooLong", dto.getWebsite());
//        }
//
//        if (!URL_PATTERN.matcher(dto.getWebsite()).matches()) {
//            reportIssue(assessment, "invalidContactWebsiteFormat", dto.getWebsite());
//        }
//    }
    }

    private void assessEntity(LanguageResponseDTO dto, DataQualityAssessment assessment) {
        if (!StringUtil.valueExists(dto.getLanguageCode())) {
            reportIssue(assessment, "languageTagMissing");
        } else {
            var tagMaxLength = getIntConstraint(assessment, "languageTagTooLong", "maxLength");
            if (Objects.nonNull(tagMaxLength) && dto.getLanguageCode().length() > tagMaxLength) {
                reportIssue(assessment, "languageTagTooLong", dto.getLanguageCode(), tagMaxLength);
            }

            var tagPattern = getPatternConstraint(assessment, "invalidLanguageTagFormat");
            if (Objects.nonNull(tagPattern) &&
                !tagPattern.matcher(dto.getLanguageCode()).matches()) {
                reportIssue(assessment, "invalidLanguageTagFormat", dto.getLanguageCode());
            }

            if (languageRepository.existsByCode(dto.getLanguageCode(), dto.getId())) {
                reportIssue(assessment, "duplicateLanguageTag", dto.getLanguageCode());
            }
        }

        if (!CollectionOperations.containsValues(dto.getName())) {
            reportIssue(assessment, "languageNameMissing");
        } else {
            var nameMaxLength = getIntConstraint(assessment, "languageNameTooLong", "maxLength");
            var namePattern = getPatternConstraint(assessment, "invalidLanguageNameFormat");

            dto.getName().forEach(name -> {
                var value = name.getContent();

                if (!StringUtil.valueExists(value)) {
                    reportIssue(assessment, "languageNameMissing");
                    return;
                }

                if (Objects.nonNull(nameMaxLength) && value.length() > nameMaxLength) {
                    reportIssue(assessment, "languageNameTooLong", value, nameMaxLength);
                }

                if (Objects.nonNull(namePattern) && !namePattern.matcher(value).matches()) {
                    reportIssue(assessment, "invalidLanguageNameFormat", value);
                }
            });
        }

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(ResearchAreaHierarchyDTO dto, DataQualityAssessment assessment) {
        if (Objects.isNull(dto)) {
            return;
        }

        var researchArea = new ResearchAreaDTO();
        researchArea.setId(dto.getId());
        researchArea.setName(dto.getName());
        researchArea.setDescription(dto.getDescription());

        assessEntity(researchArea, assessment);
    }

    private void assessResearchAreas(Collection<ResearchAreaHierarchyDTO> researchAreas,
                                     DataQualityAssessment assessment) {
        if (Objects.isNull(researchAreas)) {
            return;
        }

        researchAreas.forEach(researchArea -> assessEntity(researchArea, assessment));
    }

    private void assessEntity(ResearchAreaDTO dto, DataQualityAssessment assessment) {
        if (!CollectionOperations.containsValues(dto.getName())) {
            reportIssue(assessment, "researchAreaNameMissing");
        } else {
            var nameMaxLength =
                getIntConstraint(assessment, "researchAreaNameTooLong", "maxLength");
            var namePattern = getPatternConstraint(assessment, "invalidResearchAreaNameFormat");

            dto.getName().forEach(name -> {
                var value = name.getContent();

                if (!StringUtil.valueExists(value)) {
                    reportIssue(assessment, "researchAreaNameMissing");
                    return;
                }

                if (Objects.nonNull(nameMaxLength) && value.length() > nameMaxLength) {
                    reportIssue(assessment, "researchAreaNameTooLong", value, nameMaxLength);
                }

                if (Objects.nonNull(namePattern) && !namePattern.matcher(value).matches()) {
                    reportIssue(assessment, "invalidResearchAreaNameFormat", value);
                }
            });
        }

        // TODO: What URI?
//    if (StringUtil.valueExists(dto.getUri())) {
//        if (dto.getUri().length() > 255) {
//            reportIssue(assessment, "researchAreaUriTooLong", dto.getUri());
//        }
//
//        if (!URI_PATTERN.matcher(dto.getUri()).matches()) {
//            reportIssue(assessment, "invalidResearchAreaUriFormat", dto.getUri());
//        }
//
//        if (researchAreaRepository.existsByUri(dto.getUri(), dto.getId())) {
//            reportIssue(assessment, "duplicateResearchAreaUri", dto.getUri());
//        }
//    }

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(GeoLocationDTO dto, DataQualityAssessment assessment) {
        if (Objects.isNull(dto.getLatitude()) || dto.getLatitude() == 0.0) {
            reportIssue(assessment, "latitudeMissing");
        } else {
            var latMin = getDoubleConstraint(assessment, "latitudeOutOfRange", "min");
            var latMax = getDoubleConstraint(assessment, "latitudeOutOfRange", "max");
            if (Objects.nonNull(latMin) && Objects.nonNull(latMax) &&
                (dto.getLatitude() < latMin || dto.getLatitude() > latMax)) {
                reportIssue(assessment, "latitudeOutOfRange", dto.getLatitude(), latMin, latMax);
            }
        }

        if (Objects.isNull(dto.getLongitude()) || dto.getLongitude() == 0.0) {
            reportIssue(assessment, "longitudeMissing");
        } else {
            var lonMin = getDoubleConstraint(assessment, "longitudeOutOfRange", "min");
            var lonMax = getDoubleConstraint(assessment, "longitudeOutOfRange", "max");
            if (Objects.nonNull(lonMin) && Objects.nonNull(lonMax) &&
                (dto.getLongitude() < lonMin || dto.getLongitude() > lonMax)) {
                reportIssue(assessment, "longitudeOutOfRange", dto.getLongitude(), lonMin, lonMax);
            }
        }

        if (StringUtil.valueExists(dto.getAddress())) {

            var addressMaxLength = getIntConstraint(assessment, "addressTooLong", "maxLength");
            if (Objects.nonNull(addressMaxLength) &&
                dto.getAddress().length() > addressMaxLength) {
                reportIssue(assessment, "addressTooLong", dto.getAddress(), addressMaxLength);
            }

            var addressPattern = getPatternConstraint(assessment, "invalidAddressFormat");
            if (Objects.nonNull(addressPattern) &&
                !addressPattern.matcher(dto.getAddress()).matches()) {
                reportIssue(assessment, "invalidAddressFormat", dto.getAddress());
            }
        }

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(IdentifierResponseDTO dto, DataQualityAssessment assessment) {
        //TODO: Identifier does not hold value
        // TODO: Type not needed as it is modeled using inheritance

        if (StringUtil.valueExists(dto.regularExpression())) {
            var regexMaxLength =
                getIntConstraint(assessment, "identifierRegularExpressionTooLong", "maxLength");
            if (Objects.nonNull(regexMaxLength) &&
                dto.regularExpression().length() > regexMaxLength) {
                reportIssue(assessment, "identifierRegularExpressionTooLong",
                    dto.regularExpression(), regexMaxLength);
            }

            try {
                Pattern.compile(dto.regularExpression());
            } catch (PatternSyntaxException ex) {
                reportIssue(assessment, "invalidIdentifierRegularExpression",
                    dto.regularExpression());
            }
        }

        if (StringUtil.valueExists(dto.uriPrefix())) {
            var uriMaxLength = getIntConstraint(assessment, "identifierUriTooLong", "maxLength");
            if (Objects.nonNull(uriMaxLength) && dto.uriPrefix().length() > uriMaxLength) {
                reportIssue(assessment, "identifierUriTooLong", dto.uriPrefix(), uriMaxLength);
            }

            var uriPattern = getPatternConstraint(assessment, "invalidIdentifierUriFormat");
            if (Objects.nonNull(uriPattern) && !uriPattern.matcher(dto.uriPrefix()).matches()) {
                reportIssue(assessment, "invalidIdentifierUriFormat", dto.uriPrefix());
            }
        }

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(InvolvementDTO dto, DataQualityAssessment assessment) {
        if (Objects.isNull(dto.getDateFrom()) && Objects.isNull(dto.getDateTo()) &&
            (Objects.isNull(dto.getResearchAreasId()) || dto.getResearchAreasId().isEmpty())) {
            return; // involvement is considered an activity if any of the following is set, otherwise don't check for any issues
        }

        if (Objects.isNull(dto.getDateFrom())) {
            reportIssue(assessment, "activityStartDateMissing");
        } else {
            var startDateMinYear =
                getIntConstraint(assessment, "activityStartDateBefore", "minYear");
            if (Objects.nonNull(startDateMinYear) &&
                dto.getDateFrom().isBefore(LocalDate.of(startDateMinYear, 1, 1))) {
                reportIssue(
                    assessment,
                    "activityStartDateBefore",
                    dto.getDateFrom(),
                    startDateMinYear
                );
            }

            var minAgeYears = getIntConstraint(
                assessment, "activityStartDateBeforeMinAge", "minAgeYears");
            if (Objects.nonNull(minAgeYears) && Objects.nonNull(dto.getPersonBirthDate()) &&
                dto.getDateFrom().isBefore(dto.getPersonBirthDate().plusYears(minAgeYears))) {
                reportIssue(
                    assessment,
                    "activityStartDateBeforeMinAge",
                    dto.getDateFrom(),
                    dto.getPersonBirthDate(),
                    minAgeYears
                );
            }

            var startDateMaxFutureYears =
                getIntConstraint(assessment, "activityStartDateTooFarInFuture", "maxFutureYears");
            if (Objects.nonNull(startDateMaxFutureYears) &&
                dto.getDateFrom().isAfter(LocalDate.now().plusYears(startDateMaxFutureYears))) {
                reportIssue(
                    assessment,
                    "activityStartDateTooFarInFuture",
                    dto.getDateFrom(),
                    startDateMaxFutureYears
                );
            }
        }

        if (Objects.isNull(dto.getDateTo())) {
            reportIssue(assessment, "activityEndDateMissing");
        } else if (Objects.nonNull(dto.getDateFrom()) &&
            dto.getDateTo().isBefore(dto.getDateFrom())) {
            reportIssue(
                assessment,
                "activityEndDateBeforeStartDate",
                dto.getDateTo(),
                dto.getDateFrom()
            );
        }

        if (!CollectionOperations.containsValues(dto.getResearchAreasId())) {
            reportIssue(assessment, "activityResearchAreasMissing");
        }

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(PersonContributionDTO dto, DataQualityAssessment assessment,
                              EventType eventType, OtherEventType otherEventType,
                              LocalDate documentDate) {
        if (Objects.isNull(dto.getDateFrom()) && Objects.isNull(dto.getDateTo()) &&
            (Objects.isNull(dto.getResearchAreasId()) || dto.getResearchAreasId().isEmpty())) {
            return; // contribution is considered an activity if any of the following is set, otherwise don't check for any issues
        }

        if (Objects.isNull(dto.getPersonId())) {
            return;
        }

        var person = personRepository.findById(dto.getPersonId());
        if (person.isEmpty()) {
            return;
        }

        var birthDate =
            Objects.requireNonNullElse(person.get().getPersonalInfo(), new PersonalInfo())
                .getLocalBirthDate();

        assessment.setActivitiesCount(assessment.getActivitiesCount() + 1);

        if (Objects.isNull(dto.getDateFrom())) {
            reportIssue(assessment, "activityStartDateMissing");
        } else {
            var startDateMinYear =
                getIntConstraint(assessment, "activityStartDateBefore", "minYear");
            if (Objects.nonNull(startDateMinYear) &&
                dto.getDateFrom().isBefore(LocalDate.of(startDateMinYear, 1, 1))) {
                reportIssue(
                    assessment,
                    "activityStartDateBefore",
                    dto.getDateFrom(),
                    startDateMinYear
                );
            }

            var minAgeYears = getIntConstraint(
                assessment, "activityStartDateBeforeMinAge", "minAgeYears");
            if (Objects.nonNull(minAgeYears) && Objects.nonNull(birthDate) &&
                dto.getDateFrom().isBefore(birthDate.plusYears(minAgeYears))) {
                reportIssue(
                    assessment,
                    "activityStartDateBeforeMinAge",
                    dto.getDateFrom(),
                    birthDate,
                    minAgeYears
                );
            }

            var startDateMaxFutureYears =
                getIntConstraint(assessment, "activityStartDateTooFarInFuture", "maxFutureYears");
            if (Objects.nonNull(startDateMaxFutureYears) &&
                dto.getDateFrom().isAfter(LocalDate.now().plusYears(startDateMaxFutureYears))) {
                reportIssue(
                    assessment,
                    "activityStartDateTooFarInFuture",
                    dto.getDateFrom(),
                    startDateMaxFutureYears
                );
            }
        }

        if (Objects.isNull(dto.getDateTo())) {
            reportIssue(assessment, "activityEndDateMissing");
        } else if (Objects.nonNull(dto.getDateFrom()) &&
            dto.getDateTo().isBefore(dto.getDateFrom())) {
            reportIssue(
                assessment,
                "activityEndDateBeforeStartDate",
                dto.getDateTo(), dto.getDateFrom()
            );
        }

        if (!CollectionOperations.containsValues(dto.getResearchAreasId())) {
            reportIssue(assessment, "activityResearchAreasMissing");
        }

        if (dto instanceof PersonEventContributionDTO eventContribution) {
            boolean linkedWithCourse = eventType.equals(EventType.COURSE);

            if (!linkedWithCourse) {
                if (Objects.nonNull(eventContribution.getLectureHoursPerWeek())) {
                    reportIssue(assessment,
                        "lectureHoursOnlyForCourse");
                }

                if (Objects.nonNull(eventContribution.getTutorialHoursPerWeek())) {
                    reportIssue(assessment,
                        "tutorialHoursOnlyForCourse");
                }

                if (Objects.nonNull(eventContribution.getLabHoursPerWeek())) {
                    reportIssue(assessment,
                        "labHoursOnlyForCourse");
                }

                if (Objects.nonNull(eventContribution.getOtherContactHoursPerWeek())) {
                    reportIssue(assessment,
                        "otherContactHoursOnlyForCourse");
                }
            }

            boolean reviewerContribution =
                eventContribution.getEventContributionType().equals(EventContributionType.REVIEWER);

            boolean linkedWithConference = eventType.equals(EventType.CONFERENCE);

            if (Objects.nonNull(eventContribution.getNumberOfReviewsOrAssessment())) {
                var maxReviews = getIntConstraint(assessment, "numberOfReviewsTooHigh", "max");
                if (Objects.nonNull(maxReviews) &&
                    eventContribution.getNumberOfReviewsOrAssessment() > maxReviews) {
                    reportIssue(
                        assessment,
                        "numberOfReviewsTooHigh",
                        eventContribution.getNumberOfReviewsOrAssessment(),
                        maxReviews
                    );
                }

                if (!(linkedWithConference && reviewerContribution)) {
                    reportIssue(assessment, "numberOfReviewsOnlyForConferenceReviewer");
                }
            }

            boolean trial = eventType.equals(EventType.OTHER_EVENT) &&
                otherEventType.equals(OtherEventType.TRIAL);

            if (CollectionOperations.containsValues(eventContribution.getCaseName()) && !trial) {
                reportIssue(assessment, "caseOnlyForTrial");
            }

            if (CollectionOperations.containsValues(eventContribution.getLocationJurisdiction()) &&
                !trial) {
                reportIssue(assessment, "locationJurisdictionOnlyForTrial");
            }
        }

        if (dto instanceof PersonDocumentContributionDTO documentContribution) {
            if (Objects.nonNull(documentDate) && Objects.nonNull(birthDate) &&
                documentDate.isBefore(birthDate)) {
                reportIssue(assessment, "documentBeforePersonBirth");
            }

            if (Boolean.TRUE.equals(documentContribution.getIsMainContributor()) &&
                !EnumSet.of(
                        DocumentContributionType.AUTHOR,
                        DocumentContributionType.PRESENTER,
                        DocumentContributionType.EDITOR,
                        DocumentContributionType.ADVISOR,
                        DocumentContributionType.ARGUER,
                        DocumentContributionType.BOARD_MEMBER)
                    .contains(documentContribution.getContributionType())) {
                reportIssue(
                    assessment,
                    "invalidMainContributorFlag",
                    documentContribution.getContributionType().name()
                );
            }

            if (Boolean.TRUE.equals(documentContribution.getIsCorrespondingContributor()) &&
                !EnumSet.of(
                        DocumentContributionType.AUTHOR,
                        DocumentContributionType.PRESENTER,
                        DocumentContributionType.EDITOR)
                    .contains(documentContribution.getContributionType())) {
                reportIssue(
                    assessment,
                    "invalidCorrespondingContributorFlag",
                    documentContribution.getContributionType().name()
                );
            }
        }

        assessEntity(dto.getContact(), assessment);

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private boolean isResolvableDoi(String doi) {
        try {
            restTemplateProvider.provideRestTemplate()
                .getForEntity("https://doi.org/" + doi, String.class);

            return true;
        } catch (RestClientException e) {
            return false;
        }
    }

    private boolean isResolvableHandle(String handle) {
        try {
            restTemplateProvider.provideRestTemplate()
                .getForEntity("https://hdl.handle.net/" + handle, String.class);

            return true;
        } catch (RestClientException e) {
            return false;
        }
    }

    private void validateRange(Integer value, String fieldName, DataQualityAssessment assessment) {
        if (Objects.isNull(value)) {
            return;
        }

        var min = getIntConstraint(assessment, fieldName + "BelowMinimum", "min");
        if (Objects.nonNull(min) && value < min) {
            reportIssue(assessment, fieldName + "BelowMinimum", value, min);
        }

        var max = getIntConstraint(assessment, fieldName + "AboveMaximum", "max");
        if (Objects.nonNull(max) && value > max) {
            reportIssue(assessment, fieldName + "AboveMaximum", value, max);
        }
    }

    @Nullable
    private Integer getIntConstraint(DataQualityAssessment assessment, String issueKey,
                                     String constraintKey) {
        var value = DataQualityAssessmentConfigurationLoader.getConstraint(
            assessment.getProfileName(), assessment.getProfileVersion(), issueKey, constraintKey);

        return value instanceof Number number ? number.intValue() : null;
    }

    @Nullable
    private Double getDoubleConstraint(DataQualityAssessment assessment, String issueKey,
                                       String constraintKey) {
        var value = DataQualityAssessmentConfigurationLoader.getConstraint(
            assessment.getProfileName(), assessment.getProfileVersion(), issueKey, constraintKey);

        return value instanceof Number number ? number.doubleValue() : null;
    }

    @Nullable
    private Pattern getPatternConstraint(DataQualityAssessment assessment, String issueKey) {
        var value = DataQualityAssessmentConfigurationLoader.getConstraint(
            assessment.getProfileName(), assessment.getProfileVersion(), issueKey, "pattern");

        if (!(value instanceof String pattern)) {
            return null;
        }

        return compiledPatternCache.computeIfAbsent(pattern, Pattern::compile);
    }

    private void reportIssue(DataQualityAssessment assessment, String issueKey, Object... params) {
        if (Objects.isNull(assessment.getIssues())) {
            assessment.setIssues(new ArrayList<>());
        }

        var severityAndDimension =
            DataQualityAssessmentConfigurationLoader.getIssueSeverityAndDimension(
                assessment.getProfileName(),
                assessment.getProfileVersion(),
                issueKey
            );

        if (Objects.isNull(severityAndDimension)) {
            return;
        }

        assessment.getIssues().add(
            ConstraintEvaluationResult.builder()
                .key(issueKey)
                .parameters(Arrays.stream(params)
                    .map(String::valueOf)
                    .toList())
                .severity(severityAndDimension.a)
                .dimension(severityAndDimension.b)
                .blocking(severityAndDimension.c)
                .build()
        );
    }

    private void finishUpAssessment(DataQualityAssessment assessment, List<String> targets) {
        assessment.setFinishedAt(Instant.now());

        computeRuleCounts(assessment, targets);

        var scoringTargets = scoringTargets(targets);

        double totalPoints = DataQualityAssessmentConfigurationLoader.getTotalPointsWeighed(
            assessment.getProfileName(),
            assessment.getProfileVersion(),
            scoringTargets
        );

        double totalPointsFair =
            DataQualityAssessmentConfigurationLoader.getTotalPointsWeighedFair(
                assessment.getProfileName(),
                assessment.getProfileVersion(),
                scoringTargets
            );

        assessment.setTotalPoints(totalPoints);
        assessment.setTotalPointsFair(totalPointsFair);

        var deductions = computeDeductions(assessment);

        double achievedPoints = totalPoints - deductions.points();
        double achievedFairPoints = totalPointsFair - deductions.fairPoints();

        assessment.setAchievedPointsNormalised(achievedPoints);
        assessment.setAchievedFairPointsNormalised(achievedFairPoints);

        assessment.setDimensionScores(
            computeDimensionScores(assessment, scoringTargets, deductions.perDimension()));

        assessment.setQualityScore(percentage(achievedPoints, totalPoints));
        assessment.setQualityScoreFair(percentage(achievedFairPoints, totalPointsFair));

        assessment.setValid(
            assessment.getIssues().stream()
                .noneMatch(ConstraintEvaluationResult::isBlocking));

        assessment.setPublicationCandidate(assessment.getValid() && assessment.getQualityScore() >=
            DataQualityAssessmentConfigurationLoader.getProfile(assessment.getProfileName(),
                assessment.getProfileVersion()).minimumRequiredScore());

    }

    private void computeRuleCounts(DataQualityAssessment assessment, List<String> targets) {
        assessment.setErrorFailedRules(
            (int) assessment.getIssues().stream()
                .filter(i -> i.getSeverity() == IssueSeverity.ERROR)
                .count());

        assessment.setWarningFailedRules(
            (int) assessment.getIssues().stream()
                .filter(i -> i.getSeverity() == IssueSeverity.WARNING)
                .count());

        assessment.setInfoFailedRules(
            (int) assessment.getIssues().stream()
                .filter(i -> i.getSeverity() == IssueSeverity.INFO)
                .count());

        assessment.setPassedRules(
            DataQualityAssessmentConfigurationLoader.getTotalRuleCount(
                assessment.getProfileName(),
                assessment.getProfileVersion(),
                targets)
                - assessment.getErrorFailedRules()
                - assessment.getWarningFailedRules()
        );
    }

    private List<String> scoringTargets(List<String> targets) {
        return targets.stream().filter(this::isScoringTarget).toList();
    }

    private boolean isScoringTarget(String target) {
        return Objects.isNull(target) ||
            NON_SCORING_TARGETS.stream().noneMatch(target::startsWith);
    }

    private Deductions computeDeductions(DataQualityAssessment assessment) {
        double deductedPoints = 0;
        double deductedFairPoints = 0;

        EnumMap<QualityDimension, Double> deductedPerDimension =
            new EnumMap<>(QualityDimension.class);

        for (var issue : assessment.getIssues()) {
            var remark =
                DataQualityAssessmentConfigurationLoader.getIssue(
                    assessment.getProfileName(),
                    assessment.getProfileVersion(),
                    issue.getKey()
                );

            if (Objects.isNull(remark) || !isScoringTarget(remark.target())) {
                continue;
            }

            double weightedPoints =
                DataQualityAssessmentConfigurationLoader.getWeightedPoints(
                    assessment.getProfileName(),
                    assessment.getProfileVersion(),
                    remark
                );

            deductedPoints += weightedPoints;

            deductedPerDimension.merge(
                remark.dimension(),
                weightedPoints,
                Double::sum);

            if (remark.usedForFairCompliance()) {
                deductedFairPoints += weightedPoints;
            }
        }

        return new Deductions(deductedPoints, deductedFairPoints, deductedPerDimension);
    }

    private EnumMap<QualityDimension, DimensionScore> computeDimensionScores(
        DataQualityAssessment assessment, List<String> targets,
        EnumMap<QualityDimension, Double> deductedPerDimension) {

        EnumMap<QualityDimension, DimensionScore> dimensionScores =
            new EnumMap<>(QualityDimension.class);

        for (var dimension : QualityDimension.values()) {
            double dimensionTotal =
                DataQualityAssessmentConfigurationLoader
                    .getTotalPointsWeighed(
                        assessment.getProfileName(),
                        assessment.getProfileVersion(),
                        targets,
                        dimension
                    );

            if (dimensionTotal == 0) {
                continue;
            }

            double achieved =
                dimensionTotal - deductedPerDimension.getOrDefault(dimension, 0.0);

            dimensionScores.put(
                dimension,
                new DimensionScore(
                    dimensionTotal,
                    achieved,
                    achieved / dimensionTotal * 100.0));
        }

        return dimensionScores;
    }

    private double percentage(double achieved, double total) {
        return total == 0 ? 100.0 : achieved / total * 100.0;
    }

    private record Deductions(
        double points,
        double fairPoints,
        EnumMap<QualityDimension, Double> perDimension
    ) {
    }
}
