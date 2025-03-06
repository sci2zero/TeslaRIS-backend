package rs.teslaris.core.assessment.dto;

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

    private Integer commissionId;

    private Integer classificationYear;

    private String researchAreaCode;

    private Integer authorCount;

    private Boolean experimental;

    private Boolean theoretical;

    private Boolean simulation;

    private JournalPublicationType journalPublicationType;

    private ProceedingsPublicationType proceedingsPublicationType;
}
