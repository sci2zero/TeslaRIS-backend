package rs.teslaris.thesislibrary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.person.Contact;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class RegistryBookContactInformation {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "residence_country_id")
    private Country residenceCountry;

    @Column(name = "street_and_number")
    private String streetAndNumber;

    @Column(name = "place")
    private String place;

    @Column(name = "municipality")
    private String municipality;

    @Column(name = "postal_code")
    private String postalCode;

    @Embedded
    private Contact contact;
}
