package rs.teslaris.core.model.person;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PersonalInfo {

    @Column(name = "birth_date")
    private LocalDate localBirthDate;

    @Column(name = "place_of_birth")
    private String placeOfBrith;

    @Column(name = "sex")
    private Sex sex;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "postalNumber", column = @Column(name = "professional_postal_number"))
    })
    @AssociationOverrides({
        @AssociationOverride(
            name = "country",
            joinColumns = @JoinColumn(name = "professional_country_id")
        ),
        @AssociationOverride(
            name = "streetAndNumber",
            joinTable = @JoinTable(
                name = "person_prof_street",
                joinColumns = @JoinColumn(name = "person_id"),
                inverseJoinColumns = @JoinColumn(name = "mlc_id"))),
        @AssociationOverride(
            name = "city",
            joinTable = @JoinTable(
                name = "person_prof_city",
                joinColumns = @JoinColumn(name = "person_id"),
                inverseJoinColumns = @JoinColumn(name = "mlc_id"))),
        @AssociationOverride(
            name = "state",
            joinTable = @JoinTable(
                name = "person_prof_state",
                joinColumns = @JoinColumn(name = "person_id"),
                inverseJoinColumns = @JoinColumn(name = "mlc_id")
            )
        )
    })
    private PostalAddress professionalPostalAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "postalNumber", column = @Column(name = "private_postal_number"))
    })
    @AssociationOverrides({
        @AssociationOverride(
            name = "country",
            joinColumns = @JoinColumn(name = "private_country_id")),
        @AssociationOverride(
            name = "streetAndNumber",
            joinTable = @JoinTable(
                name = "person_priv_street",
                joinColumns = @JoinColumn(name = "person_id"),
                inverseJoinColumns = @JoinColumn(name = "mlc_id"))),
        @AssociationOverride(
            name = "city",
            joinTable = @JoinTable(
                name = "person_priv_city",
                joinColumns = @JoinColumn(name = "person_id"),
                inverseJoinColumns = @JoinColumn(name = "mlc_id"))),
        @AssociationOverride(
            name = "state",
            joinTable = @JoinTable(
                name = "person_priv_state",
                joinColumns = @JoinColumn(name = "person_id"),
                inverseJoinColumns = @JoinColumn(name = "mlc_id")))
    })
    private PostalAddress privatePostalAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "contactEmail", column = @Column(name = "professional_contact_email")),
        @AttributeOverride(name = "phoneNumber", column = @Column(name = "professional_phone_number")),
        @AttributeOverride(name = "faxNumber", column = @Column(name = "professional_fax_number")),
        @AttributeOverride(name = "mobilePhoneNumber", column = @Column(name = "professional_mobile_phone_number"))
    })
    private Contact professionalContact;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "contactEmail", column = @Column(name = "private_contact_email")),
        @AttributeOverride(name = "phoneNumber", column = @Column(name = "private_phone_number")),
        @AttributeOverride(name = "faxNumber", column = @Column(name = "private_fax_number")),
        @AttributeOverride(name = "mobilePhoneNumber", column = @Column(name = "private_mobile_phone_number"))
    })
    private Contact privateContact;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "uris")
    private Set<String> uris = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> displayTitle = new HashSet<>();
}
