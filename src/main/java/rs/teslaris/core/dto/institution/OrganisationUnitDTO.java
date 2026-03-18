package rs.teslaris.core.dto.institution;


import java.time.LocalDate;
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
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.OrganisationUnitSector;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnitDTO {

    private Integer id;

    private List<MultilingualContentDTO> name;

    private List<MultilingualContentDTO> nameAbbreviation;

    private List<MultilingualContentDTO> description;

    private List<MultilingualContentDTO> keyword;

    private Set<ResearchAreaHierarchyDTO> researchAreas;

    private String scopusAfid;

    private String openAlexId;

    private String ror;

    private String ringgold;

    private String fundref;

    private String isni;

    private String athensId;

    private String ncesId;

    private String fctId;

    private String dgeecId;

    private String nifId;

    private GeoLocationDTO location;

    private ContactDTO contact;

    private Set<String> uris;

    private OrganisationUnitSector sector;

    private Boolean startup;

    private LocalDate dateEstablished;

    private PostalAddressDTO postalAddress;

    // Only for responses

    private String logoServerFilename;

    private String logoBackgroundHex;

    private List<ThesisType> allowedThesisTypes;

    private boolean isClientInstitutionCris;

    private String institutionEmailDomainCris;

    private boolean validatingEmailDomainCris;

    private boolean allowingSubdomainsCris;

    private boolean isLegalEntity;

    private boolean isClientInstitutionDl;

    private String institutionEmailDomainDl;

    private boolean validatingEmailDomainDl;

    private boolean allowingSubdomainsDl;

    private Integer superInstitutionId;

    private List<MultilingualContentDTO> superInstitutionName;
}
