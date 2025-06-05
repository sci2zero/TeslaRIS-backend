package rs.teslaris.thesislibrary.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PostalAddressDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhdThesisPrePopulatedDataDTO {

    private PersonNameDTO personName;

    private LocalDate localBirthDate;

    private String placeOfBirth;

    private PostalAddressDTO postalAddress;

    private ContactDTO contact;

    private String institutionName;

    private Integer institutionId;

    private String place;

    private String title;

    private String mentor;

    private String commission;

    private LocalDate defenceDate;
}
