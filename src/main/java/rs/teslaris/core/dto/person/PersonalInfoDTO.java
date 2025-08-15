package rs.teslaris.core.dto.person;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

    @NotNull(message = "You have to provide a contact info.")
    @Valid
    private ContactDTO contact;

    private String apvnt;

    @JsonProperty("eCrisId")
    private String eCrisId;

    @JsonProperty("eNaukaId")
    private String eNaukaId;

    private String orcid;

    private String scopusAuthorId;

    private String openAlexId;

    private String webOfScienceResearcherId;

    private Set<String> uris;

    private List<MultilingualContentDTO> displayTitle = new ArrayList<>();
}
