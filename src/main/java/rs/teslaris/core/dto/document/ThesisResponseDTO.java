package rs.teslaris.core.dto.document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThesisResponseDTO extends ThesisDTO {

    private String languageCode;

    private Boolean isOnPublicReview;

    private Boolean isOnPublicReviewPause;

    private List<LocalDate> publicReviewDates;

    private LocalDate publicReviewEnd;

    private List<DocumentFileResponseDTO> preliminaryFiles = new ArrayList<>();

    private List<DocumentFileResponseDTO> preliminarySupplements = new ArrayList<>();

    private List<DocumentFileResponseDTO> commissionReports = new ArrayList<>();

    private Boolean publicReviewCompleted;
}
