package rs.teslaris.core.dto.institution;


import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.GeoLocationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.OrganisationUnitSector;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnitRequestDTO {

    @NotNull(message = "You have to provide organisation name.")
    private List<MultilingualContentDTO> name;

    @NotNull(message = "You have to provide name abbreviation.")
    private List<MultilingualContentDTO> nameAbbreviation;

    @NotNull(message = "You have to provide organisation description.")
    private List<MultilingualContentDTO> description;

    @NotNull(message = "You have to provide organisation keywords.")
    private List<MultilingualContentDTO> keyword;

    @NotNull(message = "You have to provide research areas.")
    private List<Integer> researchAreasId;

    private GeoLocationDTO location;

    private ContactDTO contact;

    private String scopusAfid;

    private String openAlexId;

    private String ror;

    private String ringgold;

    private String fundref;

    private String isni;

    private String fctId;

    private String taxNumber;

    private Integer oldId;

    private Set<String> uris;

    private Set<ThesisType> allowedThesisTypes;

    private boolean clientInstitutionCris;

    private String institutionEmailDomainCris;

    private boolean validatingEmailDomainCris;

    private boolean allowingSubdomainsCris;

    private boolean clientInstitutionDl;

    private String institutionEmailDomainDl;

    private boolean validatingEmailDomainDl;

    private boolean allowingSubdomainsDl;

    private boolean legalEntity;

    private OrganisationUnitSector sector;

    private Boolean startup;

    private LocalDate dateEstablished;

    private PostalAddressDTO postalAddress;
}
