package rs.teslaris.migrator.converter.hydrator;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.migrator.util.MigrationRunStartedEvent;

/**
 * Matches a project team member to an existing person by Ciência ID, then ORCID (MAP-028).
 */
@Component
@RequiredArgsConstructor
public class HydratorPersonResolver {

    static final String NO_IDENTIFIER = "person has no identifier";

    static final String NOT_FOUND = "identifier not found";

    static final String AMBIGUOUS = "ambiguous identifier";

    private final PersonService personService;

    // Empty value = looked up, not found; ambiguous identifiers are not cached
    private final Map<String, Optional<Integer>> personIds = new ConcurrentHashMap<>();


    public Match resolve(String cienciaId, String orcid) {
        if (isBlank(cienciaId) && isBlank(orcid)) {
            return new Match(null, NO_IDENTIFIER);
        }

        var ambiguous = false;
        for (var identifier : new String[] {cienciaId, orcid}) {
            if (isBlank(identifier)) {
                continue;
            }

            try {
                var personId = personIds.computeIfAbsent(identifier.trim(), this::lookUp);
                if (personId.isPresent()) {
                    return new Match(personId.get(), null);
                }
            } catch (IncorrectResultSizeDataAccessException e) {
                ambiguous = true;
            }
        }

        return new Match(null, ambiguous ? AMBIGUOUS : NOT_FOUND);
    }

    @EventListener(MigrationRunStartedEvent.class)
    public void clearCache() {
        personIds.clear();
    }

    private Optional<Integer> lookUp(String identifier) {
        return personService.findPersonByIdentifier(identifier).map(Person::getId);
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }

    public record Match(Integer personId, String reason) {

        public boolean found() {
            return Objects.nonNull(personId);
        }
    }
}
