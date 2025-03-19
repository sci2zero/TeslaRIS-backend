package rs.teslaris.core.assessment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.document.JournalPublicationType;
import rs.teslaris.core.model.document.ProceedingsPublicationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImaginaryPublicationAssessmentRequestDTO {

    private Integer containingEntityId;

    @NotNull(message = "You have to provide commission ID.")
    @Positive
    private Integer commissionId;

    @NotNull(message = "You have to provide classification year.")
    @Min(1999)
    private Integer classificationYear;

    @NotBlank(message = "You have to provide research area.")
    private String researchAreaCode;

    @NotNull(message = "You have to provide author count.")
    @Positive
    private Integer authorCount;

    private Boolean experimental;

    private Boolean theoretical;

    private Boolean simulation;

    private JournalPublicationType journalPublicationType;

    private ProceedingsPublicationType proceedingsPublicationType;

    private String issn;

    private String confId;
}
