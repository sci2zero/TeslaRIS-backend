package rs.teslaris.project.dto.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.project.model.project.ProjectStatus;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrepopulatedProjectMetadataDTO {

    private String doi;

    private List<MultilingualContentDTO> name = new ArrayList<>();

    private List<MultilingualContentDTO> nameAbbreviation = new ArrayList<>();

    private List<MultilingualContentDTO> description = new ArrayList<>();

    private List<MultilingualContentDTO> keywords = new ArrayList<>();

    private List<String> uris = new ArrayList<>();

    private ProjectStatus status;

    private String dateFrom;

    private String dateTo;

    private MonetaryAmountDTO costs;

    private List<MultilingualContentDTO> funderName = new ArrayList<>();

    private List<PrepopulatedPersonDTO> persons = new ArrayList<>();

    private List<PrepopulatedOrganisationDTO> organisations = new ArrayList<>();

    private List<PrepopulatedEventDTO> events = new ArrayList<>();
}