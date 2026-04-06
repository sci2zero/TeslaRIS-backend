package rs.teslaris.thesislibrary.dto;

import java.util.List;

public record ThesisPublicReviewResponseDTO(
    String nameAndSurname,
    String titleSr,
    String titleOther,
    String organisationUnitNameSr,
    String organisationUnitNameOther,
    String scientificAreaSr,
    String scientificAreaOther,
    List<String> publicReviewStartDates,
    List<String> publicReviewEndDates,
    Boolean isOnShortenedReview,
    Integer databaseId
) {
}
