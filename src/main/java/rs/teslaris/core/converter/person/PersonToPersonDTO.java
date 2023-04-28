package rs.teslaris.core.converter.person;

import java.util.ArrayList;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonResponseDto;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.model.person.Person;

@Component
public class PersonToPersonDTO {

    public PersonResponseDto toDTO(Person person) {
        var otherNames = new ArrayList<PersonNameDTO>();
        var biography = new ArrayList<MultilingualContentDTO>();
        var keyword = new ArrayList<MultilingualContentDTO>();

        for (var otherName : person.getOtherNames()) {
            otherNames.add(new PersonNameDTO(otherName.getFirstname(), otherName.getOtherName(),
                otherName.getLastname(), otherName.getDateFrom(), otherName.getDateTo()));
        }

        for (var bio : person.getBiography()) {
            biography.add(new MultilingualContentDTO(bio.getLanguage().getId(), bio.getContent(),
                bio.getPriority()));
        }

        for (var keyw : person.getKeyword()) {
            biography.add(new MultilingualContentDTO(keyw.getLanguage().getId(), keyw.getContent(),
                keyw.getPriority()));
        }

        return new PersonResponseDto(
            new PersonNameDTO(person.getName().getFirstname(), person.getName().getOtherName(),
                person.getName().getLastname(), person.getName().getDateFrom(),
                person.getName().getDateTo()), otherNames,
            new PersonalInfoDTO(person.getPersonalInfo()
                .getLocalBirthDate(), person.getPersonalInfo().getPlaceOfBrith(),
                person.getPersonalInfo()
                    .getSex(), new PostalAddressDTO(),
                new ContactDTO(person.getPersonalInfo().getContact().getContactEmail(),
                    person.getPersonalInfo().getContact().getPhoneNumber()), person.getApvnt(),
                person.getMnid(), person.getOrcid(), person.getScopusAuthorId()), biography,
            keyword,
            person.getApproveStatus());
    }
}
