package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.apache.tika.io.FilenameUtils;
import org.apache.tika.language.detect.LanguageDetector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.indexmodel.DocumentFileIndex;
import rs.teslaris.core.indexrepository.DocumentFileIndexRepository;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.document.DocumentFileRepository;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.util.ResourceMultipartFile;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.MissingDataException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.ExpressionTransformer;
import rs.teslaris.core.util.search.SearchRequestType;

@Service
@RequiredArgsConstructor
@Transactional
@Traceable
public class DocumentFileServiceImpl extends JPAServiceImpl<DocumentFile>
    implements DocumentFileService {

    private final FileService fileService;

    private final DocumentFileRepository documentFileRepository;

    private final DocumentRepository documentRepository;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final MultilingualContentService multilingualContentService;

    private final DocumentFileIndexRepository documentFileIndexRepository;

    private final LanguageDetector languageDetector;

    private final SearchService<DocumentFileIndex> searchService;

    private final ExpressionTransformer expressionTransformer;
    private final Tika tika = new Tika();
    @Value("${document_file.approved_by_default}")
    private Boolean documentFileApprovedByDefault;

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
    public DocumentFile getDocumentByServerFilename(String serverFilename) {
        return documentFileRepository.getReferenceByServerFilename(serverFilename)
            .orElseThrow(() -> new NotFoundException("Document with given name does not exist."));
    }

    @Override
    public DocumentFileIndex findDocumentFileIndexByDatabaseId(Integer databaseId) {
        return documentFileIndexRepository.findDocumentFileIndexByDatabaseId(databaseId)
            .orElseThrow(
                () -> new NotFoundException("Document file index with given ID does not exist."));
    }

    private void setCommonFields(DocumentFile documentFile, DocumentFileDTO documentFileDTO) {
        if (documentFileDTO.getFile().getSize() > 0) {
            documentFile.setFilename(documentFileDTO.getFile().getOriginalFilename());
            documentFile.setMimeType(detectMimeType(documentFileDTO.getFile()));
            documentFile.setFileSize(
                Math.floorDiv(documentFileDTO.getFile().getSize(), (1024 * 1024)));
        }

        if (Objects.nonNull(documentFileDTO.getDescription())) {
            documentFile.setDescription(
                multilingualContentService.getMultilingualContent(
                    documentFileDTO.getDescription()));
        }

        documentFile.setResourceType(documentFileDTO.getResourceType());
        documentFile.setAccessRights(documentFileDTO.getAccessRights());

        if (documentFile.getAccessRights().equals(AccessRights.OPEN_ACCESS)) {
            if ((documentFile.getResourceType().equals(ResourceType.OFFICIAL_PUBLICATION) ||
                documentFile.getResourceType().equals(ResourceType.PREPRINT)) &&
                Objects.isNull(documentFileDTO.getLicense())) {
                throw new MissingDataException(
                    "You have to provide CC licence for open access documents.");
            }
            documentFile.setLicense(documentFileDTO.getLicense());
        }

        documentFile.setTimestamp(LocalDateTime.now());
    }

    @Override
    public DocumentFile saveNewDocument(DocumentFileDTO documentFile, Boolean index) {
        var newDocumentFile = new DocumentFile();

        setCommonFields(newDocumentFile, documentFile);

        if (!index) {
            documentFile.setResourceType(
                ResourceType.PROOF); // Save every non-indexed (proof) as its own type
        }

        return saveDocument(documentFile, newDocumentFile, index);
    }

    @Override
    public DocumentFile saveNewPublicationDocument(DocumentFileDTO documentFile, Boolean index,
                                                   Document document) {
        var newDocumentFile = new DocumentFile();

        setCommonFields(newDocumentFile, documentFile);
        newDocumentFile.setIsVerifiedData(document instanceof Thesis);
        newDocumentFile.setDocument(document);

        if (!index) {
            documentFile.setResourceType(
                ResourceType.PROOF); // Save every non-indexed (proof) as its own type
        }

        return saveDocument(documentFile, newDocumentFile, index);
    }

    @Override
    public DocumentFile saveNewPersonalDocument(DocumentFileDTO documentFile, Boolean index,
                                                Person person) {
        var newDocumentFile = new DocumentFile();

        setCommonFields(newDocumentFile, documentFile);
        newDocumentFile.setIsVerifiedData(false);
        newDocumentFile.setPerson(person);

        if (!index) {
            documentFile.setResourceType(
                ResourceType.PROOF); // Save every non-indexed (proof) as its own type
        }

        return saveDocument(documentFile, newDocumentFile, index);
    }

    @Override
    public DocumentFile saveNewPreliminaryDocument(DocumentFileDTO documentFile) {
        var newDocumentFile = new DocumentFile();

        setCommonFields(newDocumentFile, documentFile);
        newDocumentFile.setCanEdit(false);
        newDocumentFile.setLatest(true);
        newDocumentFile.setIsVerifiedData(true);

        return saveDocument(documentFile, newDocumentFile, false);
    }

    private DocumentFile saveDocument(DocumentFileDTO documentFile, DocumentFile newDocumentFile,
                                      Boolean index) {
        var serverFilename =
            fileService.store(documentFile.getFile(), UUID.randomUUID().toString());
        newDocumentFile.setServerFilename(serverFilename);

        newDocumentFile.setApproveStatus(
            documentFileApprovedByDefault ? ApproveStatus.APPROVED : ApproveStatus.REQUESTED);
        var savedDocumentFile = save(newDocumentFile);

        if (index && newDocumentFile.getApproveStatus().equals(ApproveStatus.APPROVED)) {
            parseAndIndexPdfDocument(newDocumentFile, documentFile.getFile(), serverFilename,
                new DocumentFileIndex());
        }

        return savedDocumentFile;
    }

    @Override
    public DocumentFileResponseDTO editDocumentFile(DocumentFileDTO documentFile, Boolean index,
                                                    Integer documentId) {
        var documentFileResponse = editDocumentFile(documentFile, index);

        if (Objects.nonNull(documentId) && index) {
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId)
                .ifPresent(documentIndex -> {
                    documentIndex.setIsOpenAccess(
                        documentRepository.isDocumentPubliclyAvailable(documentId));
                    documentPublicationIndexRepository.save(documentIndex);
                });
        }

        return documentFileResponse;
    }

    @Override
    public DocumentFileResponseDTO editDocumentFile(DocumentFileDTO documentFile, Boolean index) {
        var documentFileToEdit = findDocumentFileById(documentFile.getId());

        if (!documentFileToEdit.getCanEdit()) {
            throw new StorageException(
                "Document file with ID " + documentFile.getId() + " can't be edited.");
        }

        setCommonFields(documentFileToEdit, documentFile);

        if (!index) {
            documentFile.setResourceType(
                ResourceType.PROOF); // Save every non-indexed (proof) as supplement
        }

        if (documentFile.getFile().getSize() > 0) {
            var serverFilename =
                fileService.store(documentFile.getFile(), documentFileToEdit.getServerFilename());
            documentFileToEdit.setServerFilename(serverFilename);

            if (index && documentFileToEdit.getApproveStatus().equals(ApproveStatus.APPROVED)) {
                try {
                    var documentIndexToUpdate =
                        findDocumentFileIndexByDatabaseId(documentFileToEdit.getId());
                    parseAndIndexPdfDocument(documentFileToEdit, documentFile.getFile(),
                        documentFileToEdit.getServerFilename(), documentIndexToUpdate);


                } catch (NotFoundException e) {
                    return DocumentFileConverter.toDTO(
                        documentFileRepository.save(documentFileToEdit));
                }
            }
        }
        return DocumentFileConverter.toDTO(documentFileRepository.save(documentFileToEdit));
    }

    @Override
    public void deleteDocumentFile(String serverFilename) {
        documentFileRepository.getReferenceByServerFilename(serverFilename)
            .ifPresent(documentToDelete -> {
                fileService.delete(serverFilename);
                delete(documentToDelete.getId());
            });
    }

    @Override
    public void changeApproveStatus(Integer documentFileId, Boolean approved) throws IOException {
        var documentFile = findOne(documentFileId);
        documentFile.setApproveStatus(approved ? ApproveStatus.APPROVED : ApproveStatus.DECLINED);
        save(documentFile);

        var fileResource = fileService.loadAsResource(documentFile.getServerFilename());

        parseAndIndexPdfDocument(documentFile,
            new ResourceMultipartFile(documentFile.getServerFilename(), documentFile.getFilename(),
                documentFile.getMimeType(),
                new ByteArrayResource(
                    new InputStreamResource(fileResource).getContentAsByteArray())),
            documentFile.getServerFilename(), new DocumentFileIndex());
    }

    private String detectMimeType(MultipartFile file) {
        var contentAnalyzer = new Tika();

        String trueMimeType;
        String specifiedMimeType;

        try {
            trueMimeType = contentAnalyzer.detect(file.getInputStream());

            var originalFilename = FilenameUtils.normalize(
                Objects.requireNonNullElse(file.getOriginalFilename(), ""));
            if (originalFilename.isEmpty()) {
                throw new StorageException("File does not have a valid name.");
            }
            specifiedMimeType = Files.probeContentType(Path.of(originalFilename));

        } catch (IOException e) {
            throw new StorageException("Failed to detect MIME type for file.");
        }

        if (!trueMimeType.equals(specifiedMimeType)) {
            if (!(trueMimeType.contains("zip") && specifiedMimeType.contains("zip"))) {
                throw new StorageException(String.format(
                    "MIME type mismatch: detected [%s], specified [%s]. Aborting.",
                    trueMimeType,
                    specifiedMimeType
                ));
            }
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

    @Override
    @Nullable
    public Integer findDocumentIdForFilename(String filename) {
        return documentFileRepository.getDocumentIdByFilename(filename);
    }

    private Query buildSimpleSearchQuery(List<String> tokens) {
        var minShouldMatch = (int) Math.ceil(tokens.size() * 0.8);

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
            return b.minimumShouldMatch(Integer.toString(minShouldMatch));
        })))._toQuery();
    }

    private boolean isPdfFile(MultipartFile multipartFile) {
        try {
            var isSpecifiedPDF = Objects.equals(multipartFile.getContentType(), "application/pdf");
            var detectedType = tika.detect(multipartFile.getInputStream());

            if (isSpecifiedPDF && !detectedType.equals("application/pdf")) {
                throw new LoadingException("MIME type mismatch for indexable document.");
            }

            return isSpecifiedPDF;
        } catch (IOException e) {
            return false;
        }
    }

    private String extractDocumentContent(MultipartFile multipartPdfFile) {
        try (var inputStream = multipartPdfFile.getInputStream();
             var pdDocument = Loader.loadPDF(inputStream.readAllBytes())) {
            var textStripper = new PDFTextStripper();
            return textStripper.getText(pdDocument);
        } catch (IOException e) {
            throw new LoadingException("Error while trying to load PDF file content.");
        }
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

        documentFileIndexRepository.save(documentIndex);
    }
}
