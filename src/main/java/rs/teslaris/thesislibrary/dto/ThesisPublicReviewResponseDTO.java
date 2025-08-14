package rs.teslaris.thesislibrary.dto;

public record ThesisPublicReviewResponseDTO(
    String nameAndSurname,
    String titleSr,
    String titleOther,
    String organisationUnitNameSr,
    String organisationUnitNameOther,
    String scientificAreaSr,
    String scientificAreaOther,
    String publicReviewStartDate,
    String publicReviewEndDate,
    Integer databaseId
) {
}
