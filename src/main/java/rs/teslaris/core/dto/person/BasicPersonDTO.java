package rs.teslaris.core.dto.person;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.person.EmploymentPosition;
import rs.teslaris.core.model.person.Sex;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BasicPersonDTO implements PersonIdentifierable {

    private Integer id;

    private Integer oldId;

    @Valid
    private PersonNameDTO personName;

    private String contactEmail;

    private Sex sex;

    private LocalDate localBirthDate;

    private String phoneNumber;

    private String apvnt;

    @JsonProperty("eCrisId")
    private String eCrisId;

    @JsonProperty("eNaukaId")
    private String eNaukaId;

    private String orcid;

    private String scopusAuthorId;

    @Positive(message = "Organisation unit id must be a positive number.")
    private Integer organisationUnitId;

    private EmploymentPosition employmentPosition;

    private List<MultilingualContentDTO> displayTitle = new ArrayList<>();

    private String openAlexId;

    private String webOfScienceId;
}
