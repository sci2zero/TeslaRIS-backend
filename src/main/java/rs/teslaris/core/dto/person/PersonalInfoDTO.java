package rs.teslaris.core.dto.person;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.person.Sex;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonalInfoDTO implements PersonIdentifierable {

    private LocalDate localBirthDate;

    private String placeOfBirth;

    private Sex sex;

    @Valid
    private PostalAddressDTO postalAddress;

    @Valid
    private PostalAddressDTO privatePostalAddress;

    @NotNull(message = "You have to provide a contact info.")
    @Valid
    private ContactDTO contact;

    @Valid
    private ContactDTO privateContact;

    private String apvnt;

    @JsonProperty("eCrisId")
    private String eCrisId;

    @JsonProperty("eNaukaId")
    private String eNaukaId;

    private String orcid;

    private String scopusAuthorId;

    private String openAlexId;

    private String webOfScienceResearcherId;

    private String nationalScienceId;

    private String scholarId;

    private String authenticusId;

    private String lattesId;

    private Set<String> uris;

    private List<MultilingualContentDTO> displayTitle = new ArrayList<>();

    // used only for responses

    private Integer id;


    public PersonalInfoDTO(PersonalInfoDTO other) {
        this.localBirthDate = other.localBirthDate;
        this.placeOfBirth = other.placeOfBirth;
        this.sex = other.sex;
        this.postalAddress =
            other.postalAddress != null ? new PostalAddressDTO(other.postalAddress) : null;
        this.privatePostalAddress =
            other.privatePostalAddress != null ? new PostalAddressDTO(other.privatePostalAddress) :
                null;
        this.contact = other.contact != null ? new ContactDTO(other.contact) : null;
        this.privateContact =
            other.privateContact != null ? new ContactDTO(other.privateContact) : null;
        this.apvnt = other.apvnt;
        this.eCrisId = other.eCrisId;
        this.eNaukaId = other.eNaukaId;
        this.orcid = other.orcid;
        this.scopusAuthorId = other.scopusAuthorId;
        this.openAlexId = other.openAlexId;
        this.webOfScienceResearcherId = other.webOfScienceResearcherId;
        this.nationalScienceId = other.nationalScienceId;
        this.scholarId = other.scholarId;
        this.authenticusId = other.authenticusId;
        this.lattesId = other.lattesId;
        this.uris = other.uris != null ? new HashSet<>(other.uris) : null;
        this.displayTitle = other.displayTitle != null
            ? other.displayTitle.stream().map(MultilingualContentDTO::new)
            .collect(Collectors.toList())
            : new ArrayList<>();
        this.id = other.id;
    }
}
