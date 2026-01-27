package rs.teslaris.importer.model.converter.harvest;


import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.JournalPublicationType;
import rs.teslaris.core.model.document.ProceedingsPublicationType;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.Event;
import rs.teslaris.importer.model.common.MultilingualContent;
import rs.teslaris.importer.model.common.OrganisationUnit;
import rs.teslaris.importer.model.common.Person;
import rs.teslaris.importer.model.common.PersonDocumentContribution;
import rs.teslaris.importer.model.common.PersonName;
import rs.teslaris.importer.utility.openalex.OpenAlexImportUtility;

public class OpenAlexConverter {

    public static Optional<DocumentImport> toCommonImportModel(
        OpenAlexImportUtility.OpenAlexPublication record) {

        var document = new DocumentImport();
        document.setSource("OPEN_ALEX");

        if (!hasValidSource(record)) {
            return Optional.empty();
        }

        populateBasicFields(document, record);

        var publicationYear = extractPublicationYear(record.publicationDate());

        if (Objects.nonNull(record.typeCrossref())) {
            switch (record.typeCrossref()) {
                case "journal-article" -> handleJournalArticle(document, record);
                case "proceedings-article" ->
                    handleProceedingsArticle(document, record, publicationYear);
                default -> {
                    return Optional.empty();
                }
            }
        } else if (Objects.nonNull(record.type()) &&
            (record.type().equals("article") || record.type().equals("review")) &&
            Objects.nonNull(record.primaryLocation()) &&
            Objects.nonNull(record.primaryLocation().source()) &&
            StringUtil.valueExists(record.primaryLocation().source().type())) {
            switch (record.primaryLocation().source().type()) {
                case "journal" -> handleJournalArticle(document, record);
                case "conference" -> handleProceedingsArticle(document, record, publicationYear);
                default -> {
                    return Optional.empty();
                }
            }
        } else {
            return Optional.empty();
        }

        populateContributions(document, record);

        populateKeywords(document, record);

        return Optional.of(document);
    }

    private static boolean hasValidSource(OpenAlexImportUtility.OpenAlexPublication record) {
        return Objects.nonNull(record.primaryLocation()) &&
            Objects.nonNull(record.primaryLocation().source());
    }

    private static void populateBasicFields(DocumentImport document,
                                            OpenAlexImportUtility.OpenAlexPublication record) {
        var publicationYear = extractPublicationYear(record.publicationDate());
        document.setDocumentDate(String.valueOf(publicationYear));
        var cleanId = cleanOpenAlexId(record.id());
        document.setIdentifier(cleanId);
        document.setOpenAlexId(cleanId);

        if (Objects.nonNull(record.doi())) {
            document.setDoi(record.doi().replace("https://doi.org/", ""));
        }

        document.getTitle().add(new MultilingualContent("EN", record.title(), 1));
    }

    private static void handleJournalArticle(DocumentImport document,
                                             OpenAlexImportUtility.OpenAlexPublication record) {
        var source = record.primaryLocation().source();
        var sourceId = cleanOpenAlexId(source.id());

        document.setPublicationType(DocumentPublicationType.JOURNAL_PUBLICATION);
        document.setJournalPublicationType(
            record.type().equals("review") ? JournalPublicationType.REVIEW_ARTICLE :
                JournalPublicationType.RESEARCH_ARTICLE);
        document.getPublishedIn().add(new MultilingualContent("EN", source.displayName(), 1));
        document.setJournalOpenAlexId(sourceId);

        var issns = source.issn();
        if (Objects.nonNull(issns) && !issns.isEmpty()) {
            document.setPrintIssn(issns.getFirst());
            if (issns.size() > 1) {
                document.setEIssn(issns.getLast());
            }
        }
    }

    private static void handleProceedingsArticle(DocumentImport document,
                                                 OpenAlexImportUtility.OpenAlexPublication record,
                                                 int publicationYear) {
        var source = record.primaryLocation().source();
        var sourceId = cleanOpenAlexId(source.id());

        document.setPublicationType(DocumentPublicationType.PROCEEDINGS_PUBLICATION);
        document.setProceedingsPublicationType(ProceedingsPublicationType.REGULAR_FULL_ARTICLE);
        document.getPublishedIn()
            .add(new MultilingualContent("EN", "Proceedings of " + source.displayName(), 1));

        var event = new Event();
        event.setOpenAlexId(sourceId);
        event.getName().add(new MultilingualContent("EN", source.displayName(), 1));
        event.setDateFrom(LocalDate.of(publicationYear, 1, 1));
        event.setDateTo(LocalDate.of(publicationYear, 12, 31));

        document.setEvent(event);
    }

    private static void populateContributions(DocumentImport document,
                                              OpenAlexImportUtility.OpenAlexPublication record) {
        FunctionalUtil.forEachWithCounter(record.authorships(), (i, authorship) -> {
            var contribution = new PersonDocumentContribution();
            contribution.setOrderNumber(i + 1);
            contribution.setContributionType(DocumentContributionType.AUTHOR);
            contribution.setIsCorrespondingContributor(authorship.isCorresponding());

            var person = constructPerson(authorship);
            contribution.setPerson(person);

            authorship.institutions().forEach(institution -> {
                var organisationUnit = new OrganisationUnit();
                organisationUnit.setImportId(cleanOpenAlexId(institution.id()));
                organisationUnit.getName()
                    .add(new MultilingualContent("EN", institution.displayName(), 1));
                if (Objects.nonNull(institution.ror())) {
                    organisationUnit.setRor(institution.ror().replace("https://ror.org/", ""));
                }
                organisationUnit.setOpenAlexId(organisationUnit.getImportId());

                contribution.getInstitutions().add(organisationUnit);
            });

            document.getContributions().add(contribution);
        });
    }

    private static void populateKeywords(DocumentImport document,
                                         OpenAlexImportUtility.OpenAlexPublication record) {
        var keywords = record.keywords().stream()
            .map(OpenAlexImportUtility.OpenAlexPublication.Keyword::displayName)
            .toList();
        document.getKeywords().add(new MultilingualContent("EN", String.join("\n", keywords), 1));
    }

    private static String cleanOpenAlexId(String url) {
        return url.replace("https://openalex.org/", "");
    }

    private static int extractPublicationYear(String date) {
        return Integer.parseInt(date.split("-")[0]);
    }

    @NotNull
    private static Person constructPerson(
        OpenAlexImportUtility.OpenAlexPublication.Authorship authorship) {
        var person = new Person();
        person.setImportId(cleanOpenAlexId(authorship.author().id()));
        person.setOpenAlexId(person.getImportId());

        if (Objects.nonNull(authorship.author().orcid())) {
            person.setOrcid(authorship.author().orcid().replace("https://orcid.org/", ""));
        }

        var personNameParts = authorship.author().displayName().split(" ");
        person.setName(new PersonName(personNameParts[0], "", personNameParts[1]));
        return person;
    }
}
