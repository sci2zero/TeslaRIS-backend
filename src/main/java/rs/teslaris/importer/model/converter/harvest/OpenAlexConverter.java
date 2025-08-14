package rs.teslaris.importer.model.converter.harvest;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import org.apache.logging.log4j.util.Strings;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.util.FunctionalUtil;
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

        boolean hasSpecifiedSource = Objects.nonNull(record.primaryLocation().source());
        String sourceId = "";
        if (hasSpecifiedSource) {
            sourceId = record.primaryLocation().source().id().replace("https://openalex.org/", "");
        }

        if (record.typeCrossref().equals("journal-article") &&
            hasSpecifiedSource) {
            document.setPublicationType(DocumentPublicationType.JOURNAL_PUBLICATION);
            var journalName = record.primaryLocation().source().displayName();
            document.getPublishedIn().add(new MultilingualContent("EN", journalName, 1));
            document.setJournalOpenAlexId(sourceId);

            if (Objects.nonNull(record.primaryLocation().source().issn())) {
                var issns = record.primaryLocation().source().issn();
                if (issns.size() == 2) {
                    document.setPrintIssn(issns.getFirst());
                    document.setEIssn(issns.getLast());
                } else if (!issns.isEmpty()) {
                    document.setPrintIssn(issns.getFirst());
                }
            }
        } else if (record.typeCrossref().equals("proceedings-article") &&
            hasSpecifiedSource) {
            document.setPublicationType(DocumentPublicationType.PROCEEDINGS_PUBLICATION);
            var conferenceName = record.primaryLocation().source().displayName();
            document.getPublishedIn()
                .add(new MultilingualContent("EN", "Proceedings of " + conferenceName, 1));

            var event = new Event();
            event.setOpenAlexId(sourceId);
            event.getName().add(new MultilingualContent("EN", conferenceName, 1));
            document.setEvent(event);
        } else {
            return Optional.empty();
        }

        document.setIdentifier(record.id().replace("https://openalex.org/", ""));
        document.setOpenAlexId(record.id().replace("https://openalex.org/", ""));

        if (Objects.nonNull(record.doi())) {
            document.setDoi(record.doi().replace("https://doi.org/", ""));
        }

        var publicationYear = Integer.parseInt(record.publicationDate().split("-")[0]);
        document.setDocumentDate(String.valueOf(publicationYear));
        document.getEvent().setDateFrom(LocalDate.of(publicationYear, 1, 1));
        document.getEvent().setDateTo(LocalDate.of(publicationYear, 12, 31));

        document.getTitle().add(new MultilingualContent("EN", record.title(), 1));

        FunctionalUtil.forEachWithCounter(record.authorships(), (i, authorship) -> {
            var contribution = new PersonDocumentContribution();
            contribution.setOrderNumber(i + 1);
            contribution.setContributionType(DocumentContributionType.AUTHOR);
            contribution.setIsCorrespondingContributor(authorship.isCorresponding());

            var person = new Person();
            person.setImportId(authorship.author().id().replace("https://openalex.org/", ""));
            person.setOpenAlexId(person.getImportId());

            if (Objects.nonNull(authorship.author().orcid())) {
                person.setOrcid(authorship.author().orcid().replace("https://orcid.org/", ""));
            }

            var personNameParts = authorship.author().displayName().split(" ");
            person.setName(new PersonName(personNameParts[0], "", personNameParts[1]));
            contribution.setPerson(person);

            authorship.institutions().forEach(institution -> {
                var organisationUnit = new OrganisationUnit();
                organisationUnit.setImportId(institution.id().replace("https://openalex.org/", ""));
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

        var keywords = new ArrayList<String>();
        record.keywords().forEach(keyword -> {
            keywords.add(keyword.displayName());
        });
        document.getKeywords().add(new MultilingualContent("EN", Strings.join(keywords, '\n'), 1));

        return Optional.of(document);
    }
}
