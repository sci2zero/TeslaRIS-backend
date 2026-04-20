package rs.teslaris.core.controller.identifier;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.service.interfaces.identifier.EntityIdentifierService;

@RestController
@RequestMapping("/api/entity-identifier")
@RequiredArgsConstructor
@Traceable
public class EntityIdentifierController {

    private final EntityIdentifierService entityIdentifierService;


    @DeleteMapping("/{entityIdentifierId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_ENTITY_IDENTIFIER')")
//    @EntityIdentifierEditCheck
    public void deleteEntityIdentifier(@PathVariable Integer entityIdentifierId) {
        entityIdentifierService.deleteEntityIdentifier(entityIdentifierId);
    }
}
