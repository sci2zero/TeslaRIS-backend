package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.person.PersonalInfoDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedPersonsDTO {

    private PersonalInfoDTO leftPerson;

    private PersonalInfoDTO rightPerson;
}
