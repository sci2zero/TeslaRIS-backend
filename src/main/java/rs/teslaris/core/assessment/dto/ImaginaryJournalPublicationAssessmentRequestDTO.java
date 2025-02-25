package rs.teslaris.core.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImaginaryJournalPublicationAssessmentRequestDTO {

    private Integer journalId;

    private Integer commissionId;

    private Integer classificationYear;

    private String researchAreaCode;

    private Integer authorCount;
}
