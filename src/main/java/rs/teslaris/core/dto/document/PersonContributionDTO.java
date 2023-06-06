package rs.teslaris.core.dto.document;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
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

    private Integer personId;

    @Valid
    private List<MultilingualContentDTO> contributionDescription;

    @NotNull(message = "You have to specify an order number.")
    @Positive(message = "Order number must be a positive number.")
    private Integer orderNumber;

    @NotNull(message = "You have to provide a list of institution Ids.")
    private List<Integer> institutionIds;

    @Valid
    @NotNull(message = "You have to provide a display affiliation statement.")
    private List<MultilingualContentDTO> displayAffiliationStatement;

    @NotNull(message = "You have to provide a person name.")
    private PersonNameDTO personName;

    @Valid
    @NotNull(message = "You have to provide a person postal address.")
    private PostalAddressDTO postalAddress;

    @NotNull(message = "You have to provide a person contact info.")
    private ContactDTO contact;
}
