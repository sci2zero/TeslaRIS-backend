package rs.teslaris.importer.model.converter.harvest;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.JournalPublicationType;
import rs.teslaris.core.model.document.ProceedingsPublicationType;
import rs.teslaris.core.model.skgif.agent.Agent;
import rs.teslaris.core.model.skgif.agent.SKGIFOrganisation;
import rs.teslaris.core.model.skgif.agent.SKGIFPerson;
import rs.teslaris.core.model.skgif.researchproduct.ResearchProduct;
import rs.teslaris.core.model.skgif.venue.Venue;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.MultilingualContent;
import rs.teslaris.importer.model.common.OrganisationUnit;
import rs.teslaris.importer.model.common.Person;
import rs.teslaris.importer.model.common.PersonDocumentContribution;
import rs.teslaris.importer.model.common.PersonName;
import rs.teslaris.importer.utility.skgif.SKGIFImportUtility;

@Slf4j
public class SKGIFConverter {

    public static Optional<DocumentImport> toCommonImportModel(ResearchProduct record,
                                                               String sourceIdentifierPrefix,
                                                               String baseUrl) {
        if (((Objects.isNull(record.getManifestations()) || record.getManifestations().isEmpty()) &&
            !StringUtil.valueExists(record.getCreationDate()))) {
            return Optional.empty();
        }

        if (record.getManifestations().stream().allMatch(m -> Objects.isNull(m.getBiblio()) ||
            !StringUtil.valueExists(m.getBiblio().getIn()))) {
            return Optional.empty();
        }

        var document = new DocumentImport();

        setCommonFields(record, document, sourceIdentifierPrefix);
        if (!StringUtil.valueExists(document.getDocumentDate()) || document.getTitle().isEmpty()) {
            return Optional.empty();
        }

        if (record.getProductType().equals("literature")) {
            var bibliographicInfo = record.getManifestations().stream().filter(
                    m -> Objects.nonNull(m.getBiblio()) &&
                        StringUtil.valueExists(m.getBiblio().getIn())).findFirst().get()
                .getBiblio();

            var venueId = bibliographicInfo.getIn();
            var optionalVenue =
                SKGIFImportUtility.fetchEntityFromExternalGraph(venueId, "venue", Venue.class,
                    baseUrl);
            if (optionalVenue.isEmpty()) {
                return Optional.empty();
            }

            var venue = optionalVenue.get().getFirst();

            if (!List.of("conference", "journal").contains(venue.getType().toLowerCase())) {
                log.error("Unsupported venue type: {}", venue.getType());
                return Optional.empty();
            }

            setVenueInformation(venue, document);

            document.setVolume(bibliographicInfo.getVolume());
            document.setIssue(bibliographicInfo.getIssue());

            if (Objects.nonNull(bibliographicInfo.getPages())) {
                document.setStartPage(bibliographicInfo.getPages().getFirst());
                document.setEndPage(bibliographicInfo.getPages().getLast());
            }
        } else {
            log.error("Unsupported product type: {}", record.getProductType());
            return Optional.empty();
        }

        setContributorInformation(record, document, baseUrl, sourceIdentifierPrefix);

        return Optional.of(document);
    }

    private static void setCommonFields(ResearchProduct record, DocumentImport document,
                                        String sourceIdentifierPrefix) {
        var identifier = record.getLocalIdentifier();
        if (record.getLocalIdentifier().contains(")")) {
            identifier = record.getLocalIdentifier().split("\\)")[1];
        }
        document.setIdentifier(sourceIdentifierPrefix + ":" + identifier);
        document.getInternalIdentifiers().add(document.getIdentifier());

        if (StringUtil.valueExists(record.getCreationDate())) {
            document.setDocumentDate(record.getCreationDate().split("T")[0].substring(0, 4));
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
                return;
            }

            document.setDocumentDate(earliestDistributionDate.get().split("T")[0].substring(0, 4));
        }

        record.getTitles().forEach((lang, title) -> {
            if (title.isEmpty()) {
                return;
            }

            document.getTitle()
                .add(new MultilingualContent(lang.replace("@", "").toUpperCase(), title.getFirst(),
                    document.getTitle().size() + 1));
        });

        if (Objects.nonNull(record.getAbstracts())) {
            record.getAbstracts().forEach((lang, description) -> {
                if (description.isEmpty()) {
                    return;
                }

                document.getDescription()
                    .add(new MultilingualContent(lang.replace("@", "").toUpperCase(),
                        description.getFirst(),
                        document.getDescription().size() + 1));
            });
        }

        if (Objects.nonNull(record.getIdentifiers())) {
            record.getIdentifiers().stream()
                .filter(i -> i.getScheme().equals("openalex"))
                .findFirst().ifPresent(
                    i ->
                        document.setOpenAlexId(i.getValue()));

            record.getIdentifiers().stream()
                .filter(i -> i.getScheme().equals("doi"))
                .findFirst().ifPresent(
                    i ->
                        document.setDoi(i.getValue()));

            record.getIdentifiers().stream()
                .filter(i -> i.getScheme().equals("uri") && i.getValue().contains("scopus"))
                .findFirst().ifPresent(
                    i ->
                        document.setScopusId(
                            i.getValue().replace("https://www.scopus.com/pages/publications/", "")));
        }
    }

    private static void setVenueInformation(Venue venue, DocumentImport document) {
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
    }

    private static void setContributorInformation(ResearchProduct record, DocumentImport document,
                                                  String baseUrl, String sourceIdentifierPrefix) {
        FunctionalUtil.forEachWithCounter(record.getContributions(), (i, contribution) -> {
            if (!contribution.getRole().equalsIgnoreCase("author")) {
                return;
            }

            var authorship = new PersonDocumentContribution();
            authorship.setOrderNumber(i + 1);
            authorship.setContributionType(DocumentContributionType.AUTHOR);
            authorship.setIsCorrespondingContributor(false);

            if (contribution.getBy().startsWith("(" + sourceIdentifierPrefix + ")")) {
                SKGIFImportUtility.fetchEntityFromExternalGraph(
                    contribution.getBy(),
                    "person",
                    Agent.class,
                    baseUrl
                ).ifPresent(agents -> {
                    var author = agents.getFirst();

                    var person = new Person();
                    person.setName(
                        new PersonName(((SKGIFPerson) author).getGivenName(), "",
                            ((SKGIFPerson) author).getFamilyName()));

                    if (Objects.nonNull(author.getIdentifiers())) {
                        author.getIdentifiers().stream()
                            .filter(id -> id.getScheme().equals("orcid"))
                            .findFirst().ifPresent(
                                id ->
                                    person.setOrcid(id.getValue()));

                        author.getIdentifiers().stream()
                            .filter(id -> id.getScheme().equals("openalex"))
                            .findFirst().ifPresent(
                                id ->
                                    person.setOpenAlexId(id.getValue()));

                        author.getIdentifiers().stream()
                            .filter(id -> id.getScheme().equals("uri") &&
                                id.getValue().contains("scopus"))
                            .findFirst().ifPresent(
                                id ->
                                    person.setScopusAuthorId(
                                        id.getValue().replace(
                                            "https://www.scopus.com/authid/detail.uri?authorId=", "")));
                    }

                    agents.stream().filter(agent -> agent instanceof SKGIFOrganisation)
                        .forEach(organisation -> {
                            var institution = new OrganisationUnit();

                            institution.getName()
                                .add(new MultilingualContent("EN", organisation.getName(), 1));

                            if (Objects.nonNull(organisation.getIdentifiers())) {
                                organisation.getIdentifiers().stream()
                                    .filter(identifier -> identifier.getScheme().equals("openalex"))
                                    .findFirst().ifPresent(
                                        identifier ->
                                            institution.setOpenAlexId(identifier.getValue()));

                                organisation.getIdentifiers().stream()
                                    .filter(identifier -> identifier.getScheme().equals("ror"))
                                    .findFirst().ifPresent(
                                        identifier ->
                                            institution.setRor(identifier.getValue()));
                            }

                            if (StringUtil.valueExists(institution.getOpenAlexId())) {
                                institution.setImportId(institution.getOpenAlexId());
                            } else {
                                institution.setImportId(organisation.getLocalIdentifier());
                            }

                            authorship.getInstitutions().add(institution);
                        });

                    if (StringUtil.valueExists(person.getOrcid())) {
                        person.setImportId(person.getOrcid());
                    } else if (StringUtil.valueExists(person.getScopusAuthorId())) {
                        person.setImportId(person.getScopusAuthorId());
                    } else if (StringUtil.valueExists(person.getOpenAlexId())) {
                        person.setImportId(person.getOpenAlexId());
                    } else {
                        person.setImportId(String.valueOf(author.getLocalIdentifier()));
                    }

                    authorship.setPerson(person);
                    document.getContributions().add(authorship);
                });
            } else {
                var person = new Person();
                var nameParts = contribution.getBy().split(" ");
                person.setName(new PersonName(nameParts[0], "", nameParts[1]));
                person.setImportId(String.valueOf(authorship.getOrderNumber()));

                authorship.setPerson(person);
                document.getContributions().add(authorship);
            }
        });
    }
}
