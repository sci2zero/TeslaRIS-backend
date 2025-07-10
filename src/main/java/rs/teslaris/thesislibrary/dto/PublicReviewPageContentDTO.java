package rs.teslaris.thesislibrary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.thesislibrary.model.PageContentType;

public record PublicReviewPageContentDTO(
    Integer institutionId,

    @NotNull(message = "You have to provide a page content type.")
    PageContentType contentType,

    @NotNull(message = "You have to provide applicable thesis types.")
    List<ThesisType> thesisTypes,

    @NotNull(message = "You have to provide content.")
    @Valid
    List<MultilingualContentDTO> content
) {
}
