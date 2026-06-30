package rs.teslaris.revisioner.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.GeneticMaterialDTO;
import rs.teslaris.core.dto.document.IntangibleProductDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.dto.document.MaterialProductDTO;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.institution.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.util.search.CollectionOperations;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.session.RestTemplateProvider;
import rs.teslaris.revisioner.model.EntityRevision;

@Component
@RequiredArgsConstructor
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
    private final Pattern DOI_PATTERN =
        Pattern.compile("^10\\.\\d{4,9}/[-._;()/:A-Za-z0-9]+$");
    private final Pattern HANDLE_PATTERN =
        Pattern.compile("^\\d{2}\\.\\d{3,5}(\\.\\d+)?/\\S+$");
    private final RevisionHydratorRegistry revisionHydratorRegistry;

    private final DocumentRepository documentRepository;

    private final PersonRepository personRepository;

    private final OrganisationUnitRepository organisationUnitRepository;

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
        Map.entry(PersonalInfoDTO.class, (dto, rev) -> assessEntity((PersonalInfoDTO) dto, rev))
    );


    public void assessDataQuality(EntityRevision entityRevision, String json,
                                  ObjectMapper objectMapper)
        throws JsonProcessingException {
        Class<?> dtoClass = revisionHydratorRegistry.getDtoClass(entityRevision.getEntityType());

        Object dto = objectMapper.treeToValue(objectMapper.readTree(json), dtoClass);
        assessEntity(dto, entityRevision);
    }

    private void assessEntity(Object dto, EntityRevision entityRevision) {
        BiConsumer<Object, EntityRevision> assessor = assessors.get(dto.getClass());

        if (Objects.nonNull(assessor)) {
            assessor.accept(dto, entityRevision);
        }
    }

    private void assessEntity(DocumentDTO dto, EntityRevision entityRevision) {
        entityRevision.setQualityDataScore(0.0);

        if (!CollectionOperations.containsValues(dto.getTitle())) {
            reportIssue(entityRevision, "titleMissing");
        } else {
            dto.getTitle().forEach(title -> {
                var value = title.getContent();

                if (!StringUtil.valueExists(value)) {
                    reportIssue(entityRevision, "invalidTitleFormat");
                    return;
                }

                if (value.length() > 255) {
                    reportIssue(entityRevision, "titleTooLong");
                }

                if (!TITLE_PATTERN.matcher(value).matches()) {
                    reportIssue(entityRevision, "invalidTitleFormat");
                }
            });
        }

        if (!CollectionOperations.containsValues(dto.getDescription())) {
            reportIssue(entityRevision, "descriptionMissing");
        }

        if (!CollectionOperations.containsValues(dto.getContributions())) {
            reportIssue(entityRevision, "contributorsMissing");
        } else {
            boolean hasManagedPerson =
                dto.getContributions()
                    .stream()
                    .anyMatch(c -> Objects.nonNull(c.getPersonId()));

            if (!hasManagedPerson) {
                reportIssue(entityRevision, "noManagedContributor");
            }
        }

        if (!StringUtil.valueExists(dto.getDocumentDate())) {
            reportIssue(entityRevision, "documentDateMissing");
        } else {
            try {
                LocalDate date = StringUtil.parseDocumentDate(dto.getDocumentDate());

                if (date.isBefore(LocalDate.of(1950, 1, 1))) {
                    reportIssue(entityRevision, "documentDateBefore1950");
                }

                if (date.isAfter(LocalDate.now().plusYears(3))) {
                    reportIssue(entityRevision, "documentDateTooFarInFuture");
                }
            } catch (Exception e) {
                reportIssue(entityRevision, "invalidDocumentDateFormat");
            }
        }

        if (!StringUtil.valueExists(dto.getDoi())) {
            reportIssue(entityRevision, "noDoiPresent");
        } else {
            var doi = dto.getDoi();

            if (doi.length() < 9) {
                reportIssue(entityRevision, "doiTooShort");
            }

            if (doi.length() > 255) {
                reportIssue(entityRevision, "doiTooLong");
            }

            if (!DOI_PATTERN.matcher(doi).matches()) {
                reportIssue(entityRevision, "invalidDoiFormat");
            }

            if (documentRepository.existsByDoi(doi, dto.getId())) {
                reportIssue(entityRevision, "duplicateDoi");
            }

            if (!isResolvableDoi(doi)) {
                reportIssue(entityRevision, "doiNotResolvable");
            }
        }

        if (!StringUtil.valueExists(dto.getHandleId())) {
            reportIssue(entityRevision, "noHandlePresent");
        } else {
            var handle = dto.getHandleId();

            if (handle.length() < 8) {
                reportIssue(entityRevision, "handleTooShort");
            }

            if (handle.length() > 255) {
                reportIssue(entityRevision, "handleTooLong");
            }

            if (!HANDLE_PATTERN.matcher(handle).matches()) {
                reportIssue(entityRevision, "invalidHandleFormat");
            }

            if (documentRepository.existsByHandleId(handle, dto.getId())) {
                reportIssue(entityRevision, "duplicateHandle");
            }

            if (!isResolvableHandle(handle)) {
                reportIssue(entityRevision, "handleNotResolvable");
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
                if (thesis.getTopicAcceptanceDate()
                    .isBefore(LocalDate.of(1950, 1, 1))) {
                    reportIssue(entityRevision, "topicAcceptanceDateBefore1950");
                }

                if (thesis.getTopicAcceptanceDate()
                    .isAfter(LocalDate.now())) {
                    reportIssue(entityRevision, "topicAcceptanceDateFuture");
                }
            }

            if (Objects.isNull(thesis.getThesisDefenceDate())) {
                reportIssue(entityRevision, "thesisDefenceDateMissing");
            } else {
                if (Objects.nonNull(thesis.getTopicAcceptanceDate()) &&
                    thesis.getThesisDefenceDate().isBefore(thesis.getTopicAcceptanceDate())) {
                    reportIssue(entityRevision, "defenceBeforeAcceptance");
                }

                if (thesis.getThesisDefenceDate().isAfter(LocalDate.now().plusYears(1))) {
                    reportIssue(entityRevision, "defenceTooFarInFuture");
                }
            }
        }

        // TODO: metadataLicenseMissing
    }

    private void assessEntity(Set<PersonNameDTO> dto, EntityRevision entityRevision) {
        if (!CollectionOperations.containsValues(dto)) {
            reportIssue(entityRevision, "nameMissing");
        } else {
            dto.forEach(name -> {
                if (!StringUtil.valueExists(name.getFirstname())) {
                    reportIssue(entityRevision, "firstNameMissing");
                }

                if (!StringUtil.valueExists(name.getLastname())) {
                    reportIssue(entityRevision, "lastNameMissing");
                }

                if (StringUtil.valueExists(name.getFirstname()) &&
                    name.getFirstname().length() > 100) {
                    reportIssue(entityRevision, "firstNameTooLong");
                }

                if (StringUtil.valueExists(name.getLastname()) &&
                    name.getLastname().length() > 100) {
                    reportIssue(entityRevision, "lastNameTooLong");
                }

                if (!PERSON_NAME_PATTERN.matcher(name.getFirstname()).matches()) {
                    reportIssue(entityRevision, "invalidFirstNameFormat");
                }

                if (!PERSON_NAME_PATTERN.matcher(name.getLastname()).matches()) {
                    reportIssue(entityRevision, "invalidLastNameFormat");
                }
            });
        }
    }

    private void assessEntity(PersonalInfoDTO dto, EntityRevision entityRevision) {
        if (Objects.isNull(dto.getLocalBirthDate())) {
            reportIssue(entityRevision, "birthDateMissing");
        } else {
            var birthDate = dto.getLocalBirthDate();

            if (birthDate.isBefore(LocalDate.of(1900, 1, 1))) {
                reportIssue(entityRevision, "birthDateBefore1900");
            }

            if (birthDate.isAfter(LocalDate.now())) {
                reportIssue(entityRevision, "birthDateInFuture");
            }
        }

        if (!StringUtil.valueExists(dto.getOrcid())) {
            reportIssue(entityRevision, "noOrcidPresent");
        } else {

            if (!ORCID_PATTERN.matcher(dto.getOrcid()).matches()) {
                reportIssue(entityRevision, "invalidOrcidFormat");
            }

            if (personRepository.existsByOrcid(dto.getOrcid(), dto.getId())) {
                reportIssue(entityRevision, "duplicateOrcid");
            }
        }

        if (StringUtil.valueExists(dto.getWebOfScienceResearcherId())) {
            var rid = dto.getWebOfScienceResearcherId();

            if (rid.length() < 11) {
                reportIssue(entityRevision, "webOfScienceResearcherIdTooShort");
            }

            if (rid.length() > 11) {
                reportIssue(entityRevision, "webOfScienceResearcherIdTooLong");
            }

            if (!WEB_OF_SCIENCE_RESEARCHER_ID_PATTERN.matcher(rid).matches()) {
                reportIssue(entityRevision, "invalidWebOfScienceResearcherIdFormat");
            }

            if (personRepository.existsByWebOfScienceId(rid, dto.getId())) {
                reportIssue(entityRevision, "duplicateWebOfScienceResearcherId");
            }
        }

        if (StringUtil.valueExists(dto.getScopusAuthorId())) {
            var id = dto.getScopusAuthorId();

            if (!SCOPUS_AUTHOR_ID_PATTERN.matcher(id).matches()) {
                reportIssue(entityRevision, "invalidScopusAuthorIdFormat");
            }

            if (personRepository.existsByScopusAuthorId(id, dto.getId())) {
                reportIssue(entityRevision, "duplicateScopusAuthorId");
            }
        }

        if (StringUtil.valueExists(dto.getOpenAlexId())) {
            if (!OPENALEX_PATTERN.matcher(dto.getOpenAlexId()).matches()) {
                reportIssue(entityRevision, "invalidOpenAlexIdFormat");
            }

            if (personRepository.existsByOpenAlexId(dto.getOpenAlexId(), dto.getId())) {
                reportIssue(entityRevision, "duplicateOpenAlexId");
            }
        }

        if (StringUtil.valueExists(dto.getScholarId())) {
            if (!GOOGLE_SCHOLAR_PATTERN.matcher(dto.getScholarId()).matches()) {
                reportIssue(entityRevision, "invalidGoogleScholarIdFormat");
            }

            if (personRepository.existsByScholarId(dto.getScholarId(), dto.getId())) {
                reportIssue(entityRevision, "duplicateGoogleScholarId");
            }
        }

        if (StringUtil.valueExists(dto.getLattesId())) {
            if (!LATTES_PATTERN.matcher(dto.getLattesId()).matches()) {
                reportIssue(entityRevision, "invalidLattesIdFormat");
            }
        }

        if (StringUtil.valueExists(dto.getNationalScienceId())) {
            if (!CIENCIA_ID_PATTERN.matcher(dto.getNationalScienceId()).matches()) {
                reportIssue(entityRevision, "invalidCienciaIdFormat");
            }
        }

        if (StringUtil.valueExists(dto.getAuthenticusId())) {
            if (personRepository.existsByAuthenticusId(dto.getAuthenticusId(), dto.getId())) {
                reportIssue(entityRevision, "duplicateAuthenticusId");
            }
        }

//        if (!CollectionOperations.containsValues(dto.getBiography())) {
//            reportIssue(entityRevision, "biographyMissing");
//        }

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
                    reportIssue(entityRevision, "organisationUnitNameTooLong");
                }

                if (!ORGANISATION_NAME_PATTERN.matcher(value).matches()) {
                    reportIssue(entityRevision, "invalidOrganisationUnitNameFormat");
                }
            });
        }

        if (!CollectionOperations.containsValues(dto.getDescription())) {
            reportIssue(entityRevision, "organisationUnitDescriptionMissing");
        }

        if (StringUtil.valueExists(dto.getRor())) {

            if (!ROR_PATTERN.matcher(dto.getRor()).matches()) {
                reportIssue(entityRevision, "invalidRorFormat");
            }

            if (organisationUnitRepository.existsByROR(dto.getRor(), dto.getId())) {
                reportIssue(entityRevision, "duplicateRor");
            }
        }

        if (StringUtil.valueExists(dto.getIsni())) {

            if (!ISNI_PATTERN.matcher(dto.getIsni()).matches()) {
                reportIssue(entityRevision, "invalidIsniFormat");
            }

            if (organisationUnitRepository.existsByIsni(dto.getIsni(), dto.getId())) {
                reportIssue(entityRevision, "duplicateIsni");
            }
        }

        if (StringUtil.valueExists(dto.getScopusAfid())) {

            if (!SCOPUS_AFID_PATTERN.matcher(dto.getScopusAfid()).matches()) {
                reportIssue(entityRevision, "invalidScopusAfidFormat");
            }

            if (organisationUnitRepository.existsByScopusAfid(dto.getScopusAfid(), dto.getId())) {
                reportIssue(entityRevision, "duplicateScopusAfid");
            }
        }

        if (StringUtil.valueExists(dto.getGrid())) {
            if (!GRID_PATTERN.matcher(dto.getGrid()).matches()) {
                reportIssue(entityRevision, "invalidGridFormat");
            }

            if (organisationUnitRepository.existsByGrid(dto.getGrid(), dto.getId())) {
                reportIssue(entityRevision, "duplicateGrid");
            }
        }

        if (StringUtil.valueExists(dto.getRinggold())) {
            if (!RINGGOLD_PATTERN.matcher(dto.getRinggold()).matches()) {
                reportIssue(entityRevision, "invalidRinggoldFormat");
            }

            if (organisationUnitRepository.existsByRinggold(dto.getRinggold(), dto.getId())) {
                reportIssue(entityRevision, "duplicateRinggold");
            }
        }

        if (StringUtil.valueExists(dto.getFundref())) {
            if (!FUNDREF_PATTERN.matcher(dto.getFundref()).matches()) {
                reportIssue(entityRevision, "invalidFundrefFormat");
            }
        }

        if (Objects.nonNull(dto.getDateEstablished()) &&
            Objects.nonNull(dto.getDateDissolved()) &&
            dto.getDateDissolved().isBefore(dto.getDateEstablished())) {

            reportIssue(entityRevision, "dateDissolvedBeforeEstablished");
        }

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
        var newRemarks = RevisionConfigurationLoader.getDataQualityRemark(remarkKey, params);

        Map<String, MultiLingualContent> existingRemarks =
            entityRevision.getQualityDataReport()
                .stream()
                .collect(Collectors.toMap(
                    mc -> mc.getLanguage().getLanguageTag(),
                    Function.identity(),
                    (left, right) -> left));

        for (var newRemark : newRemarks) {
            var languageTag = newRemark.getLanguage().getLanguageTag();

            var existing = existingRemarks.get(languageTag);

            if (Objects.isNull(existing)) {
                entityRevision.getQualityDataReport().add(newRemark);
                existingRemarks.put(languageTag, newRemark);
            } else {
                existing.setContent(
                    existing.getContent()
                        + System.lineSeparator()
                        + System.lineSeparator()
                        + newRemark.getContent()
                );
            }
        }
    }
}
