package rs.teslaris.core.dto.document;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.commontypes.ResearchAreaHierarchyDTO;
import rs.teslaris.core.model.document.IntangibleProductType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IntangibleProductDTO extends DocumentDTO implements PublishableDTO {

    private String internalNumber;

    @Valid
    private List<MultilingualContentDTO> productUsers = new ArrayList<>();

    @NotNull(message = "You have to provide the intangible product type.")
    private IntangibleProductType intangibleProductType;

    @NotNull(message = "You have to provide research area ids.")
    private Set<Integer> researchAreasId = new HashSet<>();

    @Positive(message = "Publisher id cannot be a negative number.")
    private Integer publisherId;

    private Boolean authorReprint;

    // used only for responses

    private List<ResearchAreaHierarchyDTO> researchAreas = new ArrayList<>();
}
