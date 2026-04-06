package rs.teslaris.core.controller.person;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.person.involvement.EmploymentPositionDTO;
import rs.teslaris.core.service.interfaces.person.EmploymentPositionService;

@RestController
@RequestMapping("/api/employment-position")
@RequiredArgsConstructor
@Traceable
public class EmploymentPositionController {

    private final EmploymentPositionService employmentPositionService;


    @GetMapping("/{employmentPositionId}")
    public EmploymentPositionDTO readEmploymentPosition(
        @PathVariable Integer employmentPositionId) {
        return employmentPositionService.readEmploymentPosition(employmentPositionId);
    }

    @GetMapping("/search")
    public Page<EmploymentPositionDTO> searchEmploymentPositions(Pageable pageable,
                                                                 @RequestParam("tokens")
                                                                 List<String> tokens,
                                                                 @RequestParam("lang")
                                                                 String language) {
        return employmentPositionService.searchEmploymentPositions(pageable,
            Strings.join(tokens, ' '),
            language.toUpperCase());
    }

    @GetMapping("/children/{parentId}")
    public List<EmploymentPositionDTO> getChildEmploymentPositions(@PathVariable Integer parentId) {
        return employmentPositionService.getChildEmploymentPositions(parentId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_EMPLOYMENT_POSITIONS')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public EmploymentPositionDTO createEmploymentPosition(
        @RequestBody EmploymentPositionDTO employmentPosition) {
        return employmentPositionService.createEmploymentPosition(employmentPosition);
    }

    @PutMapping("/{employmentPositionId}")
    @PreAuthorize("hasAuthority('EDIT_EMPLOYMENT_POSITIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editEmploymentPosition(@RequestBody EmploymentPositionDTO employmentPosition,
                                       @PathVariable Integer employmentPositionId) {
        employmentPositionService.editEmploymentPosition(employmentPosition, employmentPositionId);
    }

    @DeleteMapping("/{employmentPositionId}")
    @PreAuthorize("hasAuthority('EDIT_EMPLOYMENT_POSITIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmploymentPosition(@PathVariable Integer employmentPositionId) {
        employmentPositionService.deleteEmploymentPosition(employmentPositionId);
    }
}
