package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PersonEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.commontypes.ProfilePhotoOrLogoDTO;
import rs.teslaris.core.dto.person.BasicPersonDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;
import rs.teslaris.core.dto.person.PersonUserResponseDTO;
import rs.teslaris.core.dto.person.PersonalInfoDTO;
import rs.teslaris.core.dto.person.involvement.InvolvementDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.Triple;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.search.StringUtil;

@Validated
@RestController
@RequestMapping("/api/person")
@RequiredArgsConstructor
@Traceable
public class PersonController {

    private final PersonService personService;

    private final JwtUtil tokenUtil;

    private final DeduplicationService deduplicationService;


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

    @GetMapping("/old-id/{personOldId}")
    public PersonResponseDTO readPersonWithBasicInfoForOldId(@PathVariable Integer personOldId) {
        return personService.readPersonWithBasicInfoForOldId(personOldId);
    }

    @GetMapping("/{personId}/person-user")
    public PersonUserResponseDTO readPersonWithUser(@PathVariable Integer personId) {
        return personService.readPersonWithUser(personId);
    }

    @GetMapping("/for-user")
    public Integer getPersonIdForUser(@RequestHeader("Authorization") String bearerToken) {
        return personService.getPersonIdForUserId(
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/simple-search")
    public Page<PersonIndex> simpleSearch(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        @RequestParam(required = false, defaultValue = "false") boolean strict,
        @RequestParam(required = false, defaultValue = "0") Integer institutionId,
        Pageable pageable) {
        StringUtil.sanitizeTokens(tokens);
        return personService.findPeopleByNameAndEmployment(tokens,
            pageable, strict, institutionId);
    }

    @GetMapping("/scopus-author/{scopusId}")
    public PersonIndex findByScopusId(@PathVariable("scopusId") String scopusId) {
        return personService.findPersonByScopusAuthorId(scopusId);
    }

    @GetMapping("/employed-at/{organisationUnitId}")
    public Page<PersonIndex> findEmployeesForInstitution(@PathVariable Integer organisationUnitId,
                                                         @RequestParam Boolean fetchAlumni,
                                                         Pageable pageable) {
        return personService.findPeopleForOrganisationUnit(organisationUnitId, pageable,
            fetchAlumni);
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
    @PersonEditCheck("CREATE")
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

    @PutMapping("/name/{personId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PersonEditCheck
    public void updatePersonMainName(@PathVariable Integer personId,
                                     @RequestBody PersonNameDTO personNameDTO) {
        personService.updatePersonMainName(personId, personNameDTO);
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
        personService.updatePersonalInfo(personId, personalInfo);
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
        deduplicationService.deleteSuggestion(personId, EntityType.PERSON);
    }

    @DeleteMapping("/force/{personId}")
    @PreAuthorize("hasAuthority('FORCE_DELETE_ENTITIES')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forceDeletePerson(@PathVariable Integer personId) {
        personService.forceDeletePerson(personId);
        deduplicationService.deleteSuggestion(personId, EntityType.PERSON);
    }

    @GetMapping("/{personId}/latest-involvement")
    public InvolvementDTO getLatestInvolvementOU(@PathVariable Integer personId) {
        return personService.getLatestResearcherInvolvement(personId);
    }

    @GetMapping("/is-bound/{personId}")
    @PreAuthorize("hasAuthority('MERGE_PERSON_METADATA')")
    public boolean isPersonBoundToAUser(@PathVariable Integer personId) {
        return personService.isPersonBoundToAUser(personId);
    }

    @PatchMapping("/unmanaged/{personId}")
    @PreAuthorize("hasAuthority('SWITCH_ENTITY_TO_UNMANAGED')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void switchPersonToUnmanagedEntity(@PathVariable Integer personId) {
        personService.switchToUnmanagedEntity(personId);
        deduplicationService.deleteSuggestion(personId, EntityType.PERSON);
    }

    @PatchMapping("/profile-image/{personId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @ResponseStatus(HttpStatus.OK)
    @PersonEditCheck
    public String updatePersonProfileImage(
        @ModelAttribute @Valid ProfilePhotoOrLogoDTO profilePhotoDTO,
        @PathVariable Integer personId) throws IOException {
        return personService.setPersonProfileImage(personId, profilePhotoDTO);
    }

    @DeleteMapping("/profile-image/{personId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PersonEditCheck
    public void removePersonProfileImage(@PathVariable Integer personId) {
        personService.removePersonProfileImage(personId);
    }

    @GetMapping("/identifier-usage/{personId}")
    @PreAuthorize("hasAuthority('EDIT_PERSON_INFORMATION')")
    @PersonEditCheck
    public boolean checkIdentifierUsage(@PathVariable Integer personId,
                                        @RequestParam String identifier) {
        return personService.isIdentifierInUse(identifier, personId);
    }

    @GetMapping("/fields")
    public List<Triple<String, List<MultilingualContentDTO>, String>> getSearchFields(
        @RequestParam("export") Boolean onlyExportFields) {
        return personService.getSearchFields(onlyExportFields);
    }
}
