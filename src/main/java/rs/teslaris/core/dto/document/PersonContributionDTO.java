package rs.teslaris.core.dto.document;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PostalAddressDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonContributionDTO {

    private Integer id;

    private Integer personId;

    @NotNull(message = "You have to provide a contribution description.")
    private List<MultilingualContentDTO> contributionDescription;

    @NotNull(message = "You have to specify an order number.")
    @Positive(message = "Order number must be a positive number.")
    private Integer orderNumber;

    private List<Integer> institutionIds;

    @NotNull(message = "You have to provide a display affiliation statement.")
    private List<MultilingualContentDTO> displayAffiliationStatement;

    private PersonNameDTO personName;

    private PostalAddressDTO postalAddress;

    private ContactDTO contact;

    // only for responses
    private List<List<MultilingualContentDTO>> displayInstitutionNames = new ArrayList<>();
}
