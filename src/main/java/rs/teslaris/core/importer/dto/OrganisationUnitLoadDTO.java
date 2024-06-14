package rs.teslaris.core.importer.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnitLoadDTO {

    private List<MultilingualContentDTO> name = new ArrayList<>();

    private String nameAbbreviation;

    private String scopusAfid;
}
