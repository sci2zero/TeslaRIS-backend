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

        addTitle(document, record);
        addDoiIfPresent(document, record);
        if (!setPublicationMetadata(document, record)) {
            return Optional.empty();
        }
        document.setDocumentDate(String.valueOf(record.source().publishYear()));
        setPageInfo(document, record);
        addContributions(document, record);
        addKeywords(document, record);
        addUris(document, record);

        return Optional.of(document);
    }

    private static void addTitle(DocumentImport document,
                                 WebOfScienceImportUtility.WosPublication record) {
        document.getTitle().add(new MultilingualContent("EN", record.title(), 1));
    }

    private static void addDoiIfPresent(DocumentImport document,
                                        WebOfScienceImportUtility.WosPublication record) {
        if (Objects.nonNull(record.identifiers()) && Objects.nonNull(record.identifiers().doi()) &&
            !record.identifiers().doi().contains("***")) {
            document.setDoi(record.identifiers().doi());
        }
    }

    private static boolean setPublicationMetadata(DocumentImport document,
                                                  WebOfScienceImportUtility.WosPublication record) {
        var sourceTypes = record.sourceTypes();

        if (sourceTypes.contains("Article") || sourceTypes.contains("Meeting")) {
            document.setPublicationType(DocumentPublicationType.JOURNAL_PUBLICATION);
            document.getPublishedIn()
                .add(new MultilingualContent("EN", record.source().sourceTitle(), 1));
            addIssn(document, record);
        } else if (sourceTypes.contains("Proceedings Paper")) {
            document.setPublicationType(DocumentPublicationType.PROCEEDINGS_PUBLICATION);
            addProceedingsMetadata(document, record);
        } else {
            return false;
        }

        return true;
    }

    private static void addIssn(DocumentImport document,
                                WebOfScienceImportUtility.WosPublication record) {
        var identifiers = record.identifiers();
        if (identifiers == null) {
            return;
        }

        if (Objects.nonNull(identifiers.issn()) && !identifiers.issn().contains("***")) {
            document.setPrintIssn(identifiers.issn());
        } else if (Objects.nonNull(identifiers.eissn()) && !identifiers.eissn().contains("***")) {
            document.setEIssn(identifiers.eissn());
        }
    }

    private static void addProceedingsMetadata(DocumentImport document,
                                               WebOfScienceImportUtility.WosPublication record) {
        var conferenceName = record.source().sourceTitle();
        document.getPublishedIn()
            .add(new MultilingualContent("EN", "Proceedings of " + conferenceName, 1));

        var event = new Event();
        event.getName().add(new MultilingualContent("EN", conferenceName, 1));
        event.setDateFrom(LocalDate.of(record.source().publishYear(), 1, 1));
        event.setDateTo(LocalDate.of(record.source().publishYear(), 12, 31));
        document.setEvent(event);

        var identifiers = record.identifiers();
        if (identifiers == null) {
            return;
        }

        if (Objects.nonNull(identifiers.isbn()) && !identifiers.isbn().contains("***")) {
            document.setIsbn(identifiers.isbn());
        } else if (Objects.nonNull(identifiers.eisbn()) && !identifiers.eisbn().contains("***")) {
            document.setEisbn(identifiers.eisbn());
        }
    }

    private static void setPageInfo(DocumentImport document,
                                    WebOfScienceImportUtility.WosPublication record) {
        document.setStartPage(record.source().pages().begin());
        document.setEndPage(record.source().pages().end());
        document.setNumberOfPages(record.source().pages().count());
    }

    private static void addContributions(DocumentImport document,
                                         WebOfScienceImportUtility.WosPublication record) {
        FunctionalUtil.forEachWithCounter(record.names().authors(), (i, authorship) -> {
            var contribution = new PersonDocumentContribution();
            contribution.setOrderNumber(i + 1);
            contribution.setContributionType(DocumentContributionType.AUTHOR);
            contribution.setIsCorrespondingContributor(false);

            var person = new Person();
            person.setImportId(authorship.researcherId());
            person.setWebOfScienceResearcherId(person.getImportId());

            var nameParts = authorship.displayName().split(", ");
            person.setName(new PersonName(nameParts[1], "", nameParts[0]));

            contribution.setPerson(person);
            document.getContributions().add(contribution);
        });
    }

    private static void addKeywords(DocumentImport document,
                                    WebOfScienceImportUtility.WosPublication record) {
        var keywords = new ArrayList<>(record.keywords().authorKeywords());
        document.getKeywords().add(new MultilingualContent("EN", Strings.join(keywords, '\n'), 1));
    }

    private static void addUris(DocumentImport document,
                                WebOfScienceImportUtility.WosPublication record) {
        document.getUris().add(record.links().record());
    }
}
