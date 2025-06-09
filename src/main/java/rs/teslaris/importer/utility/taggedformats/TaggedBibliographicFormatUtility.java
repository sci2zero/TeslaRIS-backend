package rs.teslaris.importer.utility.taggedformats;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
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
import rs.teslaris.importer.model.converter.harvest.BibTexConverter;

@Component
public class TaggedBibliographicFormatUtility {

    private static MongoTemplate mongoTemplate;


    @Autowired
    public TaggedBibliographicFormatUtility(MongoTemplate mongoTemplate) {
        TaggedBibliographicFormatUtility.mongoTemplate = mongoTemplate;
    }

    public static void handleEndPage(DocumentImport document, String content) {
        document.setEndPage(content);
        try {
            int pages = Integer.parseInt(content) - Integer.parseInt(document.getStartPage());
            document.setNumberOfPages(pages);
        } catch (Exception ignored) {
        }
    }

    public static void parseIssnIsbn(DocumentImport document, String content) {
        for (var identifier : content.split("; ")) {
            if (identifier.contains("(ISBN)")) {
                document.setIsbn(identifier.split(" ")[0]);
            } else if (identifier.contains("(ISSN)")) {
                setISSN(identifier, document);
            }
        }
    }

    public static void handleDate(String content, DocumentImport document) {
        document.setDocumentDate(content);
        try {
            int year = Integer.parseInt(content);
            document.getEvent().setDateFrom(LocalDate.of(year, 1, 1));
            document.getEvent().setDateTo(LocalDate.of(year, 12, 31));
        } catch (Exception ignored) {
        }
    }

    public static void finalizeAndSaveDocument(DocumentImport doc, Integer userId,
                                               HashMap<Integer, Integer> count,
                                               List<String> affiliations, List<String> keywords) {
        doc.getImportUsersId().add(userId);
        mongoTemplate.save(doc, "documentImports");
        count.merge(userId, 1, Integer::sum);

        if (affiliations.size() == 1) {
            FunctionalUtil.forEachWithCounter(doc.getContributions(), (i, c) -> {
                var inst = new OrganisationUnit();
                inst.setImportId(String.valueOf(i + 1));
                inst.getName().add(new MultilingualContent("EN", affiliations.getFirst(), 1));
                c.getInstitutions().add(inst);
            });
        } else if (affiliations.size() == doc.getContributions().size()) {
            FunctionalUtil.forEachWithCounter(doc.getContributions(), (i, c) -> {
                var inst = new OrganisationUnit();
                inst.setImportId(String.valueOf(i + 1));
                inst.getName().add(new MultilingualContent("EN", affiliations.get(i), 1));
                c.getInstitutions().add(inst);
            });
        }
        doc.getKeywords().add(new MultilingualContent("EN", String.join("\n", keywords), 1));
        affiliations.clear();
        keywords.clear();
    }

    public static void handleSettingProceedingsAndEvent(String content, DocumentImport document) {
        var event = new Event();
        if (content.contains(";")) {
            Arrays.stream(content.split("; ")).filter(part -> part.startsWith("Conference name:"))
                .findFirst().ifPresent(
                    s -> {
                        var eventName = s.split(": ")[1];
                        event.getName().add(new MultilingualContent("EN", eventName, 1));
                        document.getPublishedIn()
                            .add(new MultilingualContent("EN", "Proceedings of " + eventName, 1));
                    });
        } else if (content.contains("Proceedings") || content.contains("proceedings")) {
            document.getPublishedIn().add(new MultilingualContent("EN", content, 1));
            event.getName().add(
                new MultilingualContent("EN", BibTexConverter.cleanProceedingsTitleToEvent(content),
                    1));
        } else {
            document.getPublishedIn()
                .add(new MultilingualContent("EN", "Proceedings of " + content, 1));
            event.getName().add(new MultilingualContent("EN", content, 1));
        }

        document.setEvent(event);
    }

    public static void setISSN(String content, DocumentImport document) {
        if (content.contains("-")) {
            document.setEIssn(content.split(" ")[0]);
        } else {
            document.setEIssn(content.substring(0, 4) + "-" + content.substring(4, 8));
        }
    }

    public static DocumentImport parseDocumentType(String content) {
        var doc = new DocumentImport();
        doc.setStartPage("");
        doc.setEndPage("");
        if ("JOUR".equalsIgnoreCase(content)) {
            doc.setPublicationType(DocumentPublicationType.JOURNAL_PUBLICATION);
        } else if ("CONF".equalsIgnoreCase(content)) {
            doc.setPublicationType(DocumentPublicationType.PROCEEDINGS_PUBLICATION);
        } else {
            return null;
        }
        return doc;
    }

    public static void addAuthor(DocumentImport document, String content) {
        var authorship = new PersonDocumentContribution();
        authorship.setOrderNumber(document.getContributions().size() + 1);
        authorship.setContributionType(DocumentContributionType.AUTHOR);

        String[] nameParts = content.split(",", 2);
        var person = new Person();
        person.setImportId(String.valueOf(authorship.getOrderNumber()));
        person.setName(nameParts.length == 2
            ? new PersonName(nameParts[1].trim(), "", nameParts[0].trim())
            : new PersonName(content.trim(), "", ""));
        authorship.setPerson(person);
        document.getContributions().add(authorship);
    }
}
