package rs.teslaris.core.converter.document;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.model.document.DocumentFile;

public class DocumentFileConverter {

    public static DocumentFileResponseDTO toDTO(DocumentFile documentFile) {
        return new DocumentFileResponseDTO(documentFile.getId(), documentFile.getFilename(),
            documentFile.getServerFilename(),
            MultilingualContentConverter.getMultilingualContentDTO(
                documentFile.getDescription()), documentFile.getResourceType(),
            documentFile.getLicense(), documentFile.getFileSize());
    }
}
