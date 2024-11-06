package rs.teslaris.core.importer.model.converter.harvest;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import rs.teslaris.core.importer.model.common.DocumentImport;
import rs.teslaris.core.importer.model.common.Event;
import rs.teslaris.core.importer.model.common.MultilingualContent;
import rs.teslaris.core.importer.model.common.OrganisationUnit;
import rs.teslaris.core.importer.model.common.Person;
import rs.teslaris.core.importer.model.common.PersonDocumentContribution;
import rs.teslaris.core.importer.model.common.PersonName;
import rs.teslaris.core.importer.utility.scopus.ScopusImportUtility;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.DocumentContributionType;

public class ScopusConverter {

    public static Optional<DocumentImport> toCommonImportModel(ScopusImportUtility.Entry entry,
                                                               ScopusImportUtility scopusImportUtility) {
        var document = new DocumentImport();

        deducePublicationType(entry, document, scopusImportUtility);
        if (Objects.isNull(document.getPublicationType())) {
            return Optional.empty();
        }

        setCommonFields(entry, document);

        document.setScopusId(entry.identifier().split(":")[1]); // format is SCOPUS_ID:XXX

        if (Objects.nonNull(entry.eIssn())) {
            document.setEIssn(entry.eIssn().substring(0, 4) + "-" + entry.eIssn().substring(4));
        }

        if (Objects.nonNull(entry.issn())) {
            document.setPrintIssn(entry.issn().substring(0, 4) + "-" + entry.issn().substring(4));
        }

        if (Objects.nonNull(entry.isbn()) && !entry.isbn().isEmpty()) {
            document.setIsbn(entry.isbn().getFirst().value());
        }

        document.setDoi(entry.doi());

        if (Objects.nonNull(entry.pageRange())) {
            var pages = entry.pageRange().split("-");
            document.setStartPage(pages[0]);
            if (pages.length > 1) {
                document.setEndPage(pages[1]);
                document.setNumberOfPages(Integer.parseInt(pages[1]) - Integer.parseInt(pages[0]));
            }
        }

        return Optional.of(document);
    }

    private static void deducePublicationType(ScopusImportUtility.Entry entry,
                                              DocumentImport document,
                                              ScopusImportUtility scopusImportUtility) {
        switch (entry.subtypeDescription()) {
            case "Article":
            case "Short Survey":
            case "Review":
            case "Data Paper":
            case "Business Article":
            case "Note":
            case "Letter":
            case "Editorial":
                document.setPublicationType(DocumentPublicationType.JOURNAL_PUBLICATION);
                break;
            case "Conference Paper":
                document.setPublicationType(DocumentPublicationType.PROCEEDINGS_PUBLICATION);

                var abstractData = scopusImportUtility.getAbstractData(entry.identifier());
                setConferenceInfo(abstractData, document);
                break;
        }
    }

    protected static void setCommonFields(ScopusImportUtility.Entry entry,
                                          DocumentImport document) {
        entry.links().forEach(link -> {
            document.getUris().add(link.href());
        });
        document.getUris().add(entry.url());

        document.setDocumentDate(entry.coverDate());

        document.getTitle().add(new MultilingualContent("EN", entry.title(), 1));

        if (Objects.nonNull(document.getDescription()) && !document.getDescription().isEmpty()) {
            document.getDescription().add(new MultilingualContent("EN", entry.description(), 1));
        }

        document.getPublishedIn().add(new MultilingualContent("EN", entry.publicationName(), 1));

        if (Objects.nonNull(entry.authKeywords())) {
            document.getKeywords()
                .add(new MultilingualContent("EN", entry.authKeywords().replace("|", "\n"), 1));
        }

        setContributionInformation(entry, document);
    }

    @NotNull
    private static Person getPerson(ScopusImportUtility.Author author) {
        var person = new Person();
        person.setScopusAuthorId(author.authId());
        var authorName = author.authName().split(" ");
        if (authorName.length == 2) {
            person.setName(new PersonName(authorName[0], "", authorName[1]));
        } else if (authorName.length == 3) {
            person.setName(new PersonName(authorName[0], authorName[1], authorName[2]));
        } else {
            person.setName(new PersonName(author.givenName(), "", author.surname()));
        }
        return person;
    }

    private static void setContributionInformation(ScopusImportUtility.Entry entry,
                                                   DocumentImport document) {
        entry.authors().forEach(author -> {
            var contribution = new PersonDocumentContribution();
            contribution.setContributionType(DocumentContributionType.AUTHOR);
            contribution.setOrderNumber(Integer.parseInt(author.seq()));

            var person = getPerson(author);
            contribution.setPerson(person);

            if (Objects.nonNull(author.afid())) {
                author.afid().forEach(authorAfid -> {
                    var authorAffiliation = entry.affiliations().stream()
                        .filter(affiliation -> authorAfid.id().equals(affiliation.afid()))
                        .findFirst();

                    if (authorAffiliation.isEmpty()) {
                        return;
                    }

                    var institution = new OrganisationUnit();
                    institution.setScopusAfid(authorAfid.id());
                    institution.getName()
                        .add(new MultilingualContent("EN", authorAffiliation.get().affilName(), 1));
                    contribution.getInstitutions().add(institution);
                });
            }

            document.getContributions().add(contribution);
        });
    }

    private static void setConferenceInfo(ScopusImportUtility.AbstractDataResponse abstractData,
                                          DocumentImport document) {
        if (Objects.nonNull(abstractData)) {
            var sourceRecord =
                abstractData.abstractRetrievalResponse().item().bibRecord().head()
                    .sourceRecord();

            var conference = new Event();
            conference.getName().add(new MultilingualContent("EN",
                sourceRecord.additionalSrcinfo()
                    .conferenceinfo().confevent().confname(), 1));

            conference.getState().add(new MultilingualContent("EN",
                sourceRecord.additionalSrcinfo()
                    .conferenceinfo().confevent().conflocation().country(), 1));

            conference.getPlace().add(new MultilingualContent("EN",
                sourceRecord.additionalSrcinfo()
                    .conferenceinfo().confevent().conflocation().city(), 1));

            var dateFromObject =
                sourceRecord.additionalSrcinfo().conferenceinfo().confevent().confdate()
                    .startdate();
            var dateToObject =
                sourceRecord.additionalSrcinfo().conferenceinfo().confevent().confdate()
                    .enddate();

            conference.setDateFrom(
                LocalDate.of(Integer.parseInt(dateFromObject.year()),
                    Integer.parseInt(dateFromObject.month()),
                    Integer.parseInt(dateFromObject.day())));

            if (Objects.nonNull(dateToObject)) {
                conference.setDateTo(
                    LocalDate.of(Integer.parseInt(dateToObject.year()),
                        Integer.parseInt(dateToObject.month()),
                        Integer.parseInt(dateToObject.day())));
            } else {
                conference.setDateTo(
                    LocalDate.of(Integer.parseInt(dateFromObject.year()),
                        Integer.parseInt(dateFromObject.month()),
                        Integer.parseInt(dateFromObject.day())));
            }

            document.setEvent(conference);
        }
    }
}
