package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.apache.tika.language.detect.LanguageDetector;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.indexmodel.DocumentFileIndex;
import rs.teslaris.core.indexrepository.DocumentFileIndexRepository;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.repository.document.DocumentFileRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchRequestType;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentFileServiceImpl extends JPAServiceImpl<DocumentFile>
    implements DocumentFileService {

    private final FileService fileService;

    private final DocumentFileRepository documentFileRepository;

    private final MultilingualContentService multilingualContentService;

    private final DocumentFileIndexRepository documentFileIndexRepository;

    private final LanguageDetector languageDetector;

    private final SearchService<DocumentFileIndex> searchService;

    private final ExpressionTransformer expressionTransformer;

    @Override
    protected JpaRepository<DocumentFile, Integer> getEntityRepository() {
        return documentFileRepository;
    }

    @Override
    @Deprecated(forRemoval = true)
    public DocumentFile findDocumentFileById(Integer id) {
        return documentFileRepository.findById(id).orElseThrow(
            () -> new NotFoundException("Document file with given id does not exist."));
    }

    @Override
    public DocumentFileIndex findDocumentFileIndexByDatabaseId(Integer databaseId) {
        return documentFileIndexRepository.findDocumentFileIndexByDatabaseId(databaseId)
            .orElseThrow(
                () -> new NotFoundException("Document file index with given ID does not exist."));
    }

    private void setCommonFields(DocumentFile documentFile, DocumentFileDTO documentFileDTO) {
        documentFile.setFilename(documentFileDTO.getFile().getOriginalFilename());
        documentFile.setDescription(
            multilingualContentService.getMultilingualContent(documentFileDTO.getDescription()));

        documentFile.setMimeType(detectMimeType(documentFileDTO.getFile()));
        documentFile.setFileSize(documentFileDTO.getFile().getSize());
        documentFile.setResourceType(documentFileDTO.getResourceType());
        documentFile.setLicense(documentFileDTO.getLicense());
    }

    @Override
    public DocumentFile saveNewDocument(DocumentFileDTO documentFile, Boolean index) {
        var newDocumentFile = new DocumentFile();

        setCommonFields(newDocumentFile, documentFile);

        var serverFilename =
            fileService.store(documentFile.getFile(), UUID.randomUUID().toString());
        newDocumentFile.setServerFilename(serverFilename);

        newDocumentFile = save(newDocumentFile);

        if (index) {
            parseAndIndexPdfDocument(newDocumentFile, documentFile.getFile(), serverFilename,
                new DocumentFileIndex());
        }

        return newDocumentFile;
    }

    @Override
    public void editDocumentFile(DocumentFileDTO documentFile) {
        var documentFileToEdit = findDocumentFileById(documentFile.getId());

        setCommonFields(documentFileToEdit, documentFile);

        fileService.store(documentFile.getFile(), documentFileToEdit.getServerFilename());
        documentFileRepository.save(documentFileToEdit);

        var documentIndexToUpdate = findDocumentFileIndexByDatabaseId(documentFileToEdit.getId());

        parseAndIndexPdfDocument(documentFileToEdit, documentFile.getFile(),
            documentFileToEdit.getServerFilename(), documentIndexToUpdate);
    }

    @Override
    public void deleteDocumentFile(String serverFilename) {
        var documentToDelete = documentFileRepository.getReferenceByServerFilename(serverFilename);
        fileService.delete(serverFilename);
        delete(documentToDelete.getId());
    }

    private String detectMimeType(MultipartFile file) {
        var contentAnalyzer = new Tika();

        String trueMimeType;
        String specifiedMimeType;
        try {
            trueMimeType = contentAnalyzer.detect(file.getBytes());
            specifiedMimeType =
                Files.probeContentType(Path.of(Objects.requireNonNull(file.getOriginalFilename())));
        } catch (IOException e) {
            throw new StorageException("Failed to detect mime type for file.");
        }

        if (!trueMimeType.equals(specifiedMimeType) &&
            !(trueMimeType.contains("zip") && specifiedMimeType.contains("zip"))) {
            throw new StorageException("True mime type is different from specified one, aborting.");
        }

        return trueMimeType;
    }

    @Override
    public void parseAndIndexPdfDocument(DocumentFile documentFile, MultipartFile multipartPdfFile,
                                         String serverFilename, DocumentFileIndex documentIndex) {
        if (!isPdfFile(multipartPdfFile)) {
            return;
        }

        var documentContent = extractDocumentContent(multipartPdfFile);
        var documentTitle = extractDocumentTitle(multipartPdfFile);

        var contentLanguageDetected = detectLanguage(documentContent);
        var titleLanguageDetected = detectLanguage(documentTitle);

        saveDocumentIndex(documentContent, documentTitle, contentLanguageDetected,
            titleLanguageDetected, documentFile, serverFilename, documentIndex);

        documentFileIndexRepository.save(documentIndex);
    }

    @Override
    public Page<DocumentFileIndex> searchDocumentFiles(List<String> tokens,
                                                       Pageable pageable, SearchRequestType type) {
        if (type.equals(SearchRequestType.SIMPLE)) {
            return searchService.runQuery(buildSimpleSearchQuery(tokens),
                pageable,
                DocumentFileIndex.class, "document_file");
        }

        return searchService.runQuery(
            expressionTransformer.parseAdvancedQuery(tokens), pageable,
            DocumentFileIndex.class, "document_file");
    }

    @Override
    public void deleteIndexes() {
        documentFileIndexRepository.deleteAll();
    }

    private Query buildSimpleSearchQuery(List<String> tokens) {
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            tokens.forEach(token -> {
                b.should(sb -> sb.match(
                    m -> m.field("title_sr").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("title_other").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("description_sr").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("description_other").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("pdf_text_sr").query(token)));
                b.should(sb -> sb.match(
                    m -> m.field("pdf_text_other")
                        .query(token)));
            });
            return b;
        })))._toQuery();
    }

    private boolean isPdfFile(MultipartFile multipartFile) {
        return Objects.equals(multipartFile.getContentType(), "application/pdf");
    }

    private String extractDocumentContent(MultipartFile multipartPdfFile) {
        String documentContent;
        try (var pdfFile = multipartPdfFile.getInputStream()) {
            var pdDocument = PDDocument.load(pdfFile);
            var textStripper = new PDFTextStripper();
            documentContent = textStripper.getText(pdDocument);
            pdDocument.close();
        } catch (IOException e) {
            throw new LoadingException("Error while trying to load PDF file content.");
        }
        return documentContent;
    }

    private String extractDocumentTitle(MultipartFile multipartPdfFile) {
        var originalFilename = Objects.requireNonNull(multipartPdfFile.getOriginalFilename());
        return originalFilename.split("\\.")[0];
    }

    private String detectLanguage(String text) {
        return languageDetector.detect(text).getLanguage().toUpperCase();
    }

    private void saveDocumentIndex(String documentContent, String documentTitle,
                                   String contentLanguageDetected, String titleLanguageDetected,
                                   DocumentFile documentFile, String serverFilename,
                                   DocumentFileIndex documentIndex) {

        if (contentLanguageDetected.equals(LanguageAbbreviations.CROATIAN) ||
            contentLanguageDetected.equals(LanguageAbbreviations.SERBIAN)) {
            documentIndex.setPdfTextSr(documentContent);
        } else {
            documentIndex.setPdfTextOther(documentContent);
        }

        if (titleLanguageDetected.equals(LanguageAbbreviations.CROATIAN) ||
            titleLanguageDetected.equals(LanguageAbbreviations.SERBIAN)) {
            documentIndex.setTitleSr(documentTitle);
        } else {
            documentIndex.setTitleOther(documentTitle);
        }

        documentIndex.setDescriptionSr("");
        documentFile.getDescription().stream()
            .filter(d -> d.getLanguage().getLanguageTag().startsWith(LanguageAbbreviations.SERBIAN))
            .forEach(
                d -> documentIndex.setDescriptionSr(
                    documentIndex.getDescriptionSr() + d.getContent()));

        documentIndex.setDescriptionOther("");
        documentFile.getDescription().stream()
            .filter(
                d -> !d.getLanguage().getLanguageTag().startsWith(LanguageAbbreviations.SERBIAN))
            .forEach(
                d -> documentIndex.setDescriptionOther(
                    documentIndex.getDescriptionOther() + d.getContent() + " | "));

        documentIndex.setServerFilename(serverFilename);
        documentIndex.setDatabaseId(documentFile.getId());
    }
}
