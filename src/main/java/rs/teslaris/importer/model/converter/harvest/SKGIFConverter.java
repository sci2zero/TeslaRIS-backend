package rs.teslaris.importer.model.converter.harvest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.JournalPublicationType;
import rs.teslaris.core.model.document.ProceedingsPublicationType;
import rs.teslaris.core.model.skgif.agent.SKGIFPerson;
import rs.teslaris.core.model.skgif.researchproduct.ResearchProduct;
import rs.teslaris.core.model.skgif.venue.Venue;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.session.RestTemplateProvider;
import rs.teslaris.exporter.model.skgif.SKGIFSingleResponse;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.MultilingualContent;
import rs.teslaris.importer.model.common.Person;
import rs.teslaris.importer.model.common.PersonDocumentContribution;
import rs.teslaris.importer.model.common.PersonName;

@Slf4j
@Component
public class SKGIFConverter {

    private static RestTemplateProvider restTemplateProvider;


    @Autowired
    public SKGIFConverter(RestTemplateProvider restTemplateProvider) {
        SKGIFConverter.restTemplateProvider = restTemplateProvider;
    }

    public static Optional<DocumentImport> toCommonImportModel(ResearchProduct record,
                                                               String sourceIdentifierPrefix,
                                                               String baseUrl) {
        if (Objects.isNull(record.getManifestations()) || record.getManifestations().isEmpty() ||
            record.getManifestations().stream().allMatch(m -> Objects.isNull(m.getBiblio()) ||
                !StringUtil.valueExists(m.getBiblio().getIn()))) {
            return Optional.empty();
        }

        var document = new DocumentImport();

        var identifier = record.getLocalIdentifier();
        if (record.getLocalIdentifier().contains(")")) {
            identifier = record.getLocalIdentifier().split("\\)")[1];
        }
        document.setIdentifier(sourceIdentifierPrefix + ":" + identifier);
        document.getInternalIdentifiers().add(document.getIdentifier());

        if (StringUtil.valueExists(record.getCreationDate())) {
            document.setDocumentDate(record.getCreationDate());
        } else {
            Optional<String> earliestDistributionDate = record.getManifestations().stream()
                .filter(m -> Objects.nonNull(m.getDates()))
                .filter(m -> Objects.nonNull(m.getDates().getDistribution()))
                .filter(m -> !m.getDates().getDistribution().trim().isEmpty())
                .min(Comparator.comparing(m -> {
                    try {
                        return LocalDate.parse(m.getDates().getDistribution().split("T")[0]);
                    } catch (Exception e) {
                        return LocalDate.MAX;
                    }
                }))
                .map(m -> m.getDates().getDistribution());

            if (earliestDistributionDate.isEmpty()) {
                return Optional.empty();
            }

            document.setDocumentDate(earliestDistributionDate.get());
        }

        record.getTitles().forEach((lang, title) -> {
            if (title.isEmpty()) {
                return;
            }

            document.getTitle()
                .add(new MultilingualContent(lang.replace("@", "").toUpperCase(), title.getFirst(),
                    document.getTitle().size() + 1));
        });

        record.getAbstracts().forEach((lang, description) -> {
            if (description.isEmpty()) {
                return;
            }

            document.getDescription()
                .add(new MultilingualContent(lang.replace("@", "").toUpperCase(),
                    description.getFirst(),
                    document.getDescription().size() + 1));
        });

        if (record.getProductType().equals("literature")) {
            var venueId = record.getManifestations().stream().filter(
                    m -> Objects.nonNull(m.getBiblio()) &&
                        StringUtil.valueExists(m.getBiblio().getIn())).findFirst().get()
                .getBiblio()
                .getIn();

            var optionalVenue = fetchEntityFromExternalGraph(venueId, Venue.class, baseUrl);
            if (optionalVenue.isEmpty()) {
                return Optional.empty();
            }

            var venue = optionalVenue.get();

            if (!List.of("conference", "journal").contains(venue.getType().toLowerCase())) {
                log.error("Unsupported venue type: {}", venue.getType());
                return Optional.empty();
            }

            if (venue.getType().equalsIgnoreCase("journal")) {
                document.setPublicationType(DocumentPublicationType.JOURNAL_PUBLICATION);
                document.setJournalPublicationType(JournalPublicationType.RESEARCH_ARTICLE);
            } else {
                document.setPublicationType(DocumentPublicationType.PROCEEDINGS_PUBLICATION);
                document.setProceedingsPublicationType(
                    ProceedingsPublicationType.REGULAR_FULL_ARTICLE);
            }

            document.getPublishedIn()
                .add(new MultilingualContent("EN", venue.getTitle(), 1));

            if (venue.getIdentifiers().stream().anyMatch(id -> id.getScheme().equals("issn"))) {
                document.setPrintIssn(
                    venue.getIdentifiers().stream().filter(id -> id.getScheme().equals("issn"))
                        .findFirst().get().getValue());
            }

            if (venue.getIdentifiers().stream().anyMatch(id -> id.getScheme().equals("eissn"))) {
                document.setEIssn(
                    venue.getIdentifiers().stream().filter(id -> id.getScheme().equals("eissn"))
                        .findFirst().get().getValue());
            }

            if (venue.getIdentifiers().stream().anyMatch(id -> id.getScheme().equals("isbn"))) {
                document.setEisbn(
                    venue.getIdentifiers().stream().filter(id -> id.getScheme().equals("isbn"))
                        .findFirst().get().getValue());
            }
        } else {
            log.error("Unsupported product type: {}", record.getProductType());
            return Optional.empty();
        }

        FunctionalUtil.forEachWithCounter(record.getContributions(), (i, contribution) -> {
            if (!contribution.getRole().equalsIgnoreCase("author")) {
                return;
            }

            var authorship = new PersonDocumentContribution();
            authorship.setOrderNumber(i + 1);
            authorship.setContributionType(DocumentContributionType.AUTHOR);
            authorship.setIsCorrespondingContributor(false);

            fetchEntityFromExternalGraph(
                contribution.getBy(),
                SKGIFPerson.class,
                baseUrl
            ).ifPresent(author -> {
                var person = new Person();
                person.setName(
                    new PersonName(author.getGivenName(), "", author.getFamilyName()));

                if (author.getIdentifiers().stream()
                    .anyMatch(id -> id.getScheme().equals("orcid"))) {
                    person.setOrcid(
                        author.getIdentifiers().stream()
                            .filter(id -> id.getScheme().equals("orcid"))
                            .findFirst().get().getValue());
                }

                authorship.setPerson(person);
                document.getContributions().add(authorship);
            });
        });

        return Optional.of(document);
    }

    public static <T> Optional<T> fetchEntityFromExternalGraph(String entityId,
                                                               Class<T> entityClass,
                                                               String baseUrl) {
        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String fetchUrl = baseUrl + "venue/" + entityId;
        ResponseEntity<String> responseEntity =
            restTemplateProvider.provideRestTemplate().getForEntity(fetchUrl, String.class);

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            return Optional.empty();
        }

        try {
            var result =
                objectMapper.readValue(responseEntity.getBody(), SKGIFSingleResponse.class);

            if (result.getGraph().isEmpty()) {
                return Optional.empty();
            }

            return Optional.of((T) result.getGraph().getFirst());
        } catch (HttpClientErrorException e) {
            log.error("HTTP error for SKG-IF {} ID {}: {}", entityClass.getName(), entityId,
                e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("JSON parsing error for SKG-IF {} ID {}: {}", entityClass.getName(), entityId,
                e.getMessage());
        } catch (ResourceAccessException e) {
            log.error("SKG-IF {} source is unreachable for ID {}: {}", entityClass.getName(),
                entityId, e.getMessage());
        }

        return Optional.empty();
    }
}
