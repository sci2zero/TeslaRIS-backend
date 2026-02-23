package rs.teslaris.importer.service.impl.worker;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.PrintedPageable;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.document.DocumentLookupService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.util.search.CollectionOperations;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.MultilingualContent;

@Service
@RequiredArgsConstructor
public class DocumentEnrichmentWorker {

    private final DocumentLookupService documentLookupService;

    private final LanguageTagService languageTagService;

    private final DocumentPublicationService documentPublicationService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enrichDocumentMetadata(Integer documentId, DocumentImport documentImport) {
        var optionalIndex =
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId);

        if (optionalIndex.isEmpty()) {
            return;
        }

        var index = optionalIndex.get();
        var document = documentLookupService.fastDocumentLookup(index);

        enrichMultilingual(document::getSubTitle,
            documentImport.getSubtitle(),
            document::setSubTitle);

        enrichMultilingual(document::getDescription,
            documentImport.getDescription(),
            document::setDescription);

        enrichMultilingual(document::getKeywords,
            documentImport.getKeywords(),
            document::setKeywords);

        if (!CollectionOperations.containsValues(document.getUris()) &&
            CollectionOperations.containsValues(documentImport.getUris())) {
            document.setUris(new HashSet<>(documentImport.getUris()));
        }

        setIfMissing(document.getScopusId(), documentImport.getScopusId(),
            document::setScopusId);

        setIfMissing(document.getOpenAlexId(), documentImport.getOpenAlexId(),
            document::setOpenAlexId);

        setIfMissing(document.getWebOfScienceId(), documentImport.getWebOfScienceId(),
            document::setWebOfScienceId);

        if (document instanceof PrintedPageable printable) {
            setIfMissing(printable.getStartPage(), documentImport.getStartPage(),
                printable::setStartPage);

            setIfMissing(printable.getEndPage(), documentImport.getEndPage(),
                printable::setEndPage);

            if (Objects.isNull(printable.getNumberOfPages()) &&
                Objects.nonNull(documentImport.getNumberOfPages())) {
                printable.setNumberOfPages(documentImport.getNumberOfPages());
                index.setNumberOfPages(documentImport.getNumberOfPages());
            }

            setIfMissing(printable.getArticleNumber(), documentImport.getArticleNumber(),
                printable::setArticleNumber);
        }

        if (document instanceof JournalPublication journal) {
            setIfMissing(journal.getVolume(), documentImport.getVolume(),
                journal::setVolume);

            setIfMissing(journal.getIssue(), documentImport.getIssue(),
                journal::setIssue);
        } else if (document instanceof Monograph monograph) {
            setIfMissing(monograph.getVolume(), documentImport.getVolume(),
                monograph::setVolume);
        }

        documentPublicationService.indexCommonFields(document, index);
        documentPublicationService.save(document);
        documentPublicationIndexRepository.save(index);
    }

    private void enrichMultilingual(Supplier<Set<MultiLingualContent>> existingSupplier,
                                    List<MultilingualContent> source,
                                    Consumer<Set<MultiLingualContent>> setter) {
        if (!CollectionOperations.containsValues(existingSupplier.get()) &&
            CollectionOperations.containsValues(source)) {
            setter.accept(toMultilingualContent(source));
        }
    }

    private void setIfMissing(String current, String incoming, Consumer<String> setter) {
        if (!StringUtil.valueExists(current) && StringUtil.valueExists(incoming)) {
            setter.accept(incoming);
        }
    }

    private Set<MultiLingualContent> toMultilingualContent(List<MultilingualContent> sourceList) {
        var target = new HashSet<MultiLingualContent>();

        sourceList.forEach(source -> {
            if (Objects.isNull(source.getContent())) {
                return;
            }

            var languageTag =
                languageTagService.findLanguageTagByValue(source.getLanguageTag());

            target.add(new MultiLingualContent(
                languageTag,
                source.getContent(),
                source.getPriority()));
        });

        return target;
    }
}
