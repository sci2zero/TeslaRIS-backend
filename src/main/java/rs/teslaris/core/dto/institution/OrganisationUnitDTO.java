package rs.teslaris.core.dto.institution;


import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.commontypes.ResearchAreaHierarchyDTO;
import rs.teslaris.core.dto.person.ContactDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnitDTO {

    private Integer id;

    private List<MultilingualContentDTO> name;

    private String nameAbbreviation;

    private List<MultilingualContentDTO> keyword;

    private List<ResearchAreaHierarchyDTO> researchAreas;

    private String scopusAfid;

    private GeoLocationDTO location;

    private ContactDTO contact;

    private Set<String> uris;

    // Only for responses

    private String logoServerFilename;

    private String logoBackgroundHex;
}
