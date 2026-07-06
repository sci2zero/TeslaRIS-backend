package rs.teslaris.revisioner.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import rs.teslaris.core.dto.commontypes.CountryDTO;
import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.dto.commontypes.LanguageResponseDTO;
import rs.teslaris.core.dto.document.DatasetDTO;
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
import rs.teslaris.revisioner.model.EntityRevision;
import rs.teslaris.revisioner.repository.EntityRevisionRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataQualityCalculatorPtCris {

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

    private final Map<Class<?>, BiConsumer<Object, EntityRevision>> assessors = Map.ofEntries(
        Map.entry(ThesisResponseDTO.class,
            (dto, rev) -> assessEntity((ThesisResponseDTO) dto, rev)),
        Map.entry(DatasetDTO.class, (dto, rev) -> assessEntity((DatasetDTO) dto, rev)),
        Map.entry(PatentDTO.class, (dto, rev) -> assessEntity((PatentDTO) dto, rev)),
        Map.entry(JournalPublicationResponseDTO.class,
            (dto, rev) -> assessEntity((JournalPublicationResponseDTO) dto, rev)),
        Map.entry(MonographDTO.class, (dto, rev) -> assessEntity((MonographDTO) dto, rev)),
        Map.entry(MonographPublicationDTO.class,
            (dto, rev) -> assessEntity((MonographPublicationDTO) dto, rev)),
        Map.entry(ProceedingsResponseDTO.class,
            (dto, rev) -> assessEntity((ProceedingsResponseDTO) dto, rev)),
        Map.entry(ProceedingsPublicationDTO.class,
            (dto, rev) -> assessEntity((ProceedingsPublicationDTO) dto, rev)),
        Map.entry(GeneticMaterialDTO.class,
            (dto, rev) -> assessEntity((GeneticMaterialDTO) dto, rev)),
        Map.entry(MaterialProductDTO.class,
            (dto, rev) -> assessEntity((MaterialProductDTO) dto, rev)),
        Map.entry(IntangibleProductDTO.class,
            (dto, rev) -> assessEntity((IntangibleProductDTO) dto, rev)),
        Map.entry(PerformanceRelatedOutputDTO.class,
            (dto, rev) -> assessEntity((PerformanceRelatedOutputDTO) dto, rev)),
        Map.entry(PersonResponseDTO.class,
            (dto, rev) -> assessEntity((PersonResponseDTO) dto, rev)),
        Map.entry(EventDTO.class, (dto, rev) -> assessEntity((EventDTO) dto, rev)),
        Map.entry(OrganisationUnitDTO.class,
            (dto, rev) -> assessEntity((OrganisationUnitDTO) dto, rev)),
        Map.entry(CountryDTO.class, (dto, rev) -> assessEntity((CountryDTO) dto, rev)),
        Map.entry(ContactDTO.class, (dto, rev) -> assessEntity((ContactDTO) dto, rev)),
        Map.entry(LanguageResponseDTO.class,
            (dto, rev) -> assessEntity((LanguageResponseDTO) dto, rev)),
        Map.entry(ResearchAreaDTO.class, (dto, rev) -> assessEntity((ResearchAreaDTO) dto, rev)),
        Map.entry(IdentifierResponseDTO.class,
            (dto, rev) -> assessEntity((IdentifierResponseDTO) dto, rev)),
        Map.entry(InvolvementDTO.class, (dto, rev) -> assessEntity((InvolvementDTO) dto, rev))
    );


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void assessDataQuality(EntityRevision entityRevision, String json,
                                  ObjectMapper objectMapper, EntityRevisionRepository repository) {
        Class<?> dtoClass = revisionHydratorRegistry.getDtoClass(entityRevision.getEntityType());

        try {
            Object dto = objectMapper.treeToValue(objectMapper.readTree(json), dtoClass);

            assessEntity(dto, entityRevision);

            repository.save(entityRevision);

            log.info(
                "Successfully completed data quality assessment. revisionId={}, entityType={}, score={}, remarks={}",
                entityRevision.getId(),
                entityRevision.getEntityType(),
                entityRevision.getQualityDataScore(),
                entityRevision.getQualityDataReport().size()
            );
        } catch (JsonProcessingException e) {
            log.error(
                "Failed to deserialize revision {} of type {} into DTO {}.",
                entityRevision.getId(),
                entityRevision.getEntityType(),
                dtoClass.getName(),
                e
            );
        } catch (Exception e) {
            log.error(
                "Unexpected error while assessing data quality. revisionId={}, entityType={}, dtoClass={}",
                entityRevision.getId(),
                entityRevision.getEntityType(),
                dtoClass.getName(),
                e
            );
        }
    }

    private void assessEntity(Object dto, EntityRevision entityRevision) {
        BiConsumer<Object, EntityRevision> assessor = assessors.get(dto.getClass());

        if (Objects.isNull(assessor)) {
            log.warn(
                "No data quality assessor registered for DTO class {} (entityType={}, revisionId={}).",
                dto.getClass().getName(),
                entityRevision.getEntityType(),
                entityRevision.getId()
            );
            return;
        }

        assessor.accept(dto, entityRevision);
    }

    private void assessEntity(DocumentDTO dto, EntityRevision entityRevision) {
        entityRevision.setQualityDataScore(0.0);

        if (!CollectionOperations.containsValues(dto.getTitle())) {
            reportIssue(entityRevision, "titleMissing");
        } else {
            dto.getTitle().forEach(title -> {
                var value = title.getContent();

                if (!StringUtil.valueExists(value)) {
                    reportIssue(entityRevision, "invalidTitleFormat", value);
                    return;
                }

                if (value.length() > 255) {
                    reportIssue(entityRevision, "titleTooLong", value);
                }

                if (!TITLE_PATTERN.matcher(value).matches()) {
                    reportIssue(entityRevision, "invalidTitleFormat", value);
                }
            });
        }

        if (!CollectionOperations.containsValues(dto.getDescription())) {
            reportIssue(entityRevision, "descriptionMissing", dto.getId());
        }

        if (!CollectionOperations.containsValues(dto.getContributions())) {
            reportIssue(entityRevision, "contributorsMissing", dto.getId());
        } else {
            boolean hasManagedPerson =
                dto.getContributions()
                    .stream()
                    .anyMatch(c -> Objects.nonNull(c.getPersonId()));

            if (!hasManagedPerson) {
                reportIssue(entityRevision, "noManagedContributor", dto.getId());
            }

            dto.getContributions().forEach(
                contribution ->
                    assessEntity(contribution, entityRevision,
                        null, null, dto.getId(),
                        StringUtil.parseDocumentDate(dto.getDocumentDate()))
            );
        }

        if (!StringUtil.valueExists(dto.getDocumentDate())) {
            reportIssue(entityRevision, "documentDateMissing", dto.getId());
        } else {
            try {
                var date = StringUtil.parseDocumentDate(dto.getDocumentDate());

                if (date.isBefore(LocalDate.of(1950, 1, 1))) {
                    reportIssue(entityRevision, "documentDateBefore1950", dto.getDocumentDate());
                }

                if (date.isAfter(LocalDate.now().plusYears(3))) {
                    reportIssue(entityRevision, "documentDateTooFarInFuture",
                        dto.getDocumentDate());
                }
            } catch (Exception e) {
                reportIssue(entityRevision, "invalidDocumentDateFormat", dto.getDocumentDate());
            }
        }

        if (!StringUtil.valueExists(dto.getDoi())) {
            reportIssue(entityRevision, "noDoiPresent", dto.getId());
        } else {
            var doi = dto.getDoi();

            if (doi.length() < 9) {
                reportIssue(entityRevision, "doiTooShort", doi);
            }

            if (doi.length() > 255) {
                reportIssue(entityRevision, "doiTooLong", doi);
            }

            if (!DOI_PATTERN.matcher(doi).matches()) {
                reportIssue(entityRevision, "invalidDoiFormat", doi);
            }

            if (documentRepository.existsByDoi(doi, dto.getId())) {
                reportIssue(entityRevision, "duplicateDoi", doi);
            }

            if (!isResolvableDoi(doi)) {
                reportIssue(entityRevision, "doiNotResolvable", doi);
            }
        }

        if (!StringUtil.valueExists(dto.getHandleId())) {
            reportIssue(entityRevision, "noHandlePresent", dto.getId());
        } else {
            var handle = dto.getHandleId();

            if (handle.length() < 8) {
                reportIssue(entityRevision, "handleTooShort", handle);
            }

            if (handle.length() > 255) {
                reportIssue(entityRevision, "handleTooLong", handle);
            }

            if (!HANDLE_PATTERN.matcher(handle).matches()) {
                reportIssue(entityRevision, "invalidHandleFormat", handle);
            }

            if (documentRepository.existsByHandleId(handle, dto.getId())) {
                reportIssue(entityRevision, "duplicateHandle", handle);
            }

            if (!isResolvableHandle(handle)) {
                reportIssue(entityRevision, "handleNotResolvable", handle);
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
            reportIssue(entityRevision, "noIdentifierPresent");
        }

        if (Objects.isNull(dto.getOpenAccess())) {
            reportIssue(entityRevision, "openAccessMissing");
        }

        if ((dto instanceof IntangibleProductDTO intangibleProduct &&
            !CollectionOperations.containsValues(intangibleProduct.getResearchAreasId())) ||
            (dto instanceof MaterialProductDTO materialProduct &&
                !CollectionOperations.containsValues(materialProduct.getResearchAreasId()))) {
            reportIssue(entityRevision, "researchAreasMissing");
        }

        if (dto instanceof ThesisResponseDTO thesis) {
            validateRange(
                thesis.getNumberOfPages(),
                1, 5000,
                "numberOfPages",
                entityRevision);

            validateRange(
                thesis.getNumberOfChapters(),
                1, 30,
                "numberOfChapters",
                entityRevision);

            validateRange(
                thesis.getNumberOfReferences(),
                1, 5000,
                "numberOfReferences",
                entityRevision);

            validateRange(
                thesis.getNumberOfTables(),
                1, 200,
                "numberOfTables",
                entityRevision);

            validateRange(
                thesis.getNumberOfIllustrations(),
                1, 200,
                "numberOfIllustrations",
                entityRevision);

            validateRange(
                thesis.getNumberOfGraphs(),
                1, 200,
                "numberOfGraphs",
                entityRevision);

            validateRange(
                thesis.getNumberOfAppendices(),
                1, 30,
                "numberOfAppendices",
                entityRevision);

            if (Objects.isNull(thesis.getTopicAcceptanceDate())) {
                reportIssue(entityRevision, "topicAcceptanceDateMissing");
            } else {
                if (thesis.getTopicAcceptanceDate().isBefore(LocalDate.of(1950, 1, 1))) {
                    reportIssue(
                        entityRevision,
                        "topicAcceptanceDateBefore1950",
                        thesis.getTopicAcceptanceDate());
                }

                if (thesis.getTopicAcceptanceDate().isAfter(LocalDate.now())) {
                    reportIssue(
                        entityRevision,
                        "topicAcceptanceDateFuture",
                        thesis.getTopicAcceptanceDate());
                }
            }

            if (Objects.isNull(thesis.getThesisDefenceDate())) {
                reportIssue(entityRevision, "thesisDefenceDateMissing");
            } else {
                if (Objects.nonNull(thesis.getTopicAcceptanceDate()) &&
                    thesis.getThesisDefenceDate().isBefore(thesis.getTopicAcceptanceDate())) {
                    reportIssue(
                        entityRevision,
                        "defenceBeforeAcceptance",
                        thesis.getTopicAcceptanceDate(),
                        thesis.getThesisDefenceDate());
                }

                if (thesis.getThesisDefenceDate().isAfter(LocalDate.now().plusYears(1))) {
                    reportIssue(
                        entityRevision,
                        "defenceTooFarInFuture",
                        thesis.getThesisDefenceDate());
                }
            }
        }

        // TODO: metadataLicenseMissing
    }

    private void assessEntity(EventDTO dto, EntityRevision entityRevision) {
        dto.getContributions().forEach(
            contribution ->
                assessEntity(contribution, entityRevision,
                    dto.getEventType(), dto.getEventType().equals(EventType.OTHER_EVENT) ?
                        ((OtherEventDTO) dto).getType() : null,
                    null, null)
        );
    }

    private void assessEntity(PersonResponseDTO dto, EntityRevision entityRevision) {
        var personalInfoDTO = dto.getPersonalInfo();

        if (Objects.isNull(personalInfoDTO.getLocalBirthDate())) {
            reportIssue(entityRevision, "birthDateMissing");
        } else {
            var birthDate = personalInfoDTO.getLocalBirthDate();

            if (birthDate.isBefore(LocalDate.of(1900, 1, 1))) {
                reportIssue(entityRevision, "birthDateBefore1900", birthDate);
            }

            if (birthDate.isAfter(LocalDate.now())) {
                reportIssue(entityRevision, "birthDateInFuture", birthDate);
            }
        }

        if (!StringUtil.valueExists(personalInfoDTO.getOrcid())) {
            reportIssue(entityRevision, "noOrcidPresent");
        } else {
            if (!ORCID_PATTERN.matcher(personalInfoDTO.getOrcid()).matches()) {
                reportIssue(entityRevision, "invalidOrcidFormat", personalInfoDTO.getOrcid());
            }

            if (personRepository.existsByOrcid(personalInfoDTO.getOrcid(), dto.getId())) {
                reportIssue(entityRevision, "duplicateOrcid", personalInfoDTO.getOrcid());
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getWebOfScienceResearcherId())) {
            var rid = personalInfoDTO.getWebOfScienceResearcherId();

            if (rid.length() < 11) {
                reportIssue(entityRevision, "webOfScienceResearcherIdTooShort", rid);
            }

            if (rid.length() > 11) {
                reportIssue(entityRevision, "webOfScienceResearcherIdTooLong", rid);
            }

            if (!WEB_OF_SCIENCE_RESEARCHER_ID_PATTERN.matcher(rid).matches()) {
                reportIssue(entityRevision, "invalidWebOfScienceResearcherIdFormat", rid);
            }

            if (personRepository.existsByWebOfScienceId(rid, dto.getId())) {
                reportIssue(entityRevision, "duplicateWebOfScienceResearcherId", rid);
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getScopusAuthorId())) {
            var id = personalInfoDTO.getScopusAuthorId();

            if (!SCOPUS_AUTHOR_ID_PATTERN.matcher(id).matches()) {
                reportIssue(entityRevision, "invalidScopusAuthorIdFormat", id);
            }

            if (personRepository.existsByScopusAuthorId(id, dto.getId())) {
                reportIssue(entityRevision, "duplicateScopusAuthorId", id);
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getOpenAlexId())) {
            if (!OPENALEX_PATTERN.matcher(personalInfoDTO.getOpenAlexId()).matches()) {
                reportIssue(entityRevision, "invalidOpenAlexIdFormat",
                    personalInfoDTO.getOpenAlexId());
            }

            if (personRepository.existsByOpenAlexId(personalInfoDTO.getOpenAlexId(), dto.getId())) {
                reportIssue(entityRevision, "duplicateOpenAlexId", personalInfoDTO.getOpenAlexId());
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getScholarId())) {
            if (!GOOGLE_SCHOLAR_PATTERN.matcher(personalInfoDTO.getScholarId()).matches()) {
                reportIssue(entityRevision, "invalidGoogleScholarIdFormat",
                    personalInfoDTO.getScholarId());
            }

            if (personRepository.existsByScholarId(personalInfoDTO.getScholarId(), dto.getId())) {
                reportIssue(entityRevision, "duplicateGoogleScholarId",
                    personalInfoDTO.getScholarId());
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getLattesId())) {
            if (!LATTES_PATTERN.matcher(personalInfoDTO.getLattesId()).matches()) {
                reportIssue(entityRevision, "invalidLattesIdFormat", personalInfoDTO.getLattesId());
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getNationalScienceId())) {
            if (!CIENCIA_ID_PATTERN.matcher(personalInfoDTO.getNationalScienceId()).matches()) {
                reportIssue(entityRevision, "invalidCienciaIdFormat",
                    personalInfoDTO.getNationalScienceId());
            }
        }

        if (StringUtil.valueExists(personalInfoDTO.getAuthenticusId())) {
            if (personRepository.existsByAuthenticusId(personalInfoDTO.getAuthenticusId(),
                dto.getId())) {
                reportIssue(entityRevision, "duplicateAuthenticusId",
                    personalInfoDTO.getAuthenticusId());
            }
        }

        var personNames = new ArrayList<>(List.of(dto.getPersonName()));
        personNames.addAll(dto.getPersonOtherNames());

        if (!CollectionOperations.containsValues(personNames)) {
            reportIssue(entityRevision, "nameMissing");
        } else {
            personNames.forEach(name -> {
                if (!StringUtil.valueExists(name.getFirstname())) {
                    reportIssue(entityRevision, "firstNameMissing");
                }

                if (!StringUtil.valueExists(name.getLastname())) {
                    reportIssue(entityRevision, "lastNameMissing");
                }

                if (StringUtil.valueExists(name.getFirstname()) &&
                    name.getFirstname().length() > 100) {
                    reportIssue(entityRevision, "firstNameTooLong", name.getFirstname());
                }

                if (StringUtil.valueExists(name.getLastname()) &&
                    name.getLastname().length() > 100) {
                    reportIssue(entityRevision, "lastNameTooLong", name.getLastname());
                }

                if (StringUtil.valueExists(name.getFirstname()) &&
                    !PERSON_NAME_PATTERN.matcher(name.getFirstname()).matches()) {
                    reportIssue(entityRevision, "invalidFirstNameFormat", name.getFirstname());
                }

                if (StringUtil.valueExists(name.getLastname()) &&
                    !PERSON_NAME_PATTERN.matcher(name.getLastname()).matches()) {
                    reportIssue(entityRevision, "invalidLastNameFormat", name.getLastname());
                }
            });
        }

        if (!CollectionOperations.containsValues(dto.getBiography())) {
            reportIssue(entityRevision, "biographyMissing");
        }

        assessEntity(personalInfoDTO.getContact(), entityRevision);
        assessEntity(personalInfoDTO.getPrivateContact(), entityRevision);

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(OrganisationUnitDTO dto, EntityRevision entityRevision) {
        if (!CollectionOperations.containsValues(dto.getName())) {
            reportIssue(entityRevision, "organisationUnitNameMissing");
        } else {
            dto.getName().forEach(name -> {

                String value = name.getContent();

                if (!StringUtil.valueExists(value)) {
                    reportIssue(entityRevision, "organisationUnitNameMissing");
                    return;
                }

                if (value.length() > 255) {
                    reportIssue(entityRevision, "organisationUnitNameTooLong", value);
                }

                if (!ORGANISATION_NAME_PATTERN.matcher(value).matches()) {
                    reportIssue(entityRevision, "invalidOrganisationUnitNameFormat", value);
                }
            });
        }

        if (!CollectionOperations.containsValues(dto.getDescription())) {
            reportIssue(entityRevision, "organisationUnitDescriptionMissing");
        }

        if (StringUtil.valueExists(dto.getRor())) {

            if (!ROR_PATTERN.matcher(dto.getRor()).matches()) {
                reportIssue(entityRevision, "invalidRorFormat", dto.getRor());
            }

            if (organisationUnitRepository.existsByROR(dto.getRor(), dto.getId())) {
                reportIssue(entityRevision, "duplicateRor", dto.getRor());
            }
        }

        if (StringUtil.valueExists(dto.getIsni())) {

            if (!ISNI_PATTERN.matcher(dto.getIsni()).matches()) {
                reportIssue(entityRevision, "invalidIsniFormat", dto.getIsni());
            }

            if (organisationUnitRepository.existsByIsni(dto.getIsni(), dto.getId())) {
                reportIssue(entityRevision, "duplicateIsni", dto.getIsni());
            }
        }

        if (StringUtil.valueExists(dto.getScopusAfid())) {

            if (!SCOPUS_AFID_PATTERN.matcher(dto.getScopusAfid()).matches()) {
                reportIssue(entityRevision, "invalidScopusAfidFormat", dto.getScopusAfid());
            }

            if (organisationUnitRepository.existsByScopusAfid(dto.getScopusAfid(), dto.getId())) {
                reportIssue(entityRevision, "duplicateScopusAfid", dto.getScopusAfid());
            }
        }

        if (StringUtil.valueExists(dto.getGrid())) {
            if (!GRID_PATTERN.matcher(dto.getGrid()).matches()) {
                reportIssue(entityRevision, "invalidGridFormat", dto.getGrid());
            }

            if (organisationUnitRepository.existsByGrid(dto.getGrid(), dto.getId())) {
                reportIssue(entityRevision, "duplicateGrid", dto.getGrid());
            }
        }

        if (StringUtil.valueExists(dto.getRinggold())) {
            if (!RINGGOLD_PATTERN.matcher(dto.getRinggold()).matches()) {
                reportIssue(entityRevision, "invalidRinggoldFormat", dto.getRinggold());
            }

            if (organisationUnitRepository.existsByRinggold(dto.getRinggold(), dto.getId())) {
                reportIssue(entityRevision, "duplicateRinggold", dto.getRinggold());
            }
        }

        if (StringUtil.valueExists(dto.getFundref())) {
            if (!FUNDREF_PATTERN.matcher(dto.getFundref()).matches()) {
                reportIssue(entityRevision, "invalidFundrefFormat", dto.getFundref());
            }
        }

        if (Objects.nonNull(dto.getDateEstablished()) &&
            Objects.nonNull(dto.getDateDissolved()) &&
            dto.getDateDissolved().isBefore(dto.getDateEstablished())) {

            reportIssue(
                entityRevision,
                "dateDissolvedBeforeEstablished",
                dto.getDateEstablished(),
                dto.getDateDissolved()
            );
        }

        assessEntity(dto.getContact(), entityRevision);
        assessEntity(dto.getLocation(), entityRevision);

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(CountryDTO dto, EntityRevision entityRevision) {
        if (!StringUtil.valueExists(dto.getCode())) {
            reportIssue(entityRevision, "countryCodeMissing");
        } else {
            if (dto.getCode().length() != 2) {
                reportIssue(entityRevision, "countryCodeInvalidLength", dto.getCode());
            }

            if (!COUNTRY_CODE_PATTERN.matcher(dto.getCode()).matches()) {
                reportIssue(entityRevision, "invalidCountryCodeFormat", dto.getCode());
            }

            if (countryRepository.existsByCode(dto.getCode(), dto.getId())) {
                reportIssue(entityRevision, "duplicateCountryCode", dto.getCode());
            }
        }

        if (!CollectionOperations.containsValues(dto.getName())) {
            reportIssue(entityRevision, "countryNameMissing");
        } else {
            dto.getName().forEach(name -> {
                var value = name.getContent();

                if (!StringUtil.valueExists(value)) {
                    reportIssue(entityRevision, "countryNameMissing");
                    return;
                }

                if (value.length() > 255) {
                    reportIssue(entityRevision, "countryNameTooLong", value);
                }

                if (!COUNTRY_NAME_PATTERN.matcher(value).matches()) {
                    reportIssue(entityRevision, "invalidCountryNameFormat", value);
                }
            });
        }

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(ContactDTO dto, EntityRevision entityRevision) {
        if (StringUtil.valueExists(dto.getContactEmail())) {
            if (dto.getContactEmail().length() > 255) {
                reportIssue(entityRevision, "contactEmailTooLong", dto.getContactEmail());
            }

            if (!EMAIL_PATTERN.matcher(dto.getContactEmail()).matches()) {
                reportIssue(entityRevision, "invalidContactEmailFormat", dto.getContactEmail());
            }
        }

        if (StringUtil.valueExists(dto.getPhoneNumber())) {
            if (dto.getPhoneNumber().length() > 30) {
                reportIssue(entityRevision, "phoneNumberTooLong", dto.getPhoneNumber());
            }

            if (!PHONE_NUMBER_PATTERN.matcher(dto.getPhoneNumber()).matches()) {
                reportIssue(entityRevision, "invalidPhoneNumberFormat", dto.getPhoneNumber());
            }
        }

        if (StringUtil.valueExists(dto.getMobilePhoneNumber())) {
            if (dto.getMobilePhoneNumber().length() > 30) {
                reportIssue(entityRevision, "mobilePhoneNumberTooLong", dto.getMobilePhoneNumber());
            }

            if (!PHONE_NUMBER_PATTERN.matcher(dto.getMobilePhoneNumber()).matches()) {
                reportIssue(entityRevision, "invalidMobilePhoneNumberFormat",
                    dto.getMobilePhoneNumber());
            }
        }

        if (StringUtil.valueExists(dto.getFaxNumber())) {
            if (dto.getFaxNumber().length() > 30) {
                reportIssue(entityRevision, "faxNumberTooLong", dto.getFaxNumber());
            }

            if (!PHONE_NUMBER_PATTERN.matcher(dto.getFaxNumber()).matches()) {
                reportIssue(entityRevision, "invalidFaxNumberFormat", dto.getFaxNumber());
            }
        }

        // TODO: what website?
//    if (StringUtil.valueExists(dto.getWebsite())) {
//        if (dto.getWebsite().length() > 255) {
//            reportIssue(entityRevision, "contactWebsiteTooLong", dto.getWebsite());
//        }
//
//        if (!URL_PATTERN.matcher(dto.getWebsite()).matches()) {
//            reportIssue(entityRevision, "invalidContactWebsiteFormat", dto.getWebsite());
//        }
//    }
    }

    private void assessEntity(LanguageResponseDTO dto, EntityRevision entityRevision) {
        if (!StringUtil.valueExists(dto.getLanguageCode())) {
            reportIssue(entityRevision, "languageTagMissing");
        } else {
            if (dto.getLanguageCode().length() > 10) {
                reportIssue(entityRevision, "languageTagTooLong", dto.getLanguageCode());
            }

            if (!LANGUAGE_TAG_PATTERN.matcher(dto.getLanguageCode()).matches()) {
                reportIssue(entityRevision, "invalidLanguageTagFormat", dto.getLanguageCode());
            }

            if (languageRepository.existsByCode(dto.getLanguageCode(), dto.getId())) {
                reportIssue(entityRevision, "duplicateLanguageTag", dto.getLanguageCode());
            }
        }

        if (!CollectionOperations.containsValues(dto.getName())) {
            reportIssue(entityRevision, "languageNameMissing");
        } else {
            dto.getName().forEach(name -> {
                var value = name.getContent();

                if (!StringUtil.valueExists(value)) {
                    reportIssue(entityRevision, "languageNameMissing");
                    return;
                }

                if (value.length() > 255) {
                    reportIssue(entityRevision, "languageNameTooLong", value);
                }

                if (!LANGUAGE_NAME_PATTERN.matcher(value).matches()) {
                    reportIssue(entityRevision, "invalidLanguageNameFormat", value);
                }
            });
        }

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(ResearchAreaDTO dto, EntityRevision entityRevision) {
        if (!CollectionOperations.containsValues(dto.getName())) {
            reportIssue(entityRevision, "researchAreaNameMissing");
        } else {
            dto.getName().forEach(name -> {
                var value = name.getContent();

                if (!StringUtil.valueExists(value)) {
                    reportIssue(entityRevision, "researchAreaNameMissing");
                    return;
                }

                if (value.length() > 255) {
                    reportIssue(entityRevision, "researchAreaNameTooLong", value);
                }

                if (!RESEARCH_AREA_NAME_PATTERN.matcher(value).matches()) {
                    reportIssue(entityRevision, "invalidResearchAreaNameFormat", value);
                }
            });
        }

        // TODO: What URI?
//    if (StringUtil.valueExists(dto.getUri())) {
//        if (dto.getUri().length() > 255) {
//            reportIssue(entityRevision, "researchAreaUriTooLong", dto.getUri());
//        }
//
//        if (!URI_PATTERN.matcher(dto.getUri()).matches()) {
//            reportIssue(entityRevision, "invalidResearchAreaUriFormat", dto.getUri());
//        }
//
//        if (researchAreaRepository.existsByUri(dto.getUri(), dto.getId())) {
//            reportIssue(entityRevision, "duplicateResearchAreaUri", dto.getUri());
//        }
//    }

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(GeoLocationDTO dto, EntityRevision entityRevision) {
        if (Objects.isNull(dto.getLatitude())) {
            reportIssue(entityRevision, "latitudeMissing");
        } else if (dto.getLatitude() < -90 || dto.getLatitude() > 90) {
            reportIssue(entityRevision, "latitudeOutOfRange", dto.getLatitude());
        }

        if (Objects.isNull(dto.getLongitude())) {
            reportIssue(entityRevision, "longitudeMissing");
        } else if (dto.getLongitude() < -180 || dto.getLongitude() > 180) {
            reportIssue(entityRevision, "longitudeOutOfRange", dto.getLongitude());
        }

        if (StringUtil.valueExists(dto.getAddress())) {

            if (dto.getAddress().length() > 500) {
                reportIssue(entityRevision, "addressTooLong", dto.getAddress());
            }

            if (!ADDRESS_PATTERN.matcher(dto.getAddress()).matches()) {
                reportIssue(entityRevision, "invalidAddressFormat", dto.getAddress());
            }
        }

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(IdentifierResponseDTO dto, EntityRevision entityRevision) {
        //TODO: Identifier does not hold value
        // TODO: Type not needed as it is modeled using inheritance

        if (StringUtil.valueExists(dto.regularExpression())) {
            if (dto.regularExpression().length() > 255) {
                reportIssue(entityRevision, "identifierRegularExpressionTooLong",
                    dto.regularExpression());
            }

            try {
                Pattern.compile(dto.regularExpression());
            } catch (PatternSyntaxException ex) {
                reportIssue(entityRevision, "invalidIdentifierRegularExpression",
                    dto.regularExpression());
            }
        }

        if (StringUtil.valueExists(dto.uriPrefix())) {
            if (dto.uriPrefix().length() > 255) {
                reportIssue(entityRevision, "identifierUriTooLong", dto.uriPrefix());
            }

            if (!URI_PATTERN.matcher(dto.uriPrefix()).matches()) {
                reportIssue(entityRevision, "invalidIdentifierUriFormat", dto.uriPrefix());
            }
        }

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(InvolvementDTO dto, EntityRevision entityRevision) {
        if (Objects.isNull(dto.getDateFrom())) {
            reportIssue(entityRevision, "activityStartDateMissing");
        } else {
            if (dto.getDateFrom().isBefore(LocalDate.of(1950, 1, 1))) {
                reportIssue(
                    entityRevision,
                    "activityStartDateBefore1950",
                    dto.getDateFrom()
                );
            }

            if (Objects.nonNull(dto.getPersonBirthDate()) &&
                dto.getDateFrom().isBefore(dto.getPersonBirthDate().plusYears(15))) {
                reportIssue(
                    entityRevision,
                    "activityStartDateBeforePersonTurned15",
                    dto.getDateFrom(),
                    dto.getPersonBirthDate()
                );
            }

            if (dto.getDateFrom().isAfter(LocalDate.now().plusYears(3))) {
                reportIssue(
                    entityRevision,
                    "activityStartDateTooFarInFuture",
                    dto.getDateFrom()
                );
            }
        }

        if (Objects.isNull(dto.getDateTo())) {
            reportIssue(entityRevision, "activityEndDateMissing");
        } else if (Objects.nonNull(dto.getDateFrom()) &&
            dto.getDateTo().isBefore(dto.getDateFrom())) {
            reportIssue(
                entityRevision,
                "activityEndDateBeforeStartDate",
                dto.getDateTo(),
                dto.getDateFrom()
            );
        }

        if (!CollectionOperations.containsValues(dto.getResearchAreasId())) {
            reportIssue(entityRevision, "activityResearchAreasMissing");
        }

        // TODO metadataLicenseMissing
        // TODO metadataAccessLevelMissing
        // TODO createDateMissing
        // TODO lastModificationDateMissing
    }

    private void assessEntity(PersonContributionDTO dto, EntityRevision entityRevision,
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
            reportIssue(entityRevision, "activityStartDateMissing");
        } else {
            if (dto.getDateFrom().isBefore(LocalDate.of(1950, 1, 1))) {
                reportIssue(
                    entityRevision,
                    "activityStartDateBefore1950",
                    dto.getDateFrom()
                );
            }

            if (Objects.nonNull(birthDate) && dto.getDateFrom().isBefore(birthDate.plusYears(15))) {
                reportIssue(
                    entityRevision,
                    "activityStartDateBeforePersonTurned15",
                    dto.getDateFrom(),
                    birthDate
                );
            }

            if (dto.getDateFrom().isAfter(LocalDate.now().plusYears(3))) {
                reportIssue(
                    entityRevision,
                    "activityStartDateTooFarInFuture",
                    dto.getDateFrom()
                );
            }
        }

        if (Objects.isNull(dto.getDateTo())) {
            reportIssue(entityRevision, "activityEndDateMissing");
        } else if (Objects.nonNull(dto.getDateFrom()) &&
            dto.getDateTo().isBefore(dto.getDateFrom())) {
            reportIssue(
                entityRevision,
                "activityEndDateBeforeStartDate",
                dto.getDateTo(), dto.getDateFrom()
            );
        }

        if (!CollectionOperations.containsValues(dto.getResearchAreasId())) {
            reportIssue(entityRevision, "activityResearchAreasMissing");
        }

        if (dto instanceof PersonEventContributionDTO eventContribution) {
            boolean linkedWithCourse = eventType.equals(EventType.COURSE);

            if (!linkedWithCourse) {
                if (Objects.nonNull(eventContribution.getLectureHoursPerWeek())) {
                    reportIssue(entityRevision,
                        "lectureHoursOnlyForCourse");
                }

                if (Objects.nonNull(eventContribution.getTutorialHoursPerWeek())) {
                    reportIssue(entityRevision,
                        "tutorialHoursOnlyForCourse");
                }

                if (Objects.nonNull(eventContribution.getLabHoursPerWeek())) {
                    reportIssue(entityRevision,
                        "labHoursOnlyForCourse");
                }

                if (Objects.nonNull(eventContribution.getOtherContactHoursPerWeek())) {
                    reportIssue(entityRevision,
                        "otherContactHoursOnlyForCourse");
                }
            }

            boolean reviewerContribution =
                eventContribution.getEventContributionType().equals(EventContributionType.REVIEWER);

            boolean linkedWithConference = eventType.equals(EventType.CONFERENCE);

            if (Objects.nonNull(eventContribution.getNumberOfReviewsOrAssessment())) {
                if (eventContribution.getNumberOfReviewsOrAssessment() > 20) {
                    reportIssue(
                        entityRevision,
                        "numberOfReviewsTooHigh",
                        eventContribution.getNumberOfReviewsOrAssessment()
                    );
                }

                if (!(linkedWithConference && reviewerContribution)) {
                    reportIssue(entityRevision, "numberOfReviewsOnlyForConferenceReviewer");
                }
            }

            boolean trial = eventType.equals(EventType.OTHER_EVENT) &&
                otherEventType.equals(OtherEventType.TRIAL);

            if (CollectionOperations.containsValues(eventContribution.getCaseName()) && !trial) {
                reportIssue(entityRevision, "caseOnlyForTrial");
            }

            if (CollectionOperations.containsValues(eventContribution.getLocationJurisdiction()) &&
                !trial) {
                reportIssue(entityRevision, "locationJurisdictionOnlyForTrial");
            }
        }

        if (dto instanceof PersonDocumentContributionDTO documentContribution) {
            if (Objects.nonNull(documentDate) && Objects.nonNull(birthDate) &&
                documentDate.isBefore(birthDate)) {
                reportIssue(
                    entityRevision,
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
                    entityRevision,
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
                    entityRevision,
                    "invalidCorrespondingContributorFlag",
                    documentContribution.getContributionType().name()
                );
            }
        }

        assessEntity(dto.getContact(), entityRevision);

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
                               EntityRevision entityRevision) {
        if (Objects.isNull(value)) {
            return;
        }

        if (value < min) {
            reportIssue(entityRevision, fieldName + "BelowMinimum");
        }

        if (value > max) {
            reportIssue(entityRevision, fieldName + "AboveMaximum");
        }
    }

    private void reportIssue(EntityRevision entityRevision, String remarkKey, Object... params) {
        if (Objects.isNull(entityRevision.getQualityDataReport())) {
            entityRevision.setQualityDataReport(new HashSet<>());
        }

        var stringParams = Strings.join(Arrays.asList(params), ',');
        entityRevision.getQualityDataReport()
            .add(remarkKey + ":" + (!stringParams.isBlank() ? stringParams : "N/A"));
    }
}
