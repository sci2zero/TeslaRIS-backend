package rs.teslaris.core.model.person;

import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
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

    @Column(name = "birth_date", nullable = false)
    LocalDate localBirthDate;

    @Column(name = "place_of_birth", nullable = false)
    String placeOfBrith;

    @Column(name = "sex", nullable = false)
    Sex sex;

    PostalAddress postalAddress;

    @Embedded
    Contact contact;
}
