package rs.teslaris.core.converter.person;

import java.util.Objects;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PersonNameType;
import rs.teslaris.core.util.language.SerbianTransliteration;

public class PersonNameConverter {

    public static PersonNameDTO toDTO(PersonName personName) {
        return new PersonNameDTO(personName.getId(), personName.getFirstname(),
            personName.getOtherName(), personName.getLastname(), personName.getDateFrom(),
            personName.getDateTo(),
            Objects.requireNonNullElse(personName.getNameType(), PersonNameType.DISPLAY_NAME));
    }

    public static PersonNameDTO toTransliteratedDTO(PersonName personName) {
        return new PersonNameDTO(personName.getId(),
            SerbianTransliteration.toCyrillic(personName.getFirstname()),
            SerbianTransliteration.toCyrillic(personName.getOtherName()),
            SerbianTransliteration.toCyrillic(personName.getLastname()), personName.getDateFrom(),
            personName.getDateTo(),
            Objects.requireNonNullElse(personName.getNameType(), PersonNameType.DISPLAY_NAME));
    }
}
