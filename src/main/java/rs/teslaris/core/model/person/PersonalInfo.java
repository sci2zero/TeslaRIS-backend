package rs.teslaris.core.model.person;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
