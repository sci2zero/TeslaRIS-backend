package rs.teslaris.thesislibrary.dto;

public record ThesisPublicReviewResponseDTO(
    String nameAndSurname,
    String titleSr,
    String titleOther,
    String organisationUnitNameSr,
    String organisationUnitNameOther,
    String scientificArea,
    String publicReviewStartDate,
    String publicReviewEndDate,
    Integer databaseId
) {
}
