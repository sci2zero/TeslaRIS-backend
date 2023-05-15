package rs.teslaris.core.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.exception.StorageException;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.repository.document.DocumentFileRepository;
import rs.teslaris.core.service.DocumentFileService;
import rs.teslaris.core.service.FileService;
import rs.teslaris.core.service.MultilingualContentService;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentFileServiceImpl implements DocumentFileService {

    private final FileService fileService;

    private final DocumentFileRepository documentFileRepository;

    private final MultilingualContentService multilingualContentService;


    @Override
    public DocumentFile findDocumentFileById(Integer id) {
        return documentFileRepository.findById(id).orElseThrow(
            () -> new NotFoundException("Document file with given id does not exist."));
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
    public DocumentFile saveNewDocument(DocumentFileDTO documentFile) {
        var newDocumentFile = new DocumentFile();

        setCommonFields(newDocumentFile, documentFile);

        var serverFilename =
            fileService.store(documentFile.getFile(), UUID.randomUUID().toString());
        newDocumentFile.setServerFilename(serverFilename);

        return documentFileRepository.save(newDocumentFile);
    }

    @Override
    public void editDocumentFile(DocumentFileDTO documentFile) {
        var documentFileToEdit = findDocumentFileById(documentFile.getId());

        setCommonFields(documentFileToEdit, documentFile);

        fileService.store(documentFile.getFile(), documentFileToEdit.getServerFilename());
        documentFileRepository.save(documentFileToEdit);
    }

    @Override
    public void deleteDocumentFile(String serverFilename) {
        var documentToDelete = documentFileRepository.getReferenceByServerFilename(serverFilename);
        fileService.delete(serverFilename);
        documentFileRepository.delete(documentToDelete);
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
}
