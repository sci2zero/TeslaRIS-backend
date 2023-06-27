package rs.teslaris.core.converter.person;

import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.person.PersonName;

public class PersonNameConverter {

    public static PersonNameDTO toDTO(PersonName personName) {
        return new PersonNameDTO(personName.getFirstname(), personName.getOtherName(),
            personName.getLastname(), personName.getDateFrom(), personName.getDateTo());
    }
}
