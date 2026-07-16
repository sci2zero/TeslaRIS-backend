package rs.teslaris.revisioner.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.document.GeneticMaterialDTO;
import rs.teslaris.core.dto.document.IntangibleProductDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.dto.document.MaterialProductDTO;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.dto.document.OtherEventDTO;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.dto.document.PersonContributionDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.document.PersonEventContributionDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
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
import rs.teslaris.core.repository.institution.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.util.search.CollectionOperations;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.session.RestTemplateProvider;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityIssue;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;
import rs.teslaris.revisioner.repository.DataQualityAssessmentRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataQualityCalculatorPtCris {

    private static final String PROFILE_NAME = "PTCRIS";

    private static final String PROFILE_VERSION = "1.0.0";

    private static final Pattern TITLE_PATTERN = Pattern.compile(
        "^[\\p{L}\\p{N}\\s\\-.,;:!?()'\"/&]+$"
    );

    private static final Pattern PERSON_NAME_PATTERN =
        Pattern.compile("^[\\p{L}\\p{M}\\p{N} .,'\\-]+$");

    private static final Pattern ORGANISATION_NAME_PATTERN =
        Pattern.compile("^[\\p{L}\\p{M}\\p{N} .,'()&/\\-]+$");

    private static final Pattern ORCID_PATTERN =
        Pattern.compile("^\\d{4}-\\d{4}-\\d{4}-\\d{3}[\\dX]$");

    private static final Pattern WEB_OF_SCIENCE_RESEARCHER_ID_PATTERN =
        Pattern.compile("^[A-Z]{1,2}-\\d{4}-\\d{4}$");

    private static final Pattern SCOPUS_AUTHOR_ID_PATTERN =
        Pattern.compile("^\\d{10,12}$");

    private static final Pattern ROR_PATTERN =
        Pattern.compile("^0[a-z0-9]{6}\\d{2}$");

    private static final Pattern ISNI_PATTERN =
        Pattern.compile("^\\d{4}\\s?\\d{4}\\s?\\d{4}\\s?[\\dX]{4}$");

    private static final Pattern SCOPUS_AFID_PATTERN =
        Pattern.compile("^\\d{6,15}$");

    private static final Pattern GOOGLE_SCHOLAR_PATTERN =
        Pattern.compile("^[A-Za-z0-9_-]{12}$");

    private static final Pattern LATTES_PATTERN =
        Pattern.compile("^\\d{16}$");

    private static final Pattern OPENALEX_PATTERN =
        Pattern.compile("^A\\d{4,10}$");

    private static final Pattern CIENCIA_ID_PATTERN = Pattern.compile("^[A-Za-z0-9-]{4,50}$");

    private static final Pattern RINGGOLD_PATTERN =
        Pattern.compile("^\\d{1,10}$");

    private static final Pattern GRID_PATTERN =
        Pattern.compile("^grid\\.\\d+\\.[0-9a-f]{1,2}$", Pattern.CASE_INSENSITIVE);

    private static final Pattern FUNDREF_PATTERN = Pattern.compile("^10\\.13039/\\d{6,12}$");
    private static final Pattern COUNTRY_CODE_PATTERN =
        Pattern.compile("^[a-z]{2}$");
    private static final Pattern COUNTRY_NAME_PATTERN =
        Pattern.compile("^[\\p{L}\\p{N}\\p{M} .,'()\\-/&]+$");
    private static final Pattern LANGUAGE_TAG_PATTERN =
        Pattern.compile("^[a-z]{2,3}(-[A-Za-z0-9]{2,8})*$");
    private static final Pattern LANGUAGE_NAME_PATTERN =
        Pattern.compile("^[\\p{L}\\p{M}\\p{N} .,'()\\-/&]+$");
    private static final Pattern RESEARCH_AREA_NAME_PATTERN =
        Pattern.compile("^[\\p{L}\\p{M}\\p{N} .,'():/&-]+$");
    private static final Pattern URI_PATTERN =
        Pattern.compile("^(https?|ftp)://\\S+$");
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_NUMBER_PATTERN =
        Pattern.compile("^[+]?[0-9()\\- /]{5,30}$");
    private static final Pattern ADDRESS_PATTERN =
        Pattern.compile("^[\\p{L}\\p{M}\\p{N} .,'()/#:&\\-]+$");
    private final Pattern DOI_PATTERN =
        Pattern.compile("^10\\.\\d{4,9}/[-._;()/:A-Za-z0-9]+$");
    private final Pattern HANDLE_PATTERN =
        Pattern.compile("^\\d{2}\\.\\d{3,5}(\\.\\d+)?/\\S+$");
    private final RevisionHydratorRegistry revisionHydratorRegistry;

    private final DocumentRepository documentRepository;

    private final PersonRepository personRepository;

    private final OrganisationUnitRepository organisationUnitRepository;

    private final CountryRepository countryRepository;

    private final LanguageRepository languageRepository;

    private final RestTemplateProvider restTemplateProvider;

    private final Map<Class<?>, BiConsumer<Object, DataQualityAssessment>> assessors =
        Map.ofEntries(
            Map.entry(ThesisResponseDTO.class,
                (dto, assessment) -> assessEntity((ThesisResponseDTO) dto, assessment)),
            Map.entry(PatentDTO.class,
                (dto, assessment) -> assessEntity((PatentDTO) dto, assessment)),
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
                (dto, assessment) -> assessEntity((InvolvementDTO) dto, assessment))
        );


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void assessDataQuality(DataQualityAssessment assessment, String json,
                                  ObjectMapper objectMapper,
                                  DataQualityAssessmentRepository repository) {
        Class<?> dtoClass =
            revisionHydratorRegistry.getDtoClass(assessment.getRevision().getEntityType());

        try {
            Object dto = objectMapper.treeToValue(objectMapper.readTree(json), dtoClass);

            assessEntity(dto, assessment);

            repository.save(assessment);

            log.info(
                "Successfully completed data quality assessment. revisionId={}, entityType={}, score={}, remarks={}",
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

    private void assessEntity(Object dto, DataQualityAssessment assessment) {
        BiConsumer<Object, DataQualityAssessment> assessor = assessors.get(dto.getClass());

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
        finishUpAssessment(assessment);
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

                if (value.length() > 255) {
                    reportIssue(assessment, "titleTooLong", value);
                }

                if (!TITLE_PATTERN.matcher(value).matches()) {
                    reportIssue(assessment, "invalidTitleFormat", value);
                }
            });
        }

        if (!CollectionOperations.containsValues(dto.getDescription())) {
            reportIssue(assessment, "descriptionMissing", dto.getId());
        }

        var documentDate = FlexibleDateConverter.fromDTO(dto.getDocumentDate());

        if (!CollectionOperations.containsValues(dto.getContributions())) {
            reportIssue(assessment, "contributorsMissing", dto.getId());
        } else {
            boolean hasManagedPerson =
                dto.getContributions()
                    .stream()
                    .anyMatch(c -> Objects.nonNull(c.getPersonId()));

            if (!hasManagedPerson) {
                reportIssue(assessment, "noManagedContributor", dto.getId());
            }

            dto.getContributions().forEach(
                contribution ->
                    assessEntity(contribution, assessment,
                        null, null, dto.getId(),
                        StringUtil.parseDocumentDate(FlexibleDate.toISOString(documentDate)))
            );
        }

        if (!FlexibleDate.isDatePresentAndValid(documentDate)) {
            reportIssue(assessment, "documentDateMissing", dto.getId());
        } else {
            try {
                var date = StringUtil.parseDocumentDate(FlexibleDate.toISOString(documentDate));

                if (date.isBefore(LocalDate.of(1950, 1, 1))) {
                    reportIssue(assessment, "documentDateBefore1950", dto.getDocumentDate());
                }

                if (date.isAfter(LocalDate.now().plusYears(3))) {
                    reportIssue(assessment, "documentDateTooFarInFuture",
                        dto.getDocumentDate());
                }
            } catch (Exception e) {
                reportIssue(assessment, "invalidDocumentDateFormat", dto.getDocumentDate());
            }
        }

        if (!StringUtil.valueExists(dto.getDoi())) {
            reportIssue(assessment, "noDoiPresent", dto.getId());
        } else {
            var doi = dto.getDoi();

            if (doi.length() < 9) {
                reportIssue(assessment, "doiTooShort", doi);
            }

            if (doi.length() > 255) {
                reportIssue(assessment, "doiTooLong", doi);
            }

            if (!DOI_PATTERN.matcher(doi).matches()) {
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
            reportIssue(assessment, "noHandlePresent", dto.getId());
        } else {
            var handle = dto.getHandleId();

            if (handle.length() < 8) {
                reportIssue(assessment, "handleTooShort", handle);
            }

            if (handle.length() > 255) {
                reportIssue(assessment, "handleTooLong", handle);
            }

            if (!HANDLE_PATTERN.matcher(handle).matches()) {
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

        if (dto instanceof ThesisResponseDTO thesis) {
            validateRange(
                thesis.getNumberOfPages(),
                1, 5000,
                "numberOfPages",
                assessment);

            validateRange(
                thesis.getNumberOfChapters(),
                1, 30,
                "numberOfChapters",
                assessment);

            validateRange(
                thesis.getNumberOfReferences(),
                1, 5000,
                "numberOfReferences",
                assessment);

            validateRange(
                thesis.getNumberOfTables(),
                1, 200,
                "numberOfTables",
                assessment);

            validateRange(
                thesis.getNumberOfIllustrations(),
                1, 200,
                "numberOfIllustrations",
                assessment);

            validateRange(
                thesis.getNumberOfGraphs(),
                1, 200,
                "numberOfGraphs",
                assessment);

            validateRange(
                thesis.getNumberOfAppendices(),
                1, 30,
                "numberOfAppendices",
                assessment);

            if (Objects.isNull(thesis.getTopicAcceptanceDate())) {
                reportIssue(assessment, "topicAcceptanceDateMissing");
            } else {
                if (thesis.getTopicAcceptanceDate().isBefore(LocalDate.of(1950, 1, 1))) {
                    reportIssue(
                        assessment,
                        "topicAcceptanceDateBefore1950",
                        thesis.getTopicAcceptanceDate());
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

                if (thesis.getThesisDefenceDate().isAfter(LocalDate.now().plusYears(1))) {
                    reportIssue(
                        assessment,
                        "defenceTooFarInFuture",
                        thesis.getThesisDefenceDate());
                }
            }
        }

        // TODO: metadataLicenseMissing
    }

    private void assessEntity(EventDTO dto, DataQualityAssessment assessment) {
        dto.getContributions().forEach(
            contribution ->
                assessEntity(contribution, assessment,
                    dto.getEventType(), dto.getEventType().equals(EventType.OTHER_EVENT) ?
                        ((OtherEventDTO) dto).getType() : null,
                    null, null)
        );
    }

    private void assessEntity(PersonResponseDTO dto, DataQualityAssessment assessment) {
        var personalInfoDTO = dto.getPersonalInfo();

        if (Objects.isNull(personalInfoDTO.getLocalBirthDate())) {
            reportIssue(assessment, "birthDateMissing");
        } else {
            var birthDate = personalInfoDTO.getLocalBirthDate();

            if (birthDate.isBefore(LocalDate.of(1900, 1, 1))) {
                reportIssue(assessment, "birthDateBefore1900", birthDate);
            }

            if (birthDate.isAfter(LocalDate.now())) {
                reportIssue(assessment, "birthDateInFuture", birthDate);
            }
        }

        if (!StringUtil.valueExists(personalInfoDTO.getOrcid())) {
            reportIssue(assessment, "noOrcidPresent");
        } else {
            if (!ORCID_PATTERN.matcher(personalInfoDTO.getOrcid()).matches()) {
                reportIssue(assessment, "invalidOrcidFormat", personalInfoDTO.getOrcid());
            }

            if (personRepository.existsByOrcid(personalInfoDTO.getOrcid(), dto.getId())) {
                reportIssue(assessment, "duplicateOrcid", personalInfoDTO.getOrcid());
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getWebOfScienceResearcherId())) {
            var rid = personalInfoDTO.getWebOfScienceResearcherId();

            if (rid.length() < 11) {
                reportIssue(assessment, "webOfScienceResearcherIdTooShort", rid);
            }

            if (rid.length() > 11) {
                reportIssue(assessment, "webOfScienceResearcherIdTooLong", rid);
            }

            if (!WEB_OF_SCIENCE_RESEARCHER_ID_PATTERN.matcher(rid).matches()) {
                reportIssue(assessment, "invalidWebOfScienceResearcherIdFormat", rid);
            }

            if (personRepository.existsByWebOfScienceId(rid, dto.getId())) {
                reportIssue(assessment, "duplicateWebOfScienceResearcherId", rid);
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getScopusAuthorId())) {
            var id = personalInfoDTO.getScopusAuthorId();

            if (!SCOPUS_AUTHOR_ID_PATTERN.matcher(id).matches()) {
                reportIssue(assessment, "invalidScopusAuthorIdFormat", id);
            }

            if (personRepository.existsByScopusAuthorId(id, dto.getId())) {
                reportIssue(assessment, "duplicateScopusAuthorId", id);
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getOpenAlexId())) {
            if (!OPENALEX_PATTERN.matcher(personalInfoDTO.getOpenAlexId()).matches()) {
                reportIssue(assessment, "invalidOpenAlexIdFormat",
                    personalInfoDTO.getOpenAlexId());
            }

            if (personRepository.existsByOpenAlexId(personalInfoDTO.getOpenAlexId(), dto.getId())) {
                reportIssue(assessment, "duplicateOpenAlexId", personalInfoDTO.getOpenAlexId());
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getScholarId())) {
            if (!GOOGLE_SCHOLAR_PATTERN.matcher(personalInfoDTO.getScholarId()).matches()) {
                reportIssue(assessment, "invalidGoogleScholarIdFormat",
                    personalInfoDTO.getScholarId());
            }

            if (personRepository.existsByScholarId(personalInfoDTO.getScholarId(), dto.getId())) {
                reportIssue(assessment, "duplicateGoogleScholarId",
                    personalInfoDTO.getScholarId());
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getLattesId())) {
            if (!LATTES_PATTERN.matcher(personalInfoDTO.getLattesId()).matches()) {
                reportIssue(assessment, "invalidLattesIdFormat", personalInfoDTO.getLattesId());
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getNationalScienceId())) {
            if (!CIENCIA_ID_PATTERN.matcher(personalInfoDTO.getNationalScienceId()).matches()) {
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
            personNames.forEach(name -> {
                if (!StringUtil.valueExists(name.getFirstname())) {
                    reportIssue(assessment, "firstNameMissing");
                }

                if (!StringUtil.valueExists(name.getLastname())) {
                    reportIssue(assessment, "lastNameMissing");
                }

                if (StringUtil.valueExists(name.getFirstname()) &&
                    name.getFirstname().length() > 100) {
                    reportIssue(assessment, "firstNameTooLong", name.getFirstname());
                }

                if (StringUtil.valueExists(name.getLastname()) &&
                    name.getLastname().length() > 100) {
                    reportIssue(assessment, "lastNameTooLong", name.getLastname());
                }

                if (StringUtil.valueExists(name.getFirstname()) &&
                    !PERSON_NAME_PATTERN.matcher(name.getFirstname()).matches()) {
                    reportIssue(assessment, "invalidFirstNameFormat", name.getFirstname());
                }

                if (StringUtil.valueExists(name.getLastname()) &&
                    !PERSON_NAME_PATTERN.matcher(name.getLastname()).matches()) {
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
            dto.getName().forEach(name -> {

                String value = name.getContent();

                if (!StringUtil.valueExists(value)) {
                    reportIssue(assessment, "organisationUnitNameMissing");
                    return;
                }

                if (value.length() > 255) {
                    reportIssue(assessment, "organisationUnitNameTooLong", value);
                }

                if (!ORGANISATION_NAME_PATTERN.matcher(value).matches()) {
                    reportIssue(assessment, "invalidOrganisationUnitNameFormat", value);
                }
            });
        }

        if (!CollectionOperations.containsValues(dto.getDescription())) {
            reportIssue(assessment, "organisationUnitDescriptionMissing");
        }

        if (StringUtil.valueExists(dto.getRor())) {

            if (!ROR_PATTERN.matcher(dto.getRor()).matches()) {
                reportIssue(assessment, "invalidRorFormat", dto.getRor());
            }

            if (organisationUnitRepository.existsByROR(dto.getRor(), dto.getId())) {
                reportIssue(assessment, "duplicateRor", dto.getRor());
            }
        }

        if (StringUtil.valueExists(dto.getIsni())) {

            if (!ISNI_PATTERN.matcher(dto.getIsni()).matches()) {
                reportIssue(assessment, "invalidIsniFormat", dto.getIsni());
            }

            if (organisationUnitRepository.existsByIsni(dto.getIsni(), dto.getId())) {
                reportIssue(assessment, "duplicateIsni", dto.getIsni());
            }
        }

        if (StringUtil.valueExists(dto.getScopusAfid())) {

            if (!SCOPUS_AFID_PATTERN.matcher(dto.getScopusAfid()).matches()) {
                reportIssue(assessment, "invalidScopusAfidFormat", dto.getScopusAfid());
            }

            if (organisationUnitRepository.existsByScopusAfid(dto.getScopusAfid(), dto.getId())) {
                reportIssue(assessment, "duplicateScopusAfid", dto.getScopusAfid());
            }
        }

        if (StringUtil.valueExists(dto.getGrid())) {
            if (!GRID_PATTERN.matcher(dto.getGrid()).matches()) {
                reportIssue(assessment, "invalidGridFormat", dto.getGrid());
            }

            if (organisationUnitRepository.existsByGrid(dto.getGrid(), dto.getId())) {
                reportIssue(assessment, "duplicateGrid", dto.getGrid());
            }
        }

        if (StringUtil.valueExists(dto.getRinggold())) {
            if (!RINGGOLD_PATTERN.matcher(dto.getRinggold()).matches()) {
                reportIssue(assessment, "invalidRinggoldFormat", dto.getRinggold());
            }

            if (organisationUnitRepository.existsByRinggold(dto.getRinggold(), dto.getId())) {
                reportIssue(assessment, "duplicateRinggold", dto.getRinggold());
            }
        }

        if (StringUtil.valueExists(dto.getFundref())) {
            if (!FUNDREF_PATTERN.matcher(dto.getFundref()).matches()) {
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

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(CountryDTO dto, DataQualityAssessment assessment) {
        if (!StringUtil.valueExists(dto.getCode())) {
            reportIssue(assessment, "countryCodeMissing");
        } else {
            if (dto.getCode().length() != 2) {
                reportIssue(assessment, "countryCodeInvalidLength", dto.getCode());
            }

            if (!COUNTRY_CODE_PATTERN.matcher(dto.getCode()).matches()) {
                reportIssue(assessment, "invalidCountryCodeFormat", dto.getCode());
            }

            if (countryRepository.existsByCode(dto.getCode(), dto.getId())) {
                reportIssue(assessment, "duplicateCountryCode", dto.getCode());
            }
        }

        if (!CollectionOperations.containsValues(dto.getName())) {
            reportIssue(assessment, "countryNameMissing");
        } else {
            dto.getName().forEach(name -> {
                var value = name.getContent();

                if (!StringUtil.valueExists(value)) {
                    reportIssue(assessment, "countryNameMissing");
                    return;
                }

                if (value.length() > 255) {
                    reportIssue(assessment, "countryNameTooLong", value);
                }

                if (!COUNTRY_NAME_PATTERN.matcher(value).matches()) {
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
        if (StringUtil.valueExists(dto.getContactEmail())) {
            if (dto.getContactEmail().length() > 255) {
                reportIssue(assessment, "contactEmailTooLong", dto.getContactEmail());
            }

            if (!EMAIL_PATTERN.matcher(dto.getContactEmail()).matches()) {
                reportIssue(assessment, "invalidContactEmailFormat", dto.getContactEmail());
            }
        }

        if (StringUtil.valueExists(dto.getPhoneNumber())) {
            if (dto.getPhoneNumber().length() > 30) {
                reportIssue(assessment, "phoneNumberTooLong", dto.getPhoneNumber());
            }

            if (!PHONE_NUMBER_PATTERN.matcher(dto.getPhoneNumber()).matches()) {
                reportIssue(assessment, "invalidPhoneNumberFormat", dto.getPhoneNumber());
            }
        }

        if (StringUtil.valueExists(dto.getMobilePhoneNumber())) {
            if (dto.getMobilePhoneNumber().length() > 30) {
                reportIssue(assessment, "mobilePhoneNumberTooLong", dto.getMobilePhoneNumber());
            }

            if (!PHONE_NUMBER_PATTERN.matcher(dto.getMobilePhoneNumber()).matches()) {
                reportIssue(assessment, "invalidMobilePhoneNumberFormat",
                    dto.getMobilePhoneNumber());
            }
        }

        if (StringUtil.valueExists(dto.getFaxNumber())) {
            if (dto.getFaxNumber().length() > 30) {
                reportIssue(assessment, "faxNumberTooLong", dto.getFaxNumber());
            }

            if (!PHONE_NUMBER_PATTERN.matcher(dto.getFaxNumber()).matches()) {
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
            if (dto.getLanguageCode().length() > 10) {
                reportIssue(assessment, "languageTagTooLong", dto.getLanguageCode());
            }

            if (!LANGUAGE_TAG_PATTERN.matcher(dto.getLanguageCode()).matches()) {
                reportIssue(assessment, "invalidLanguageTagFormat", dto.getLanguageCode());
            }

            if (languageRepository.existsByCode(dto.getLanguageCode(), dto.getId())) {
                reportIssue(assessment, "duplicateLanguageTag", dto.getLanguageCode());
            }
        }

        if (!CollectionOperations.containsValues(dto.getName())) {
            reportIssue(assessment, "languageNameMissing");
        } else {
            dto.getName().forEach(name -> {
                var value = name.getContent();

                if (!StringUtil.valueExists(value)) {
                    reportIssue(assessment, "languageNameMissing");
                    return;
                }

                if (value.length() > 255) {
                    reportIssue(assessment, "languageNameTooLong", value);
                }

                if (!LANGUAGE_NAME_PATTERN.matcher(value).matches()) {
                    reportIssue(assessment, "invalidLanguageNameFormat", value);
                }
            });
        }

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(ResearchAreaDTO dto, DataQualityAssessment assessment) {
        if (!CollectionOperations.containsValues(dto.getName())) {
            reportIssue(assessment, "researchAreaNameMissing");
        } else {
            dto.getName().forEach(name -> {
                var value = name.getContent();

                if (!StringUtil.valueExists(value)) {
                    reportIssue(assessment, "researchAreaNameMissing");
                    return;
                }

                if (value.length() > 255) {
                    reportIssue(assessment, "researchAreaNameTooLong", value);
                }

                if (!RESEARCH_AREA_NAME_PATTERN.matcher(value).matches()) {
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
        if (Objects.isNull(dto.getLatitude())) {
            reportIssue(assessment, "latitudeMissing");
        } else if (dto.getLatitude() < -90 || dto.getLatitude() > 90) {
            reportIssue(assessment, "latitudeOutOfRange", dto.getLatitude());
        }

        if (Objects.isNull(dto.getLongitude())) {
            reportIssue(assessment, "longitudeMissing");
        } else if (dto.getLongitude() < -180 || dto.getLongitude() > 180) {
            reportIssue(assessment, "longitudeOutOfRange", dto.getLongitude());
        }

        if (StringUtil.valueExists(dto.getAddress())) {

            if (dto.getAddress().length() > 500) {
                reportIssue(assessment, "addressTooLong", dto.getAddress());
            }

            if (!ADDRESS_PATTERN.matcher(dto.getAddress()).matches()) {
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
            if (dto.regularExpression().length() > 255) {
                reportIssue(assessment, "identifierRegularExpressionTooLong",
                    dto.regularExpression());
            }

            try {
                Pattern.compile(dto.regularExpression());
            } catch (PatternSyntaxException ex) {
                reportIssue(assessment, "invalidIdentifierRegularExpression",
                    dto.regularExpression());
            }
        }

        if (StringUtil.valueExists(dto.uriPrefix())) {
            if (dto.uriPrefix().length() > 255) {
                reportIssue(assessment, "identifierUriTooLong", dto.uriPrefix());
            }

            if (!URI_PATTERN.matcher(dto.uriPrefix()).matches()) {
                reportIssue(assessment, "invalidIdentifierUriFormat", dto.uriPrefix());
            }
        }

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(InvolvementDTO dto, DataQualityAssessment assessment) {
        if (Objects.isNull(dto.getDateFrom())) {
            reportIssue(assessment, "startDateMissing");
        } else {
            if (dto.getDateFrom().isBefore(LocalDate.of(1950, 1, 1))) {
                reportIssue(
                    assessment,
                    "activityStartDateBefore1950",
                    dto.getDateFrom()
                );
            }

            if (Objects.nonNull(dto.getPersonBirthDate()) &&
                dto.getDateFrom().isBefore(dto.getPersonBirthDate().plusYears(15))) {
                reportIssue(
                    assessment,
                    "activityStartDateBeforePersonTurned15",
                    dto.getDateFrom(),
                    dto.getPersonBirthDate()
                );
            }

            if (dto.getDateFrom().isAfter(LocalDate.now().plusYears(3))) {
                reportIssue(
                    assessment,
                    "activityStartDateTooFarInFuture",
                    dto.getDateFrom()
                );
            }
        }

        if (Objects.isNull(dto.getDateTo())) {
            reportIssue(assessment, "endDateMissing");
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
            reportIssue(assessment, "researchAreasMissing");
        }

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(PersonContributionDTO dto, DataQualityAssessment assessment,
                              EventType eventType, OtherEventType otherEventType,
                              Integer documentId, LocalDate documentDate) {
        var person = personRepository.findById(dto.getPersonId());
        if (person.isEmpty()) {
            return;
        }

        var birthDate =
            Objects.requireNonNullElse(person.get().getPersonalInfo(), new PersonalInfo())
                .getLocalBirthDate();

        if (Objects.isNull(dto.getDateFrom())) {
            reportIssue(assessment, "startDateMissing");
        } else {
            if (dto.getDateFrom().isBefore(LocalDate.of(1950, 1, 1))) {
                reportIssue(
                    assessment,
                    "activityStartDateBefore1950",
                    dto.getDateFrom()
                );
            }

            if (Objects.nonNull(birthDate) && dto.getDateFrom().isBefore(birthDate.plusYears(15))) {
                reportIssue(
                    assessment,
                    "activityStartDateBeforePersonTurned15",
                    dto.getDateFrom(),
                    birthDate
                );
            }

            if (dto.getDateFrom().isAfter(LocalDate.now().plusYears(3))) {
                reportIssue(
                    assessment,
                    "activityStartDateTooFarInFuture",
                    dto.getDateFrom()
                );
            }
        }

        if (Objects.isNull(dto.getDateTo())) {
            reportIssue(assessment, "endDateMissing");
        } else if (Objects.nonNull(dto.getDateFrom()) &&
            dto.getDateTo().isBefore(dto.getDateFrom())) {
            reportIssue(
                assessment,
                "activityEndDateBeforeStartDate",
                dto.getDateTo(), dto.getDateFrom()
            );
        }

        if (!CollectionOperations.containsValues(dto.getResearchAreasId())) {
            reportIssue(assessment, "researchAreasMissing");
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
                if (eventContribution.getNumberOfReviewsOrAssessment() > 20) {
                    reportIssue(
                        assessment,
                        "numberOfReviewsTooHigh",
                        eventContribution.getNumberOfReviewsOrAssessment()
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
                reportIssue(
                    assessment,
                    "documentBeforePersonBirth",
                    documentId,
                    documentContribution.getPersonId()
                );
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

    private void validateRange(Integer value, int min, int max, String fieldName,
                               DataQualityAssessment assessment) {
        if (Objects.isNull(value)) {
            return;
        }

        if (value < min) {
            reportIssue(assessment, fieldName + "BelowMinimum");
        }

        if (value > max) {
            reportIssue(assessment, fieldName + "AboveMaximum");
        }
    }

    private void reportIssue(DataQualityAssessment assessment,
                             String issueKey, Object... params) {

        if (Objects.isNull(assessment.getIssues())) {
            assessment.setIssues(new ArrayList<>());
        }

        var severityAndDimension =
            RevisionConfigurationLoader.getIssueSeverityAndDimension(issueKey);

        if (Objects.isNull(severityAndDimension)) {
            return;
        }

        assessment.getIssues().add(
            DataQualityIssue.builder()
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

    private void finishUpAssessment(DataQualityAssessment assessment) {
        assessment.setFinishedAt(Instant.now());
        assessment.setProfileVersion(PROFILE_VERSION);
        assessment.setProfileName(PROFILE_NAME);

        assessment.setFailedRules(
            (int) assessment.getIssues().stream()
                .filter(i -> i.getSeverity().equals(IssueSeverity.ERROR))
                .count());

        assessment.setWarningRules(
            (int) assessment.getIssues().stream()
                .filter(i -> i.getSeverity().equals(IssueSeverity.WARNING))
                .count());

        assessment.setPassedRules(
            RevisionConfigurationLoader.getTotalRuleCount()
                - assessment.getFailedRules()
                - assessment.getWarningRules());

        assessment.setValid(
            assessment.getIssues().stream().anyMatch(DataQualityIssue::isBlocking)
        );
    }
}
