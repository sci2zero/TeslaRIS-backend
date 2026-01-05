package rs.teslaris.core.service.impl.document;

import jakarta.annotation.Nullable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.MetadataPrepopulationService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.session.RestTemplateProvider;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataPrepopulationServiceImpl implements MetadataPrepopulationService {

    private final RestTemplateProvider restTemplateProvider;

    private final PersonService personService;

    private final JournalService journalService;

    private final ConferenceService conferenceService;

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
        headers.set("Accept", "application/x-bibtex");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        var restTemplate = restTemplateProvider.provideRestTemplate();

        var response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        var body = response.getBody();

        return body;
    }

    private PrepopulatedMetadataDTO parseBibTexContent(String bibtexContent, Integer importPersonId)
        throws ParseException {
        var metadata = new PrepopulatedMetadataDTO();

        var parser = new BibTeXParser();
        var database = parser.parse(new StringReader(bibtexContent));

        database.getEntries().entrySet().stream().findFirst().ifPresent(entry -> {
            BibTeXEntry bibEntry = entry.getValue();
            LaTeXParser latexParser = createLatexParser();
            var printer = new LaTeXPrinter();

            var type = bibEntry.getType().getValue();
            metadata.setDocumentPublicationType(mapBibtexTypeToPublicationType(type));

            setTitle(metadata, bibEntry);
            setAuthors(metadata, bibEntry, latexParser, printer, importPersonId);
            setPublishedInAndEntity(metadata, bibEntry);
            setAdditionalFields(metadata, bibEntry);
        });

        return metadata;
    }

    private LaTeXParser createLatexParser() {
        try {
            return new LaTeXParser();
        } catch (ParseException e) {
            log.error("Error during BibTex LaTex parsing: {}", e.getMessage());
            return null;
        }
    }

    private void setTitle(PrepopulatedMetadataDTO metadata, BibTeXEntry bibEntry) {
        var titleValue = bibEntry.getField(BibTeXEntry.KEY_TITLE);
        if (Objects.nonNull(titleValue)) {
            var title = titleValue.toUserString();
            var english = languageTagService.findLanguageTagByValue("EN");
            metadata.getTitle().add(new MultilingualContentDTO(
                english.getId(), english.getLanguageTag(), title, 1));
        }
    }

    private void setKeywords(PrepopulatedMetadataDTO metadata, BibTeXEntry bibEntry) {
        var keywordsValue = bibEntry.getField(new Key("keywords"));
        if (Objects.nonNull(keywordsValue)) {
            var keywords = keywordsValue.toUserString();
            var english = languageTagService.findLanguageTagByValue("EN");
            metadata.getKeywords().add(
                new MultilingualContentDTO(
                    english.getId(),
                    english.getLanguageTag(),
                    keywords.replace(", ", "\n"),
                    1
                )
            );
        }
    }

    private void setAuthors(PrepopulatedMetadataDTO metadata, BibTeXEntry bibEntry,
                            LaTeXParser latexParser, LaTeXPrinter printer, Integer importPersonId) {
        var authorValue = bibEntry.getField(BibTeXEntry.KEY_AUTHOR);
        if (Objects.nonNull(authorValue) && Objects.nonNull(latexParser)) {
            try {
                var parsedAuthors = printer.print(latexParser.parse(authorValue.toUserString()));
                var authors = parsedAuthors.split(" and ");
                metadata.setContributions(
                    resolveContributionsFromAuthorNames(authors, importPersonId));
            } catch (ParseException e) {
                log.error("Error during BibTex author parsing: {}", e.getMessage());
            }
        }
    }

    private void setPublishedInAndEntity(PrepopulatedMetadataDTO metadata, BibTeXEntry bibEntry) {
        metadata.setPublishedInName(getStringField(bibEntry, BibTeXEntry.KEY_JOURNAL));

        if (metadata.getDocumentPublicationType()
            .equals(DocumentPublicationType.JOURNAL_PUBLICATION)) {
            handleJournalPublication(metadata, bibEntry);
        } else if (metadata.getDocumentPublicationType()
            .equals(DocumentPublicationType.PROCEEDINGS_PUBLICATION)) {
            handleProceedingsPublication(metadata, bibEntry);
        }
    }

    private void handleJournalPublication(PrepopulatedMetadataDTO metadata, BibTeXEntry bibEntry) {
        var issn = getStringField(bibEntry, new Key("ISSN"));
        if (!issn.isEmpty()) {
            var journal = journalService.readJournalByIssn(issn, issn);
            if (Objects.nonNull(journal)) {
                metadata.setPublishEntityId(journal.getDatabaseId());
            }
        }

        if (Objects.isNull(metadata.getPublishEntityId())) {
            journalService.searchJournals(List.of(metadata.getPublishedInName().split(" ")),
                    PageRequest.of(0, 3), null, null).getContent()
                .stream()
                .filter(journal ->
                    Arrays.stream(journal.getTitleSr().split("\\|"))
                        .anyMatch(titleVariant -> titleVariant.trim()
                            .equalsIgnoreCase(metadata.getPublishedInName().trim())) &&
                        Arrays.stream(journal.getTitleOther().split("\\|"))
                            .anyMatch(titleVariant -> titleVariant.trim()
                                .equalsIgnoreCase(metadata.getPublishedInName().trim())))
                .findFirst()
                .ifPresent(foundMatch -> {
                    metadata.setPublishEntityId(foundMatch.getDatabaseId());
                    metadata.setPublishedInName(foundMatch.getTitleOther());
                });
        }
    }

    private void handleProceedingsPublication(PrepopulatedMetadataDTO metadata,
                                              BibTeXEntry bibEntry) {
        if (metadata.getPublishedInName().isEmpty()) {
            metadata.setPublishedInName(getStringField(bibEntry, BibTeXEntry.KEY_BOOKTITLE));
        }

        if (metadata.getPublishedInName().toLowerCase().contains("proceedings")) {
            metadata.setPublishedInName(
                metadata.getPublishedInName()
                    .replace("Proceedings of the", "")
                    .replace("Proceedings of", "")
                    .replace("Proceedings", "")
                    .replace("proceedings", "")
                    .trim());
        }

        conferenceService.searchConferences(
                List.of(metadata.getPublishedInName().split(" ")),
                PageRequest.of(0, 3),
                false, false,
                null, null, null).getContent()
            .stream()
            .filter(conference ->
                conference.getNameSr().trim()
                    .equalsIgnoreCase(metadata.getPublishedInName().trim()) ||
                    conference.getNameOther().trim()
                        .equalsIgnoreCase(metadata.getPublishedInName().trim()))
            .findFirst()
            .ifPresent(foundMatch -> {
                metadata.setPublishEntityId(foundMatch.getDatabaseId());
                metadata.setPublishedInName(foundMatch.getNameOther());
            });
    }

    private void setAdditionalFields(PrepopulatedMetadataDTO metadata, BibTeXEntry bibEntry) {
        metadata.setVolume(getStringField(bibEntry, BibTeXEntry.KEY_VOLUME));
        metadata.setIssue(getStringField(bibEntry, BibTeXEntry.KEY_NUMBER));
        metadata.setUrl(getStringField(bibEntry, BibTeXEntry.KEY_URL));
        metadata.setYear(getIntField(bibEntry, BibTeXEntry.KEY_YEAR, -1));
        metadata.setDoi(getStringField(bibEntry, BibTeXEntry.KEY_DOI));

        setPageInfo(metadata, bibEntry.getField(new Key("pages")));
        setKeywords(metadata, bibEntry);
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
                    PageRequest.of(0, 5), false, null, false).getContent();

            if (Objects.nonNull(inputPersonId) && !selfBindCompleted && personResults.stream()
                .anyMatch(index -> index.getDatabaseId().equals(inputPersonId))) {
                var importingAuthor = personResults.stream()
                    .filter(index -> index.getDatabaseId().equals(inputPersonId)).findFirst().get();
                contribution.setPersonId(importingAuthor.getDatabaseId());
                contribution.setInstitutionIds(
                    importingAuthor.getEmploymentInstitutionsId().isEmpty() ?
                        Collections.emptyList() :
                        List.of(importingAuthor.getEmploymentInstitutionsId().getFirst()));
                selfBindCompleted = true;
            } else if (!personResults.isEmpty()) {
                var foundAuthor = personResults.getFirst();
                contribution.setPersonId(foundAuthor.getDatabaseId());
                contribution.setInstitutionIds(
                    foundAuthor.getEmploymentInstitutionsId().isEmpty() ? Collections.emptyList() :
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

    @Nullable
    private DocumentPublicationType mapBibtexTypeToPublicationType(String bibtexType) {
        if (Objects.isNull(bibtexType)) {
            return null;
        }

        return switch (bibtexType.toLowerCase()) {
            case "article" -> DocumentPublicationType.JOURNAL_PUBLICATION;
            case "book", "booklet" -> DocumentPublicationType.MONOGRAPH;
            case "conference", "inproceedings", "inbook" ->
                DocumentPublicationType.PROCEEDINGS_PUBLICATION;
            case "incollection" -> DocumentPublicationType.MONOGRAPH_PUBLICATION;
            case "manual" -> DocumentPublicationType.SOFTWARE; // Closest guess; could be custom
            case "masterthesis", "phdthesis" -> DocumentPublicationType.THESIS;
            case "proceedings" -> DocumentPublicationType.PROCEEDINGS;
            case "techreport" -> DocumentPublicationType.MATERIAL_PRODUCT;
            case "misc" ->
                DocumentPublicationType.MATERIAL_PRODUCT; // catch-all, material product for now
            case "unpublished" -> DocumentPublicationType.JOURNAL_PUBLICATION; // assumption
            default -> null; // or throw an exception if needed, should never happen
        };
    }
}
