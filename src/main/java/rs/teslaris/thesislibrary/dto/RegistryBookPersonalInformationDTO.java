package rs.teslaris.thesislibrary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @Valid
    @NotNull(message = "You have to provide author's name.")
    private PersonNameDTO authorName;

    @NotNull(message = "You have to provide author's birthdate.")
    private LocalDate localBirthDate;

    @NotBlank(message = "You have to provide author's place of birth.")
    private String placeOfBrith;

    private String municipalityOfBrith;

    @NotNull(message = "You have to provide author's country of birth.")
    @Positive
    private Integer countryOfBirthId;

    @NotNull
    private String fatherName;

    private String fatherSurname;

    @NotNull
    private String motherName;

    private String motherSurname;

    @NotNull
    private String guardianNameAndSurname;
}

