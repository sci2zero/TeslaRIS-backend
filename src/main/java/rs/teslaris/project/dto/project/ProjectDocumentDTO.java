package rs.teslaris.project.dto.project;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.model.project.ProjectDocumentType;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDocumentDTO {

    private Integer id;

    @NotNull(message = "You have to provide project id.")
    private Integer projectId;

    @NotNull(message = "You have to provide document id.")
    private Integer documentId;

    @Valid
    private List<FundingPartDTO> fundingParts = new ArrayList<>();

    @Valid
    private List<MultilingualContentDTO> textualDescription;

    @NotNull(message = "You have to provide relation type.")
    private ProjectDocumentType relationType;

}
