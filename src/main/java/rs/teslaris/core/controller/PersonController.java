package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PersonEditCheck;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.dto.person.PersonUserResponseDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.dto.person.involvement.InvolvementDTO;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.search.StringUtil;

@Validated
@RestController
@RequestMapping("/api/person")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    private final JwtUtil tokenUtil;


    @GetMapping("/{personId}/can-edit")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    public boolean canEditPerson() {
        return true;
    }

    @GetMapping
    public Page<PersonIndex> findAll(Pageable pageable) {
        return personService.findAllIndex(pageable);
    }

    @GetMapping("/count")
    public Long countAll(Pageable pageable) {
        return personService.getResearcherCount();
    }

    @GetMapping("/{personId}")
    public PersonResponseDTO readPersonWithBasicInfo(@PathVariable Integer personId) {
        return personService.readPersonWithBasicInfo(personId);
    }

    @GetMapping("/{personId}/person-user")
    public PersonUserResponseDTO readPersonWithUser(@PathVariable Integer personId) {
        return personService.readPersonWithUser(personId);
    }

    @GetMapping("/for-user")
    public Integer getPersonIdForUser(@RequestHeader("Authorization") String bearerToken) {
        return personService.getPersonIdForUserId(
            tokenUtil.extractUserIdFromToken(bearerToken.split(" ")[1]));
    }

    @GetMapping("/simple-search")
    public Page<PersonIndex> simpleSearch(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        Pageable pageable) {
        StringUtil.sanitizeTokens(tokens);
        return personService.findPeopleByNameAndEmployment(tokens,
            pageable);
    }

    @GetMapping("/scopus-author")
    public PersonIndex findByScopusId(@RequestParam("scopusId") String scopusId) {
        return personService.findPersonByScopusAuthorId(scopusId);
    }

    @GetMapping("/employed-at/{organisationUnitId}")
    public Page<PersonIndex> findEmployeesForInstitution(@PathVariable Integer organisationUnitId,
                                                         Pageable pageable) {
        return personService.findPeopleForOrganisationUnit(organisationUnitId, pageable);
    }

    @GetMapping("/advanced-search")
    public Page<PersonIndex> advancedSearch(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        Pageable pageable) {
        return personService.advancedSearch(tokens, pageable);
    }

    @PostMapping("/basic")
    @PreAuthorize("hasAuthority('REGISTER_PERSON')")
    @Idempotent
    public BasicPersonDTO createPersonWithBasicInfo(@RequestBody @Valid BasicPersonDTO person) {
        var createdPerson = personService.createPersonWithBasicInfo(person, true);
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

    @PatchMapping("/keywords/{personId}")
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

    @PatchMapping("/approve/{personId}")
    @PreAuthorize("hasAuthority('APPROVE_PERSON')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approvePerson(@RequestParam Boolean approve,
                              @PathVariable Integer personId) {
        personService.approvePerson(personId, approve);
    }

    @DeleteMapping("/{personId}")
    @PreAuthorize("hasAuthority('DELETE_PERSON')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePerson(@PathVariable Integer personId) {
        personService.deletePerson(personId);
    }

    @GetMapping("/{personId}/latest-involvement")
    public InvolvementDTO getLatestInvolvementOU(@PathVariable Integer personId) {
        return personService.getLatestResearcherInvolvement(personId);
    }
}
