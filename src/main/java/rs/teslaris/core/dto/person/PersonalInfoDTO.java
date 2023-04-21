package rs.teslaris.core.dto.person;

import java.time.LocalDate;
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

    private LocalDate localBirthDate;

    private String placeOfBrith;

    private Sex sex;

    private PostalAddressDTO postalAddress;

    private ContactDTO contact;

    private String apvnt;

    private String mnid;

    private String orcid;

    private String scopusAuthorId;

}
