package rs.teslaris.revisioner.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
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
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.document.DocumentRepository;
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
    private final RevisionHydratorRegistry revisionHydratorRegistry;
    private final DocumentRepository documentRepository;
    private final RestTemplateProvider restTemplateProvider;
    private final Pattern DOI_PATTERN =
        Pattern.compile("^10\\.\\d{4,9}/[-._;()/:A-Za-z0-9]+$");
    private final Pattern HANDLE_PATTERN =
        Pattern.compile("^\\d{2}\\.\\d{3,5}(\\.\\d+)?/\\S+$");
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

    private void assessEntity(PersonalInfoDTO dto, EntityRevision entityRevision) {
        if (!StringUtil.valueExists(dto.getOrcid())) {
            System.out.println("OrcidIsRecommended");
        }
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
