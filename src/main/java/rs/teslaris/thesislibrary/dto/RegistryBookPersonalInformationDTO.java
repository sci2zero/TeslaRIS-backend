package rs.teslaris.thesislibrary.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.person.PersonNameDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistryBookPersonalInformationDTO {

    private PersonNameDTO authorName;

    private LocalDate localBirthDate;

    private String placeOfBrith;

    private String municipalityOfBrith;

    private Integer countryOfBirthId;

    private String fatherName;

    private String fatherSurname;

    private String motherName;

    private String motherSurname;

    private String guardianNameAndSurname;
}

