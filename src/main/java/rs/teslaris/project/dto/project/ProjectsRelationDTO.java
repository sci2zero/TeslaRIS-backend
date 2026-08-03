package rs.teslaris.project.dto.project;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.project.model.project.ProjectsRelationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectsRelationDTO {

    private Integer id;

    @Valid
    private List<MultilingualContentDTO> sourceProjectDescription = new ArrayList<>();

    @Valid
    private List<MultilingualContentDTO> targetProjectDescription = new ArrayList<>();

    @NotNull(message = "You have to provide a valid relation type.")
    private ProjectsRelationType relationType;

    @NotNull(message = "You have to provide a start date.")
    private LocalDate dateFrom;

    @NotNull(message = "You have to provide an end date.")
    private LocalDate dateTo;

    private Integer sourceProjectId;

    @Positive(message = "Project id must be a positive number.")
    private Integer targetProjectId;
}