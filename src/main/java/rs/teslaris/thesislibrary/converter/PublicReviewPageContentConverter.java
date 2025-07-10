package rs.teslaris.thesislibrary.converter;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.thesislibrary.dto.PublicReviewPageContentDTO;
import rs.teslaris.thesislibrary.model.PublicReviewPageContent;

public class PublicReviewPageContentConverter {

    public static PublicReviewPageContentDTO toDTO(PublicReviewPageContent pageContent) {
        return new PublicReviewPageContentDTO(
            pageContent.getInstitution().getId(),
            pageContent.getContentType(),
            pageContent.getThesisTypes().stream().toList(),
            MultilingualContentConverter.getMultilingualContentDTO(pageContent.getContent())
        );
    }
}
