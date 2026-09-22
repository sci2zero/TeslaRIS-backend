package rs.teslaris.project.dto.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrepopulatedEventDTO {

    private List<MultilingualContentDTO> name = new ArrayList<>();

    private List<MultilingualContentDTO> description = new ArrayList<>();

    private String city;

    private String countryCode; // TODO: Change to CountryDTO and imlpement country lookup by code

    private List<String> uris = new ArrayList<>();
}
