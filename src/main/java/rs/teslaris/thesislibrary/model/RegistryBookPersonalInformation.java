package rs.teslaris.thesislibrary.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.person.PersonName;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class RegistryBookPersonalInformation {

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private PersonName authorName;

    @Column(name = "birth_date")
    private LocalDate localBirthDate;

    @Column(name = "place_of_birth")
    private String placeOfBrith;

    @Column(name = "municipality_of_birth")
    private String municipalityOfBrith;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "birth_country_id")
    private Country countryOfBirth;

    @Column(name = "father_name")
    private String fatherName;

    @Column(name = "father_surname")
    private String fatherSurname;

    @Column(name = "mother_name")
    private String motherName;

    @Column(name = "mother_surname")
    private String motherSurname;

    @Column(name = "guardian_name_and_surname")
    private String guardianNameAndSurname;
}
