package rs.teslaris.core.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.service.PersonService;

@RestController
@RequestMapping("/api/person")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @PostMapping("/basic")
    @PreAuthorize("hasAuthority('REGISTER_PERSON')")
    public BasicPersonDTO createPersonWithBasicInfo(@RequestBody BasicPersonDTO person) {
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
    public void setPersonOtherNames(@RequestBody List<PersonNameDTO> personNames,
                                    @PathVariable Integer personId) {
        personService.setPersonOtherNames(personNames, personId);
    }

    @PatchMapping("/biography/{personId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PersonEditCheck
    public void setPersonBiography(@RequestBody List<MultilingualContentDTO> biography,
                                   @PathVariable Integer personId) {
        personService.setPersonBiography(biography, personId);
    }

    @PatchMapping("/keyword/{personId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PersonEditCheck
    public void setPersonKeyword(@RequestBody List<MultilingualContentDTO> keyword,
                                 @PathVariable Integer personId) {
        personService.setPersonKeyword(keyword, personId);
    }


    @PatchMapping("/personal-info/{personId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PersonEditCheck
    public void updatePersonalInfo(@RequestBody PersonalInfoDTO personalInfo,
                                   @PathVariable Integer personId) {
        personService.updatePersonalInfo(personalInfo, personId);
    }

}
