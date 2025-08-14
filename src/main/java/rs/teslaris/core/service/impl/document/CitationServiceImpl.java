package rs.teslaris.core.service.impl.document;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLName;
import de.undercouch.citeproc.csl.CSLNameBuilder;
import de.undercouch.citeproc.csl.CSLType;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.CitationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.tracing.SessionTrackingUtil;

@Component
@RequiredArgsConstructor
@Transactional
@Traceable
public class CitationServiceImpl implements CitationService {

    public final JournalPublicationRepository journalPublicationRepository;

    public final ProceedingsPublicationRepository proceedingsPublicationRepository;

    public final DocumentPublicationIndexRepository documentPublicationIndexRepository;


    @Override
    public CitationResponseDTO craftCitations(DocumentPublicationIndex index, String languageCode) {
        var itemBuilder = new CSLItemDataBuilder()
            .id("citationId")
            .type(deduceCSLType(index.getType()))
            .title(index.getTitleSr());

        if (index.getYear() > 0) {
            itemBuilder
                .issued(index.getYear());
        }

        addAuthors(itemBuilder, index.getAuthorNames());
        populatePublicationDetails(itemBuilder, index, languageCode);

        return generateCitations(itemBuilder.build());
    }

    private void addAuthors(CSLItemDataBuilder itemBuilder, String authorNames) {
        var authors = Arrays.stream(authorNames.split("; "))
            .map(authorName -> new CSLNameBuilder().given(authorName).build())
            .toArray(CSLName[]::new);

        itemBuilder.author(authors);
    }

    private void populatePublicationDetails(CSLItemDataBuilder itemBuilder,
                                            DocumentPublicationIndex index,
                                            String languageCode) {
        switch (index.getType()) {
            case "JOURNAL_PUBLICATION" -> {
                var journalPublication =
                    journalPublicationRepository.findById(index.getDatabaseId())
                        .orElseThrow(() -> new NotFoundException(
                            "Journal publication ID " + index.getDatabaseId() + " does not exist"));
                itemBuilder
                    .containerTitle(
                        getContent(journalPublication.getJournal().getTitle(), languageCode))
                    .volume(journalPublication.getVolume())
                    .issue(journalPublication.getIssue())
                    .page(journalPublication.getStartPage(), journalPublication.getEndPage());
            }
            case "PROCEEDINGS_PUBLICATION" -> {
                var proceedingsPublication =
                    proceedingsPublicationRepository.findById(index.getDatabaseId())
                        .orElseThrow(() -> new NotFoundException(
                            "Proceedings publication ID " + index.getDatabaseId() +
                                " does not exist"));
                itemBuilder
                    .containerTitle(
                        getContent(proceedingsPublication.getEvent().getName(), languageCode))
                    .number(proceedingsPublication.getArticleNumber())
                    .page(proceedingsPublication.getStartPage(),
                        proceedingsPublication.getEndPage());
            }
        }
    }

    private CitationResponseDTO generateCitations(CSLItemData item) {
        var response = new CitationResponseDTO();
        try {
            response.setApa(extractCitationText("apa", item));
            response.setMla(extractCitationText("modern-language-association", item));
            response.setChicago(extractCitationText("chicago-author-date", item));
            response.setHarvard(extractCitationText("harvard-cite-them-right", item));
            response.setVancouver(extractCitationText("vancouver", item));
        } catch (IOException e) {
            throw new RuntimeException(e); // Should never trigger.
        }
        return response;
    }

    private String extractCitationText(String style, CSLItemData item) throws IOException {
        return Jsoup.parse(CSL.makeAdhocBibliography(style, item).makeString())
            .selectFirst(".csl-entry").text();
    }

    private String getContent(Set<MultiLingualContent> contentList, String languageCode) {
        var localisedContent = contentList.stream()
            .filter(mc -> mc.getLanguage().getLanguageTag().equalsIgnoreCase(languageCode))
            .findFirst();
        if (localisedContent.isPresent()) {
            return localisedContent.get().getContent();
        }

        return contentList.stream()
            .findFirst()
            .map(MultiLingualContent::getContent)
            .orElseThrow(() -> new NotFoundException("Missing container title"));
    }

    @Override
    public CitationResponseDTO craftCitations(Integer documentId, String languageCode) {
        var documentIndex =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId)
                .orElseThrow(() -> new NotFoundException(
                    "Document with id " + documentId + " does not exist."));

        if (!SessionTrackingUtil.isUserLoggedIn() && !documentIndex.getIsApproved()) {
            throw new NotFoundException("Document with id " + documentId + " does not exist.");
        }

        return craftCitations(documentIndex, languageCode);
    }

    private CSLType deduceCSLType(String type) {
        return switch (type) {
            case "JOURNAL_PUBLICATION" -> CSLType.ARTICLE_JOURNAL;
            case "PROCEEDINGS_PUBLICATION" -> CSLType.PAPER_CONFERENCE;
            case "THESIS" -> CSLType.THESIS;
            case "SOFTWARE" -> CSLType.SOFTWARE;
            case "PATENT" -> CSLType.PATENT;
            case "MONOGRAPH" -> CSLType.BOOK;
            case "DATASET" -> CSLType.DATASET;
            case "MONOGRAPH_PUBLICATION" -> CSLType.CHAPTER;
            case "PROCEEDINGS" -> CSLType.ARTICLE_MAGAZINE;
            default -> CSLType.ARTICLE; // Should never return
        };
    }
}
