package rs.teslaris.project.dto.project;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.project.model.project.ProjectCollaborationType;
import rs.teslaris.project.model.project.ProjectResearchType;
import rs.teslaris.project.model.project.ProjectStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO {

    private Integer id;

    private Set<String> internalIdentifiers = new HashSet<>();

    private Set<String> oldIds = new HashSet<>();

    private Set<String> mergedIds = new HashSet<>();

    private String doi;

    private String raid;

    @Valid
    @NotNull(message = "You have to provide a project name.")
    @NotEmpty(message = "You have to provide a project name.")
    private List<MultilingualContentDTO> name;

    private List<MultilingualContentDTO> description = new ArrayList<>();

    private List<MultilingualContentDTO> nameAbbreviation = new ArrayList<>();

    private List<MultilingualContentDTO> keywords = new ArrayList<>();

    private Set<Integer> researchAreasId = new HashSet<>();

    private Set<String> uris = new HashSet<>();

    private LocalDate dateFrom;

    private LocalDate dateTo;

    @NotNull(message = "You have to provide a project status.")
    private ProjectStatus status;

    @NotNull(message = "You have to provide a collaboration type.")
    private ProjectCollaborationType collaborationType;

    @NotNull(message = "You have to provide a research type.")
    private ProjectResearchType researchType;

    private Boolean notFunded;

    private MonetaryAmountDTO costs;
}
