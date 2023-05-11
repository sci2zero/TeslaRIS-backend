package rs.teslaris.core.dto.person;

import java.time.LocalDate;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.person.Sex;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonalInfoDTO {

    @NotNull(message = "You have to provide a local bitrth date.")
    private LocalDate localBirthDate;

    private String placeOfBrith;

    @NotNull(message = "You must provide a person sex.")
    private Sex sex;

    @Valid
    private PostalAddressDTO postalAddress;

    @NotNull(message = "You have to provide a contact info.")
    @Valid
    private ContactDTO contact;

    private String apvnt;

    private String mnid;

    private String orcid;

    private String scopusAuthorId;

}
