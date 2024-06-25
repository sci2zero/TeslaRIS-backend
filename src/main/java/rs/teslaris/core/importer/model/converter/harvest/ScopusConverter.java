package rs.teslaris.core.importer.model.converter.harvest;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import rs.teslaris.core.importer.model.common.DocumentImport;
import rs.teslaris.core.importer.model.common.MultilingualContent;
import rs.teslaris.core.importer.model.common.OrganisationUnit;
import rs.teslaris.core.importer.model.common.Person;
import rs.teslaris.core.importer.model.common.PersonDocumentContribution;
import rs.teslaris.core.importer.model.common.PersonName;
import rs.teslaris.core.importer.utility.scopus.ScopusImportUtility;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.DocumentContributionType;

public class ScopusConverter {

    public static DocumentImport toCommonImportModel(ScopusImportUtility.Entry entry) {
        var document = new DocumentImport();

        if (entry.subtypeDescription().equals("Article")) {
            document.setPublicationType(DocumentPublicationType.JOURNAL_PUBLICATION);
        } else if (entry.subtypeDescription().equals("Conference Paper")) {
            document.setPublicationType(DocumentPublicationType.PROCEEDINGS_PUBLICATION);
        }

        setCommonFields(entry, document);

        document.setScopusId(entry.identifier());
        document.setEIssn(entry.eIssn());
        document.setPrintIssn(entry.issn());
        document.setDoi(entry.doi());

        if (Objects.nonNull(entry.pageRange())) {
            var pages = entry.pageRange().split("-");
            document.setStartPage(pages[0]);
            if (pages.length > 1) {
                document.setEndPage(pages[1]);
                document.setNumberOfPages(Integer.parseInt(pages[1]) - Integer.parseInt(pages[0]));
            }
        }

        return document;
    }

    protected static void setCommonFields(ScopusImportUtility.Entry entry,
                                          DocumentImport document) {
        entry.links().forEach(link -> {
            document.getUris().add(link.href());
        });
        document.getUris().add(entry.url());

        document.setDocumentDate(entry.coverDate());

        document.getTitle().add(new MultilingualContent("EN", entry.title(), 1));
        document.getDescription().add(new MultilingualContent("EN", entry.description(), 1));

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
}
