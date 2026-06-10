package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PostalAddress;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "affiliation_statements")
@SQLRestriction("deleted=false")
public class AffiliationStatement extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> displayAffiliationStatement = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "display_person_name_id")
    private PersonName displayPersonName;

    @Embedded
    private PostalAddress postalAddress;

    @Embedded
    private Contact contact;


    public AffiliationStatement(AffiliationStatement other) {
        if (Objects.isNull(other)) {
            return;
        }

        this.displayAffiliationStatement =
            other.getDisplayAffiliationStatement().stream()
                .map(mlc -> new MultiLingualContent(
                    mlc.getLanguage(),
                    mlc.getContent(),
                    mlc.getPriority()
                ))
                .collect(Collectors.toCollection(HashSet::new));

        this.displayPersonName = Objects.isNull(other.getDisplayPersonName())
            ? new PersonName()
            : new PersonName(
            other.getDisplayPersonName().getFirstname(),
            other.getDisplayPersonName().getOtherName(),
            other.getDisplayPersonName().getLastname(),
            other.getDisplayPersonName().getDateFrom(),
            other.getDisplayPersonName().getDateTo(),
            other.getDisplayPersonName().getNameType()
        );

        this.postalAddress = Objects.isNull(other.getPostalAddress())
            ? new PostalAddress()
            : new PostalAddress(
            other.getPostalAddress().getCountry(),
            other.getPostalAddress().getStreetAndNumber().stream()
                .map(mlc -> new MultiLingualContent(
                    mlc.getLanguage(),
                    mlc.getContent(),
                    mlc.getPriority()
                ))
                .collect(Collectors.toCollection(HashSet::new)),
            other.getPostalAddress().getCity().stream()
                .map(mlc -> new MultiLingualContent(
                    mlc.getLanguage(),
                    mlc.getContent(),
                    mlc.getPriority()
                ))
                .collect(Collectors.toCollection(HashSet::new)),
            other.getPostalAddress().getState().stream()
                .map(mlc -> new MultiLingualContent(
                    mlc.getLanguage(),
                    mlc.getContent(),
                    mlc.getPriority()
                ))
                .collect(Collectors.toCollection(HashSet::new)),
            other.getPostalAddress().getPostalNumber()
        );

        this.contact = Objects.isNull(other.getContact())
            ? new Contact()
            : new Contact(
            other.getContact().getContactEmail(),
            other.getContact().getPhoneNumber(),
            other.getContact().getFaxNumber(),
            other.getContact().getMobilePhoneNumber()
        );
    }
}
