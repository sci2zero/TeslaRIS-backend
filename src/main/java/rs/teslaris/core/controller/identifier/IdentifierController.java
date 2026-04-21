package rs.teslaris.core.controller.identifier;

import java.util.List;
import lombok.RequiredArgsConstructor;
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
import rs.teslaris.core.converter.identifier.IdentifierConverter;
import rs.teslaris.core.dto.identifier.IdentifierDTO;
import rs.teslaris.core.dto.identifier.IdentifierResponseDTO;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;

@RestController
@RequestMapping("api/identifier")
@RequiredArgsConstructor
@Traceable
public class IdentifierController {

    private final IdentifierService identifierService;


    @GetMapping
    public Page<IdentifierResponseDTO> readIdentifiers(@RequestParam("lang") String language,
                                                       Pageable pageable) {
        return identifierService.readAllIdentifiers(pageable, language.toUpperCase());
    }

    @GetMapping("/{identifierId}")
    public IdentifierResponseDTO readIdentifier(@PathVariable Integer identifierId) {
        return identifierService.readIdentifierById(identifierId);
    }

    @GetMapping("/access-level/{identifierId}")
    @PreAuthorize("hasAuthority('EDIT_IDENTIFIERS')")
    public String readIdentifierAccessLevel(@PathVariable Integer identifierId) {
        return identifierService.readIdentifierAccessLevel(identifierId).name();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_IDENTIFIERS')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public IdentifierResponseDTO createIdentifier(@RequestBody IdentifierDTO identifierDTO) {
        var createdIdentifier = identifierService.createIdentifier(identifierDTO);

        return IdentifierConverter.toDTO(createdIdentifier);
    }

    @PutMapping("/{identifierId}")
    @PreAuthorize("hasAuthority('EDIT_IDENTIFIERS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateIdentifier(@RequestBody IdentifierDTO identifierDTO,
                                 @PathVariable Integer identifierId) {
        identifierService.updateIdentifier(identifierId,
            identifierDTO);
    }

    @DeleteMapping("/{identifierId}")
    @PreAuthorize("hasAuthority('EDIT_IDENTIFIERS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIdentifier(@PathVariable Integer identifierId) {
        identifierService.deleteIdentifier(identifierId);
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('EDIT_ENTITY_IDENTIFIER')")
    public List<IdentifierResponseDTO> getIdentifiersApplicableToEntity(
        @RequestParam("applicableType") List<ApplicableEntityType> applicableEntityTypes) {
        return identifierService.getIdentifiersApplicableToEntity(applicableEntityTypes);
    }
}

