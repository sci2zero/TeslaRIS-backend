package rs.teslaris.core.converter.commontypes;

import rs.teslaris.core.dto.commontypes.DocumentDeduplicationSuggestionDTO;
import rs.teslaris.core.model.commontypes.DocumentDeduplicationSuggestion;

public class DocumentDeduplicationSuggestionConverter {

    public static DocumentDeduplicationSuggestionDTO toDTO(
        DocumentDeduplicationSuggestion suggestion) {
        var dto = new DocumentDeduplicationSuggestionDTO();

        dto.setId(suggestion.getId());
        dto.setLeftDocumentId(suggestion.getLeftDocument().getId());
        dto.setRightDocumentId(suggestion.getRightDocument().getId());
        dto.setLeftDocumentTitle(MultilingualContentConverter.getMultilingualContentDTO(
            suggestion.getLeftDocument().getTitle()));
        dto.setRightDocumentTitle(MultilingualContentConverter.getMultilingualContentDTO(
            suggestion.getRightDocument().getTitle()));
        dto.setDocumentPublicationType(suggestion.getDocumentType());

        return dto;
    }
}
