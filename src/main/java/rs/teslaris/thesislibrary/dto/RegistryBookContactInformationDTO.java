package rs.teslaris.thesislibrary.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.person.ContactDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistryBookContactInformationDTO {

    private Integer residenceCountryId;

    private String streetAndNumber;

    private String place;

    private String municipality;

    private String postalCode;

    private ContactDTO contact;
}
