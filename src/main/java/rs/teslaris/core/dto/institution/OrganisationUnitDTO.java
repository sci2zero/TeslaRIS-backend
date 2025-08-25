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
import rs.teslaris.core.model.document.ThesisType;

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

    private String openAlexId;

    private String ror;

    private GeoLocationDTO location;

    private ContactDTO contact;

    private Set<String> uris;

    // Only for responses

    private String logoServerFilename;

    private String logoBackgroundHex;

    private List<ThesisType> allowedThesisTypes;
}
