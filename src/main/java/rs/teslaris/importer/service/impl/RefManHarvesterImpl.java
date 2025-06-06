package rs.teslaris.importer.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
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
import rs.teslaris.importer.service.interfaces.RefManHarvester;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefManHarvesterImpl implements RefManHarvester {

    private final MongoTemplate mongoTemplate;


    @Override
    public HashMap<Integer, Integer> harvestDocumentsForAuthor(Integer userId,
                                                               MultipartFile risFile,
                                                               HashMap<Integer, Integer> newEntriesCount) {
        DocumentImport document = null;
        ArrayList<String> affiliations = new ArrayList<>(), keywords = new ArrayList<>();
        try (var reader = new BufferedReader(
            new InputStreamReader(risFile.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while (Objects.nonNull((line = reader.readLine()))) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String tag = line.length() >= 6 ? line.substring(0, 2) : "";
                String content = line.length() >= 6 ? line.substring(6).trim() : "";

                switch (tag) {
                    case "TY":
                        document = new DocumentImport();
                        if (content.equalsIgnoreCase("JOUR")) {
                            document.setPublicationType(
                                DocumentPublicationType.JOURNAL_PUBLICATION);
                        } else if (content.equalsIgnoreCase("CONF")) {
                            document.setPublicationType(
                                DocumentPublicationType.PROCEEDINGS_PUBLICATION);
                        } else {
                            document = null;
                        }
                        break;
                    case "T1", "TI":
                        if (Objects.isNull(document)) {
                            break;
                        }

                        document.getTitle().add(new MultilingualContent("EN", content, 1));
                        break;
                    case "A1", "AU", "A2":
                        if (Objects.isNull(document)) {
                            break;
                        }

                        var authorship = new PersonDocumentContribution();
                        authorship.setContributionType(DocumentContributionType.AUTHOR);
                        String[] nameParts = content.split(",", 2);
                        if (nameParts.length == 2) {
                            var person = new Person();
                            person.setName(
                                new PersonName(nameParts[1].trim(), "", nameParts[0].trim()));
                            authorship.setPerson(person);
                        } else {
                            var person = new Person();
                            person.setName(new PersonName(content.trim(), "", ""));
                            authorship.setPerson(person);
                        }
                        document.getContributions().add(authorship);
                        break;
                    case "JO":
                        if (Objects.isNull(document)) {
                            break;
                        }

                        if (document.getPublicationType()
                            .equals(DocumentPublicationType.PROCEEDINGS_PUBLICATION)) {
                            handleSettingProceedingsAndEvent(content, document);
                        } else {
                            document.getPublishedIn()
                                .add(new MultilingualContent("EN", content, 1));
                        }

                        break;
                    case "PB":
                        if (Objects.isNull(document) || !document.getPublishedIn().isEmpty()) {
                            break;
                        }

                        handleSettingProceedingsAndEvent(content, document);
                    case "VL":
                        if (Objects.isNull(document)) {
                            break;
                        }

                        document.setVolume(content);
                        break;
                    case "IS":
                        if (Objects.isNull(document)) {
                            break;
                        }

                        document.setIssue(content);
                        break;
                    case "DO":
                        if (Objects.isNull(document)) {
                            break;
                        }

                        document.setDoi(content);
                        break;
                    case "UR":
                        if (Objects.isNull(document)) {
                            break;
                        }

                        document.getUris().add(content);
                        break;
                    case "SP":
                        if (Objects.isNull(document)) {
                            break;
                        }

                        document.setStartPage(content);
                        break;
                    case "EP":
                        if (Objects.isNull(document)) {
                            break;
                        }

                        document.setEndPage(content);
                        try {
                            int numPages = Integer.parseInt(content) -
                                Integer.parseInt(document.getStartPage());
                            document.setNumberOfPages(numPages);
                        } catch (Exception ignored) {
                        }
                        break;
                    case "SN":
                        if (Objects.isNull(document)) {
                            break;
                        }

                        if (content.contains(";")) {
                            for (var identifier : content.split("; ")) {
                                if (identifier.contains("(ISBN)")) {
                                    document.setIsbn(identifier.split(" ")[0]);
                                } else if (identifier.contains("(ISSN)")) {
                                    setISSN(identifier, document);
                                }
                            }
                        }
                        break;
                    case "Y1", "PY":
                        if (Objects.isNull(document)) {
                            break;
                        }

                        document.setDocumentDate(content);
                        break;
                    case "AD":
                        if (Objects.isNull(document)) {
                            break;
                        }

                        affiliations.add(content);
                        break;
                    case "KW":
                        if (Objects.isNull(document)) {
                            break;
                        }

                        keywords.add(content);
                        break;
                    case "AB":
                        if (Objects.isNull(document)) {
                            break;
                        }

                        document.getDescription().add(new MultilingualContent("EN", content, 1));
                        break;
                    case "ER", "":
                        if (Objects.nonNull(document)) {
                            document.getImportUsersId().add(userId);
                            mongoTemplate.save(document, "documentImports");
                            newEntriesCount.merge(userId, 1, Integer::sum);

                            if (affiliations.size() == 1) {
                                document.getContributions().forEach(contribution -> {
                                    var institution = new OrganisationUnit();
                                    institution.getName().add(
                                        new MultilingualContent("EN", affiliations.getFirst(), 1));
                                    contribution.getInstitutions().add(institution);
                                });
                            } else if (affiliations.size() == document.getContributions().size()) {
                                FunctionalUtil.forEachWithCounter(document.getContributions(),
                                    (i, contribution) -> {
                                        var institution = new OrganisationUnit();
                                        institution.getName()
                                            .add(new MultilingualContent("EN", affiliations.get(i),
                                                1));
                                        contribution.getInstitutions().add(institution);
                                    });
                            }
                            affiliations.clear();

                            document.getKeywords()
                                .add(new MultilingualContent("EN", String.join("\n", keywords), 1));
                            keywords.clear();

                            document = null;
                        }
                        break;
                    default:
                        log.info("Encountered unknown tag {} with value: {}.", tag, content);
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e); // should never happen
        }

        return newEntriesCount;
    }

    private void handleSettingProceedingsAndEvent(String content, DocumentImport document) {
        if (content.contains("Proceedings") || content.contains("proceedings")) {
            document.getPublishedIn().add(new MultilingualContent("EN", content, 1));
            var event = new Event();
            event.getName().add(
                new MultilingualContent("EN", BibTexConverter.cleanProceedingsTitleToEvent(content),
                    1));
            document.setEvent(event);
        } else {
            document.getPublishedIn()
                .add(new MultilingualContent("EN", "Proceedings of " + content, 1));
            var event = new Event();
            event.getName().add(new MultilingualContent("EN", content, 1));
            document.setEvent(event);
        }
    }

    private void setISSN(String content, DocumentImport document) {
        if (content.contains("-")) {
            document.setEIssn(content.split(" ")[0]);
        } else {
            document.setEIssn(content.substring(0, 4) + "-" + content.substring(4, 8));
        }
    }
}
