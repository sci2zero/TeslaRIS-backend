package rs.teslaris.importer.model.converter.harvest;

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
import rs.teslaris.importer.model.common.Person;
import rs.teslaris.importer.model.common.PersonDocumentContribution;
import rs.teslaris.importer.model.common.PersonName;
import rs.teslaris.importer.utility.webofscience.WebOfScienceImportUtility;

public class WebOfScienceConverter {

    public static Optional<DocumentImport> toCommonImportModel(
        WebOfScienceImportUtility.WosPublication record) {
        var document = new DocumentImport();
        document.setIdentifier(record.uid().replace("WOS:", ""));
        document.setWebOfScienceId(document.getIdentifier());

        document.getTitle().add(new MultilingualContent("EN", record.title(), 1));

        if (Objects.nonNull(record.identifiers()) && Objects.nonNull(record.identifiers().doi()) &&
            !record.identifiers().doi().contains("***")) {
            document.setDoi(record.identifiers().doi());
        }

        if (record.sourceTypes().contains("Article") || record.sourceTypes().contains("Meeting")) {
            document.setPublicationType(DocumentPublicationType.JOURNAL_PUBLICATION);
            document.getPublishedIn()
                .add(new MultilingualContent("EN", record.source().sourceTitle(), 1));
            if (Objects.nonNull(record.identifiers()) &&
                Objects.nonNull(record.identifiers().issn()) &&
                !record.identifiers().issn().contains("***")) {
                document.setPrintIssn(record.identifiers().issn());
            } else if (Objects.nonNull(record.identifiers()) &&
                Objects.nonNull(record.identifiers().eissn()) &&
                !record.identifiers().eissn().contains("***")) {
                document.setEIssn(record.identifiers().eissn());
            }
        } else if (record.sourceTypes().contains("Proceedings Paper")) {
            document.setPublicationType(DocumentPublicationType.PROCEEDINGS_PUBLICATION);

            var conferenceName = record.source().sourceTitle();
            document.getPublishedIn()
                .add(new MultilingualContent("EN", "Proceedings of " + conferenceName, 1));

            var event = new Event();
            event.getName().add(new MultilingualContent("EN", conferenceName, 1));
            document.setEvent(event);

            document.getPublishedIn()
                .add(new MultilingualContent("EN", record.source().sourceTitle(), 1));
            if (Objects.nonNull(record.identifiers()) &&
                Objects.nonNull(record.identifiers().isbn()) &&
                !record.identifiers().isbn().contains("***")) {
                document.setIsbn(record.identifiers().isbn());
            } else if (Objects.nonNull(record.identifiers()) &&
                Objects.nonNull(record.identifiers().eisbn()) &&
                !record.identifiers().eisbn().contains("***")) {
                document.setEisbn(record.identifiers().eisbn());
            }
        } else {
            return Optional.empty();
        }

        document.setDocumentDate(String.valueOf(record.source().publishYear()));

        document.setStartPage(record.source().pages().begin());
        document.setEndPage(record.source().pages().end());
        document.setNumberOfPages(record.source().pages().count());

        FunctionalUtil.forEachWithCounter(record.names().authors(), (i, authorship) -> {
            var contribution = new PersonDocumentContribution();
            contribution.setOrderNumber(i + 1);
            contribution.setContributionType(DocumentContributionType.AUTHOR);
            contribution.setIsCorrespondingContributor(false);

            var person = new Person();
            person.setImportId(authorship.researcherId());
            person.setOpenAlexId(person.getImportId());

            var personNameParts = authorship.displayName().split(", ");
            person.setName(new PersonName(personNameParts[1], "", personNameParts[0]));
            contribution.setPerson(person);

            document.getContributions().add(contribution);
        });

        var keywords = new ArrayList<>(record.keywords().authorKeywords());
        document.getKeywords().add(new MultilingualContent("EN", Strings.join(keywords, '\n'), 1));

        document.getUris().add(record.links().record());

        return Optional.of(document);
    }
}
