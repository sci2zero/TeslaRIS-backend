package rs.teslaris.core.dto.document;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.document.ThesisType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThesisDTO extends DocumentDTO implements PublishableDTO {

    @Positive(message = "Publisher id cannot be a negative number.")
    private Integer organisationUnitId;

    private List<MultilingualContentDTO> externalOrganisationUnitName;

    @NotNull(message = "You have to provide thesis type.")
    private ThesisType thesisType;

    @Positive(message = "Number of pages cannot be a negative number.")
    private Integer numberOfPages;

    @PositiveOrZero(message = "Number of chapters cannot be a negative number.")
    private Integer numberOfChapters;

    @PositiveOrZero(message = "Number of references cannot be a negative number.")
    private Integer numberOfReferences;

    @PositiveOrZero(message = "Number of tables cannot be a negative number.")
    private Integer numberOfTables;

    @PositiveOrZero(message = "Number of illustrations cannot be a negative number.")
    private Integer numberOfIllustrations;

    @PositiveOrZero(message = "Number of graphs cannot be a negative number.")
    private Integer numberOfGraphs;

    @PositiveOrZero(message = "Number of appendices cannot be a negative number.")
    private Integer numberOfAppendices;

    @Positive
    private Integer languageId;

    @Positive
    private Integer writingLanguageTagId;

    private List<MultilingualContentDTO> scientificArea = new ArrayList<>();

    private List<MultilingualContentDTO> scientificSubArea = new ArrayList<>();

    @Positive(message = "Publisher id cannot be a negative number.")
    private Integer publisherId;

    private Boolean authorReprint;

    private LocalDate topicAcceptanceDate;

    private LocalDate thesisDefenceDate;

    private String printISBN;

    private String eisbn;

    private List<MultilingualContentDTO> placeOfKeep = new ArrayList<>();

    private String udc;

    private List<MultilingualContentDTO> alternateTitle = new ArrayList<>();

    private List<MultilingualContentDTO> extendedAbstract = new ArrayList<>();

    private List<MultilingualContentDTO> typeOfTitle = new ArrayList<>();

    // Only for migration
    private LocalDate publicReviewStartDate;

    private Boolean isArchived;

    private Boolean isOnPublicReview;
}
