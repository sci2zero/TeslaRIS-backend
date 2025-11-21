package rs.teslaris.importer.model.converter.harvest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.LaTeXParser;
import org.jbibtex.LaTeXPrinter;
import org.jbibtex.ParseException;
import org.jbibtex.Value;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.JournalPublicationType;
import rs.teslaris.core.model.document.ProceedingsPublicationType;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.Event;
import rs.teslaris.importer.model.common.MultilingualContent;
import rs.teslaris.importer.model.common.OrganisationUnit;
import rs.teslaris.importer.model.common.Person;
import rs.teslaris.importer.model.common.PersonDocumentContribution;
import rs.teslaris.importer.model.common.PersonName;

public class BibTexConverter {

    public static Optional<DocumentImport> toCommonImportModel(BibTeXEntry bibEntry) {
        String entryType = bibEntry.getType().getValue();
        String citationKey = bibEntry.getKey().getValue();

        boolean isArticle = "article".equalsIgnoreCase(entryType);
        boolean isProceedings =
            "inproceedings".equalsIgnoreCase(entryType) ||
                "conference".equalsIgnoreCase(entryType);
        if (!isArticle && !isProceedings) {
            return Optional.empty();
        }

        var document = new DocumentImport();
        document.setSource("BIBTEX");
        // is this ok?
        document.setIdentifier(citationKey);
        document.setPublicationType(
            isArticle ? DocumentPublicationType.JOURNAL_PUBLICATION :
                DocumentPublicationType.PROCEEDINGS_PUBLICATION
        );

        if (isArticle) {
            document.setJournalPublicationType(JournalPublicationType.RESEARCH_ARTICLE);
        } else {
            document.setProceedingsPublicationType(ProceedingsPublicationType.REGULAR_FULL_ARTICLE);
        }

        try {
            parseAuthors(bibEntry, document);
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing authors", e);
        }

        getFieldValue(bibEntry, BibTeXEntry.KEY_TITLE)
            .ifPresent(title -> document.getTitle().add(new MultilingualContent("EN", title, 1)));

        if (!addPublishedIn(bibEntry, document, isArticle)) {
            return Optional.empty();
        }

        document.setVolume(getFieldValue(bibEntry, BibTeXEntry.KEY_VOLUME).orElse(""));
        document.setArticleNumber(getFieldValue(bibEntry, BibTeXEntry.KEY_NUMBER).orElse(""));
        setPageInfo(bibEntry, document);
        document.setDocumentDate(getFieldValue(bibEntry, BibTeXEntry.KEY_YEAR).orElse(""));

        if (isProceedings) {
            Optional<String> bookTitle = getFieldValue(bibEntry, BibTeXEntry.KEY_BOOKTITLE);
            if (bookTitle.isPresent()) {
                var event = new Event();

                if (bookTitle.get().contains(";")) {
                    var eventAndProceedings = bookTitle.get().split("; ");
                    event.getName().add(
                        new MultilingualContent("EN", sanitizeBibTexString(eventAndProceedings[0]),
                            1));
                    document.getPublishedIn()
                        .add(new MultilingualContent("EN",
                            sanitizeBibTexString(eventAndProceedings[1]), 1));
                } else if (bookTitle.get().contains("Proceedings") ||
                    bookTitle.get().contains("proceedings")) {
                    document.getPublishedIn()
                        .add(new MultilingualContent("EN", sanitizeBibTexString(bookTitle.get()),
                            1));
                    String eventName = cleanProceedingsTitleToEvent(bookTitle.get());
                    event.getName()
                        .add(new MultilingualContent("EN", sanitizeBibTexString(eventName), 1));
                } else {
                    event.getName().add(
                        new MultilingualContent("EN", sanitizeBibTexString(bookTitle.get()), 1));
                    document.getPublishedIn()
                        .add(new MultilingualContent("EN",
                            "Proceedings of " + sanitizeBibTexString(bookTitle.get()), 1));
                }

                document.setEvent(event);
            } else {
                Optional<String> note = getFieldValue(bibEntry, "note");
                if (note.isEmpty()) {
                    return Optional.empty();
                }

                var conferenceName = Arrays.stream(note.get().split("; "))
                    .filter(section -> section.startsWith("Conference name")).findFirst();
                if (conferenceName.isEmpty()) {
                    return Optional.empty();
                }
                var cleanName = conferenceName.get().split(": ")[1];

                document.getPublishedIn()
                    .add(
                        new MultilingualContent("EN", "Proceedings of " + cleanName, 1));
                var event = new Event();
                event.getName().add(new MultilingualContent("EN", cleanName, 1));
                document.setEvent(event);
            }

            document.getEvent().setDateFrom(
                LocalDate.of(Integer.parseInt(document.getDocumentDate()),
                    1,
                    1));
            document.getEvent().setDateTo(
                LocalDate.of(Integer.parseInt(document.getDocumentDate()),
                    12,
                    31));
        }

        getFieldValue(bibEntry, "source").ifPresent(source -> {
            if (source.equals("Scopus")) {
                loadAdditionalData(document, bibEntry);
            }
        });

        if (document.getPublicationType().equals(DocumentPublicationType.JOURNAL_PUBLICATION) &&
            (Objects.isNull(document.getPublishedIn()) || document.getPublishedIn().isEmpty())) {
            return Optional.empty();
        }

        if (document.getPublicationType().equals(DocumentPublicationType.PROCEEDINGS_PUBLICATION) &&
            (Objects.isNull(document.getEvent()) || document.getEvent().getName().isEmpty())) {
            return Optional.empty();
        }

        return Optional.of(document);
    }

    private static Optional<String> getFieldValue(BibTeXEntry entry, Key key) {
        return Optional.ofNullable(entry.getField(key)).map(Value::toUserString);
    }

    private static Optional<String> getFieldValue(BibTeXEntry entry, String key) {
        return Optional.ofNullable(entry.getField(new Key(key))).map(Value::toUserString);
    }

    private static boolean addPublishedIn(BibTeXEntry bibEntry, DocumentImport doc,
                                          boolean isArticle) {
        if (getFieldValue(bibEntry, BibTeXEntry.KEY_JOURNAL)
            .map(journal -> {
                doc.getPublishedIn()
                    .add(new MultilingualContent("EN", sanitizeBibTexString(journal), 1));
                return true;
            }).orElse(false)) {
            return true;
        }

        if (isArticle) {
            return getFieldValue(bibEntry, BibTeXEntry.KEY_PUBLISHER)
                .or(() -> getFieldValue(bibEntry, BibTeXEntry.KEY_ORGANIZATION))
                .map(pubOrg -> {
                    doc.getPublishedIn()
                        .add(new MultilingualContent("EN", sanitizeBibTexString(pubOrg), 1));
                    return true;
                }).orElse(false);
        }

        return true;
    }

    private static void parseAuthors(BibTeXEntry bibEntry, DocumentImport doc)
        throws ParseException {
        var authorValue = bibEntry.getField(BibTeXEntry.KEY_AUTHOR);
        if (Objects.isNull(authorValue)) {
            return;
        }

        var parser = new LaTeXParser();
        var printer = new LaTeXPrinter();

        var contributions = new ArrayList<PersonDocumentContribution>();
        var authors = printer.print(parser.parse(authorValue.toUserString())).split(" and ");

        var orderNumber = 1;
        for (var authorName : authors) {
            if (authorName.equals("others")) {
                continue;
            }

            var tokens = authorName.split(", ");
            if (tokens.length == 2) {
                var contribution = new PersonDocumentContribution();
                contribution.setOrderNumber(orderNumber);
                var person = new Person();
                person.setImportId(String.valueOf(orderNumber));
                person.setName(new PersonName(tokens[1], "", tokens[0]));
                contribution.setPerson(person);
                contribution.setContributionType(DocumentContributionType.AUTHOR);

                contributions.add(contribution);
            }

            orderNumber++;
        }

        doc.getContributions().addAll(contributions);
    }

    private static void setPageInfo(BibTeXEntry bibEntry, DocumentImport doc) {
        doc.setStartPage("");
        doc.setEndPage("");
        getFieldValue(bibEntry, BibTeXEntry.KEY_PAGES).ifPresent(pages -> {
            if (pages.contains("--")) {
                var tokens = pages.split("--");
                if (tokens.length == 2) {
                    doc.setStartPage(tokens[0]);
                    doc.setEndPage(tokens[1]);
                    try {
                        int start = Integer.parseInt(tokens[0]);
                        int end = Integer.parseInt(tokens[1]);
                        doc.setNumberOfPages(end - start);
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else {
                doc.setStartPage(pages);
            }
        });
    }

    public static String cleanProceedingsTitleToEvent(String bookTitle) {
        if (bookTitle.toLowerCase().startsWith("proceedings of the ")) {
            return bookTitle.substring("proceedings of the ".length()).trim();
        } else if (bookTitle.toLowerCase().startsWith("proceedings ")) {
            return bookTitle.substring("proceedings ".length()).trim();
        }
        return bookTitle.trim();
    }

    private static void loadAdditionalData(DocumentImport document, BibTeXEntry bibEntry) {
        document.setDoi(getFieldValue(bibEntry, "doi").orElse(""));

        getFieldValue(bibEntry, "issn").ifPresent(issn -> {
            if (issn.contains("-")) {
                document.setEIssn(issn);
            } else {
                document.setEIssn(issn.substring(0, 4) + "-" + issn.substring(4));
            }
        });

        getFieldValue(bibEntry, "author_keywords").ifPresent(keywords -> document.getKeywords()
            .add(new MultilingualContent("EN", keywords.replace("; ", "\n"), 1)));

        getFieldValue(bibEntry, "abstract").ifPresent(description -> document.getDescription()
            .add(new MultilingualContent("EN", description, 1)));

        getFieldValue(bibEntry, "uri").ifPresent(uri -> document.getUris().add(uri));

        getFieldValue(bibEntry, "affiliations").ifPresent(affiliationsStr -> {
            var affiliations = affiliationsStr.split("; ");
            if (affiliations.length == 1) {
                FunctionalUtil.forEachWithCounter(document.getContributions(),
                    (i, contribution) -> {
                        var institution = new OrganisationUnit();
                        institution.setImportId(String.valueOf(i + 1));
                        institution.getName()
                            .add(new MultilingualContent("EN", affiliations[0], 1));
                        contribution.getInstitutions().add(institution);
                    });
            } else if (affiliations.length == document.getContributions().size()) {
                FunctionalUtil.forEachWithCounter(document.getContributions(),
                    (i, contribution) -> {
                        var institution = new OrganisationUnit();
                        institution.setImportId(String.valueOf(i + 1));
                        institution.getName()
                            .add(new MultilingualContent("EN", affiliations[i], 1));
                        contribution.getInstitutions().add(institution);
                    });
            }
        });
    }

    private static String sanitizeBibTexString(String value) {
        if (value == null) {
            return null;
        }
        return value
            .replace("\\&", "&")
            .replace("\\%", "%")
            .replace("\\_", "_")
            .replace("\\$", "$")
            .replaceAll("\\{([^}]*)}", "$1");
    }
}
