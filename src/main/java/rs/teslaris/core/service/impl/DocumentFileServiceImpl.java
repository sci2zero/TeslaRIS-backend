package rs.teslaris.core.service.impl;

import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.repository.document.DocumentFileRepository;
import rs.teslaris.core.service.DocumentFileService;
import rs.teslaris.core.service.FileService;
import rs.teslaris.core.service.MultilingualContentService;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentFileServiceImpl implements DocumentFileService {

    private FileService fileService;

    private DocumentFileRepository documentFileRepository;

    private MultilingualContentService multilingualContentService;


    @Override
    public DocumentFile findDocumentFileById(Integer id) {
        return documentFileRepository.findById(id).orElseThrow(
            () -> new NotFoundException("Document file with given id does not exist."));
    }

    @Override
    public DocumentFile saveNewDocument(DocumentFileDTO documentFile) {
        var newDocumentFile = new DocumentFile();
        newDocumentFile.setFilename(documentFile.getFile().getOriginalFilename());
        newDocumentFile.setDescription(
            multilingualContentService.getMultilingualContent(documentFile.getDescription()));
        newDocumentFile.setMimeType(fileService.detectMimeType(documentFile.getFile()));
        newDocumentFile.setFileSize(documentFile.getFile().getSize());
        newDocumentFile.setResourceType(documentFile.getResourceType());
        newDocumentFile.setLicense(documentFile.getLicense());

        var serverFilename = fileService.store(documentFile.getFile());
        newDocumentFile.setServerFilename(serverFilename);

        return documentFileRepository.save(newDocumentFile);
    }
}
