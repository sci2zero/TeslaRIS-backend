package rs.teslaris.core.service.impl.document;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXParser;
import org.jbibtex.Key;
import org.jbibtex.LaTeXParser;
import org.jbibtex.LaTeXPrinter;
import org.jbibtex.ParseException;
import org.jbibtex.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.document.PrepopulatedMetadataDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.MetadataPrepopulationService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.RestTemplateProvider;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataPrepopulationServiceImpl implements MetadataPrepopulationService {

    private final RestTemplateProvider restTemplateProvider;

    private final PersonService personService;

    private final JournalService journalService;

    private final LanguageTagService languageTagService;


    @Override
    public PrepopulatedMetadataDTO fetchBibTexDataForDoi(String doi, Integer importPersonId) {
        String bibtexContent = fetchBibTexFromDoi(doi);
        if (Objects.isNull(bibtexContent)) {
            return new PrepopulatedMetadataDTO();
        }

        try {
            return parseBibTexContent(bibtexContent, importPersonId);
        } catch (Exception e) {
            log.error("An error occurred while parsing BibTeX response: {}", e.getMessage());
            return new PrepopulatedMetadataDTO();
        }
    }

    private String fetchBibTexFromDoi(String doi) {
        var url = "https://doi.org/" + doi;

        var headers = new HttpHeaders();
        headers.set("Accept", "text/bibliography; style=bibtex");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        var restTemplate = restTemplateProvider.provideRestTemplate();

        var response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        var body = response.getBody();

        if (Objects.nonNull(body)) {
            System.out.println("Fetched BibTeX:\n" + body);
        }

        return body;
    }

    private PrepopulatedMetadataDTO parseBibTexContent(String bibtexContent, Integer importPersonId)
        throws ParseException {
        var metadata = new PrepopulatedMetadataDTO();

        var parser = new BibTeXParser();
        var database = parser.parse(new StringReader(bibtexContent));

        database.getEntries().entrySet().stream().findFirst().ifPresent(entry -> {
            BibTeXEntry bibEntry = entry.getValue();
            LaTeXParser latexParser = null;
            try {
                latexParser = new LaTeXParser();
            } catch (ParseException e) {
                log.error("Error during BibTex LaTex parsing: {}", e.getMessage());
            }
            var printer = new LaTeXPrinter();

            var type = bibEntry.getType().getValue();
            metadata.setDocumentPublicationType(mapBibtexTypeToPublicationType(type));

            // Title
            var titleValue = bibEntry.getField(BibTeXEntry.KEY_TITLE);
            if (Objects.nonNull(titleValue)) {
                String title = titleValue.toUserString();
                var english = languageTagService.findLanguageTagByValue("EN");
                metadata.getTitle().add(new MultilingualContentDTO(
                    english.getId(), english.getLanguageTag(), title, 1));
            }

            // Authors
            var authorValue = bibEntry.getField(BibTeXEntry.KEY_AUTHOR);
            if (Objects.nonNull(authorValue) && Objects.nonNull(latexParser)) {
                String parsedAuthors;
                try {
                    parsedAuthors = printer.print(latexParser.parse(authorValue.toUserString()));
                    var authors = parsedAuthors.split(" and ");
                    metadata.setContributions(
                        resolveContributionsFromAuthorNames(authors, importPersonId));
                } catch (ParseException e) {
                    log.error("Error during BibTex author parsing: {}", e.getMessage());
                }
            }

            // Published In
            metadata.setPublishedInName(getStringField(bibEntry, BibTeXEntry.KEY_JOURNAL));
            if (metadata.getDocumentPublicationType() ==
                DocumentPublicationType.JOURNAL_PUBLICATION) {
                var issn = getStringField(bibEntry, new Key("ISSN"));
                if (!issn.isEmpty()) {
                    var journal = journalService.readJournalByIssn(issn, issn);
                    if (Objects.nonNull(journal)) {
                        metadata.setPublishEntityId(journal.getDatabaseId());
                    }
                }
            } else if (metadata.getDocumentPublicationType() ==
                DocumentPublicationType.PROCEEDINGS_PUBLICATION
                && metadata.getPublishedInName().isEmpty()) {
                metadata.setPublishedInName(getStringField(bibEntry, BibTeXEntry.KEY_BOOKTITLE));
            }

            // Volume, Issue, URL, Year
            metadata.setVolume(getStringField(bibEntry, BibTeXEntry.KEY_VOLUME));
            metadata.setIssue(getStringField(bibEntry, BibTeXEntry.KEY_NUMBER));
            metadata.setUrl(getStringField(bibEntry, BibTeXEntry.KEY_URL));
            metadata.setYear(
                getIntField(bibEntry, BibTeXEntry.KEY_YEAR, LocalDate.now().getYear()));

            // Pages
            setPageInfo(metadata, bibEntry.getField(new Key("pages")));
        });

        return metadata;
    }

    private String getStringField(BibTeXEntry entry, Key key) {
        var value = entry.getField(key);
        return Objects.nonNull(value) ? value.toUserString() : "";
    }

    private int getIntField(BibTeXEntry entry, Key key, int defaultValue) {
        try {
            String value = getStringField(entry, key);
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void setPageInfo(PrepopulatedMetadataDTO metadata, Value pageField) {
        if (Objects.isNull(pageField)) {
            return;
        }

        var pages = pageField.toUserString().replace("–", "-");
        var tokens = pages.split("-");
        metadata.setStartPage(tokens[0]);

        if (tokens.length > 1) {
            metadata.setEndPage(tokens[1]);
        }
    }

    private List<PersonDocumentContributionDTO> resolveContributionsFromAuthorNames(
        String[] authors, Integer inputPersonId) {
        var contributions = new ArrayList<PersonDocumentContributionDTO>();
        var selfBindCompleted = false;

        var authorshipOrderNumber = 1;
        for (var authorName : authors) {
            var contribution = new PersonDocumentContributionDTO();
            contribution.setContributionType(DocumentContributionType.AUTHOR);
            contribution.setOrderNumber(authorshipOrderNumber);

            var authorNameTokens = List.of(authorName.replace(",", "").split(" "));
            contribution.setPersonName(inferAuthorName(authorNameTokens));

            var personResults =
                personService.findPeopleByNameAndEmployment(authorNameTokens,
                    PageRequest.of(0, 5), false, null).getContent();

            if (Objects.nonNull(inputPersonId) && !selfBindCompleted && personResults.stream()
                .anyMatch(index -> index.getDatabaseId().equals(inputPersonId))) {
                var importingAuthor = personResults.stream()
                    .filter(index -> index.getDatabaseId().equals(inputPersonId)).findFirst().get();
                contribution.setPersonId(importingAuthor.getDatabaseId());
                contribution.setInstitutionIds(
                    importingAuthor.getEmploymentInstitutionsId().isEmpty() ? List.of() :
                        List.of(importingAuthor.getEmploymentInstitutionsId().getFirst()));
                selfBindCompleted = true;
            } else if (!personResults.isEmpty()) {
                var foundAuthor = personResults.getFirst();
                contribution.setPersonId(foundAuthor.getDatabaseId());
                contribution.setInstitutionIds(
                    foundAuthor.getEmploymentInstitutionsId().isEmpty() ? List.of() :
                        List.of(foundAuthor.getEmploymentInstitutionsId().getFirst()));
            }

            contributions.add(contribution);
            authorshipOrderNumber++;
        }

        return contributions;
    }

    private PersonNameDTO inferAuthorName(List<String> authorNameTokens) {
        return new PersonNameDTO(null, authorNameTokens.getLast(), "", authorNameTokens.getFirst(),
            null, null);
    }

    private DocumentPublicationType mapBibtexTypeToPublicationType(String bibtexType) {
        if (Objects.isNull(bibtexType)) {
            return null;
        }

        return switch (bibtexType.toLowerCase()) {
            case "article" -> DocumentPublicationType.JOURNAL_PUBLICATION;
            case "book", "booklet" -> DocumentPublicationType.MONOGRAPH;
            case "conference", "inproceedings" -> DocumentPublicationType.PROCEEDINGS_PUBLICATION;
            case "inbook", "incollection" -> DocumentPublicationType.MONOGRAPH_PUBLICATION;
            case "manual" -> DocumentPublicationType.SOFTWARE; // Closest guess; could be custom
            case "masterthesis", "phdthesis" -> DocumentPublicationType.THESIS;
            case "proceedings" -> DocumentPublicationType.PROCEEDINGS;
            case "techreport" -> DocumentPublicationType.DATASET; // or SOFTWARE if more accurate
            case "misc" -> DocumentPublicationType.SOFTWARE; // catch-all; choose based on context
            case "unpublished" -> DocumentPublicationType.JOURNAL_PUBLICATION; // assumption
            default -> null; // or throw an exception if needed
        };
    }
}
