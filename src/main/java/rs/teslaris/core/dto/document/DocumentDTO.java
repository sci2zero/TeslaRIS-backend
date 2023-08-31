package rs.teslaris.core.dto.document;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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

    @NotBlank(message = "You have to provide a valid document date.")
    private String documentDate;

    private String doi;

    private String scopusId;

    private Integer eventId;
}
