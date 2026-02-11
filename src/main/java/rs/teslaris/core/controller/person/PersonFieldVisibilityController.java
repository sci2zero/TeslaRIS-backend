package rs.teslaris.core.controller.person;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.PersonEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.person.PersonFieldVisibilityDTO;
import rs.teslaris.core.service.interfaces.person.PersonFieldVisibilityService;

@RestController
@RequestMapping("/api/person-field-visibility")
@RequiredArgsConstructor
@Traceable
public class PersonFieldVisibilityController {

    private final PersonFieldVisibilityService personFieldVisibilityService;


    @GetMapping("/{personId}")
    @PreAuthorize("hasAuthority('SET_PERSON_FIELD_VISIBILITY')")
    @PersonEditCheck
    public PersonFieldVisibilityDTO readPublicFieldVisibilityConfiguration(
        @PathVariable Integer personId) {
        return personFieldVisibilityService.readPublicFieldConfiguration(personId);
    }

    @PatchMapping("/{personId}")
    @PreAuthorize("hasAuthority('SET_PERSON_FIELD_VISIBILITY')")
    @PersonEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void savePublicFieldVisibilityConfiguration(@PathVariable Integer personId,
                                                       @RequestBody @Valid
                                                       PersonFieldVisibilityDTO configurationDTO) {
        personFieldVisibilityService.savePublicFieldConfiguration(personId, configurationDTO);
    }
}
