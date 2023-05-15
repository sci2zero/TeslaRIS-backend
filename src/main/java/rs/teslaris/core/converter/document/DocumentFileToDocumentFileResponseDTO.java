package rs.teslaris.core.converter.document;

import rs.teslaris.core.converter.commontypes.MultilingualContentToMultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.model.document.DocumentFile;

public class DocumentFileToDocumentFileResponseDTO {

    public static DocumentFileResponseDTO toDTO(DocumentFile documentFile) {
        return new DocumentFileResponseDTO(documentFile.getId(), documentFile.getFilename(),
            documentFile.getServerFilename(),
            MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
                documentFile.getDescription()), documentFile.getResourceType(),
            documentFile.getLicense());
    }
}
