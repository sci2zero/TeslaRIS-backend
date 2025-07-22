package rs.teslaris.core.dto.document;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {

    private Integer id;

    private Integer oldId;

    @Valid
    @NotNull(message = "You have to provide a title.")
    private List<MultilingualContentDTO> title;

    @Valid
    @NotNull(message = "You have to provide a subtitle.")
    private List<MultilingualContentDTO> subTitle;

    @Valid
    @NotNull(message = "You have to provide a description.")
    private List<MultilingualContentDTO> description;

    @Valid
    @NotNull(message = "You have to provide keywords.")
    private List<MultilingualContentDTO> keywords;

    @Valid
    @NotNull(message = "You have to provide contributions.")
    private List<PersonDocumentContributionDTO> contributions;

    @NotNull(message = "You have to provide a list of URIs.")
    private Set<String> uris;

    private String documentDate;

    private String doi;

    private String scopusId;

    private String openAlexId;

    @Positive(message = "Event Id must be a positive number.")
    private Integer eventId;

    // Used only for responses
    private List<DocumentFileResponseDTO> fileItems = new ArrayList<>();

    private List<DocumentFileResponseDTO> proofs = new ArrayList<>();

    private Boolean isMetadataValid;

    private Boolean areFilesValid;
}
