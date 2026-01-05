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
import rs.teslaris.core.dto.institution.ResearchAreaDTO;
import rs.teslaris.core.model.document.MaterialProductType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaterialProductDTO extends DocumentDTO implements PublishableDTO {

    private String internalNumber;

    private Long numberProduced;

    @Valid
    private List<MultilingualContentDTO> productUsers = new ArrayList<>();

    @NotNull(message = "You have to provide the material product type.")
    private MaterialProductType materialProductType;

    @NotNull(message = "You have to provide research area ids.")
    private Set<Integer> researchAreasId = new HashSet<>();

    @Positive(message = "Publisher id cannot be a negative number.")
    private Integer publisherId;

    private Boolean authorReprint;

    // used only for responses

    private List<ResearchAreaHierarchyDTO> researchAreas = new ArrayList<>();
}
