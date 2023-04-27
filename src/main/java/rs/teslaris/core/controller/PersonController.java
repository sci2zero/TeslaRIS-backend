package rs.teslaris.core.controller;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.PersonEditCheck;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.ContactDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonResponseDto;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.dto.person.PostalAddressDTO;
import rs.teslaris.core.service.PersonService;

@Validated
@RestController
@RequestMapping("/api/person")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @PostMapping("/{personId}")
    public PersonResponseDto readPersonWithBasicInfo(@PathVariable Integer personId) {
        var person = personService.readPersonWithBasicInfo(personId);
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
                    .getSex(), new PostalAddressDTO(), new ContactDTO(), person.getApvnt(),
                person.getMnid(), person.getOrcid(), person.getScopusAuthorId()), biography,
            keyword,
            person.getApproveStatus());
    }

    @PostMapping("/basic")
    @PreAuthorize("hasAuthority('REGISTER_PERSON')")
    public BasicPersonDTO createPersonWithBasicInfo(@RequestBody @Valid BasicPersonDTO person) {
        var createdPerson = personService.createPersonWithBasicInfo(person);
        person.setId(createdPerson.getId());
        return person;
    }

    @PatchMapping("/name/{personId}/{personNameId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PersonEditCheck
    public void setPersonMainName(@PathVariable Integer personId,
                                  @PathVariable Integer personNameId) {
        personService.setPersonMainName(personNameId, personId);
    }

    @PatchMapping("/other-names/{personId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PersonEditCheck
    public void setPersonOtherNames(@RequestBody @Valid List<PersonNameDTO> personNames,
                                    @PathVariable Integer personId) {
        personService.setPersonOtherNames(personNames, personId);
    }

    @PatchMapping("/biography/{personId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PersonEditCheck
    public void setPersonBiography(@RequestBody @Valid List<MultilingualContentDTO> biography,
                                   @PathVariable Integer personId) {
        personService.setPersonBiography(biography, personId);
    }

    @PatchMapping("/keyword/{personId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PersonEditCheck
    public void setPersonKeyword(@RequestBody @Valid List<MultilingualContentDTO> keyword,
                                 @PathVariable Integer personId) {
        personService.setPersonKeyword(keyword, personId);
    }


    @PatchMapping("/personal-info/{personId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PersonEditCheck
    public void updatePersonalInfo(@RequestBody @Valid PersonalInfoDTO personalInfo,
                                   @PathVariable Integer personId) {
        personService.updatePersonalInfo(personalInfo, personId);
    }

}
