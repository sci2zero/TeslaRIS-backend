package rs.teslaris.core.controller;

import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.converter.involvement.InvolvementToInvolvementDTO;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.MembershipDTO;
import rs.teslaris.core.service.InvolvementService;

@RestController
@RequestMapping("api/involvement")
@RequiredArgsConstructor
public class InvolvementController {

    private final InvolvementService involvementService;

    private final InvolvementToInvolvementDTO involvementToInvolvementDTO;

    @PostMapping("/education/{personId}")
    public EducationDTO addEducation(@RequestBody @Valid EducationDTO education,
                                     @PathVariable Integer personId) {
        var newEducation = involvementService.addEducation(personId, education);
        return involvementToInvolvementDTO.toDTO(newEducation);
    }

    @PostMapping("/membership/{personId}")
    public MembershipDTO addMembership(@RequestBody @Valid MembershipDTO membership,
                                       @PathVariable Integer personId) {
        var newMembership = involvementService.addMembership(personId, membership);
        return involvementToInvolvementDTO.toDTO(newMembership);
    }

    @PostMapping("/employment/{personId}")
    public EmploymentDTO addEmployment(@RequestBody @Valid EmploymentDTO employment,
                                       @PathVariable Integer personId) {
        var newEmployment = involvementService.addEmployment(personId, employment);
        return involvementToInvolvementDTO.toDTO(newEmployment);
    }

    @PutMapping("/education/{involvementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateEducation(@RequestBody @Valid EducationDTO education,
                                @PathVariable Integer involvementId) {
        involvementService.updateEducation(involvementId, education);
    }

    @PutMapping("/membership/{involvementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMembership(@RequestBody @Valid MembershipDTO membership,
                                 @PathVariable Integer involvementId) {
        involvementService.updateMembership(involvementId, membership);
    }

    @PutMapping("/employment/{involvementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateEmployment(@RequestBody @Valid EmploymentDTO employment,
                                 @PathVariable Integer involvementId) {
        involvementService.updateEmployment(involvementId, employment);
    }

    @DeleteMapping("/{involvementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvolvement(@PathVariable Integer involvementId) {
        involvementService.deleteInvolvement(involvementId);
    }
}
