package rs.teslaris.core.util.identifier;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.identifier.PersonIdentifier;
import rs.teslaris.core.repository.identifier.PersonIdentifierRepository;
import rs.teslaris.core.util.persistence.IdentifierUtil;

/**
 * Person identifiers an anonymous caller may see, ordered by identifier code.
 */
@Component
public class PublicPersonIdentifiers {

    private static final ThreadLocal<Primed> PRIMED = new ThreadLocal<>();

    private static PersonIdentifierRepository personIdentifierRepository;


    @Autowired
    public PublicPersonIdentifiers(PersonIdentifierRepository personIdentifierRepository) {
        PublicPersonIdentifiers.personIdentifierRepository = personIdentifierRepository;
    }

    /**
     * Loads the given persons' identifiers in one query, and the caller must
     * {@link #clear()} in a finally block.
     */
    public static void prime(Collection<Integer> personIds) {
        if (Objects.isNull(personIds) || personIds.isEmpty()) {
            PRIMED.set(new Primed(Set.of(), Map.of()));
            return;
        }

        var covered = Set.copyOf(personIds);

        PRIMED.set(new Primed(covered, personIdentifierRepository
            .findIdentifiersForPersonsAndIdentifierAccessLevel(covered, AccessLevel.OPEN)
            .stream()
            .collect(Collectors.groupingBy(identifier -> identifier.getPerson().getId()))));
    }

    public static void clear() {
        PRIMED.remove();
    }

    public static List<PersonIdentifier> openIdentifiers(Integer personId) {
        if (Objects.isNull(personId)) {
            return List.of();
        }

        var primed = PRIMED.get();

        // A person the batch did not cover still gets looked up, so priming can never hide one.
        if (Objects.nonNull(primed) && primed.covered().contains(personId)) {
            return primed.byPerson().getOrDefault(personId, List.of());
        }

        return personIdentifierRepository.findIdentifiersForPersonAndIdentifierAccessLevel(personId,
            AccessLevel.OPEN);
    }

    /**
     * Resolvable URLs ordered by identifier code, skipping identifiers without a URL template.
     */
    public static List<String> resolvableUris(Integer personId) {
        return openIdentifiers(personId).stream()
            .map(PublicPersonIdentifiers::resolvableUri)
            .filter(Objects::nonNull)
            .toList();
    }

    @Nullable
    public static String firstResolvableUri(Integer personId) {
        return resolvableUris(personId).stream().findFirst().orElse(null);
    }

    @Nullable
    public static String resolvableUri(PersonIdentifier personIdentifier) {
        var identifier = personIdentifier.getIdentifier();

        if (Objects.isNull(identifier)) {
            return null;
        }

        return IdentifierUtil.buildIdentifierUrl(identifier.getUrlTemplate(),
            personIdentifier.getValue());
    }

    private record Primed(Set<Integer> covered, Map<Integer, List<PersonIdentifier>> byPerson) {
    }
}
