package rs.teslaris.core.model.person;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
    private PostalAddress postalAddress;

    @Embedded
    private Contact contact;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> uris = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MultiLingualContent> displayTitle = new HashSet<>();
}
