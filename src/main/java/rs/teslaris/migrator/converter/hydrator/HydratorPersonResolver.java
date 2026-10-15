package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.RunScopedCache;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.MigrationEntityType;

/**
 * Links hydrator records to persons migrated in the PERSON pass.
 * <p>
 * The curriculum owner is found through the record log, keyed by the curriculum id. A co-author
 * or project team member is reachable only through their identifiers (Ciência ID, then ORCID),
 * which the person pass stores on the person. The same person shows up in many records, so
 * lookups are cached for the run.
 */
@Component
@RequiredArgsConstructor
public class HydratorPersonResolver implements RunScopedCache {

    static final String NO_IDENTIFIER = "person has no identifier";

    static final String NOT_FOUND = "identifier not found";

    static final String AMBIGUOUS = "ambiguous identifier";

    private static final String CIENCIA_ID_CODE = "CIENCIAID";

    private final MigrationIdResolver migrationIdResolver;

    private final PersonService personService;

    private final HydratorConversionUtil conversionUtil;

    // Empty value = looked up, not found; ambiguous identifiers are not cached
    private final Map<String, Optional<Integer>> personIds = new ConcurrentHashMap<>();


    /**
     * Matches a project team member to an existing person by Ciência ID, then ORCID
     * (CW-002-MAP-000028).
     */
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

    /**
     * The record log is the direct link from a curriculum to its person; the owner's Ciência ID
     * is the fallback, so linking survives a record log that was cleared or kept elsewhere.
     */
    public Integer resolveOwner(HydratorCVModel.Curriculum record) {
        return migrationIdResolver.resolve(HydratorSource.NAME, MigrationEntityType.PERSON,
                record.id())
            .orElseGet(() -> resolveByCienciaId(ownerCienciaId(record)));
    }

    public String ownerCienciaId(HydratorCVModel.Curriculum record) {
        var info = identifyingInfo(record);

        if (Objects.isNull(info) || Objects.isNull(info.authorIdentifiers()) ||
            Objects.isNull(info.authorIdentifiers().authorIdentifier())) {
            return null;
        }

        return info.authorIdentifiers().authorIdentifier().stream()
            .filter(identifier -> Objects.nonNull(identifier.identifierType()) &&
                CIENCIA_ID_CODE.equalsIgnoreCase(identifier.identifierType().code()))
            .map(HydratorCVModel.AuthorIdentifier::identifier)
            .filter(value -> Objects.nonNull(value) && !value.isBlank())
            .map(String::trim)
            .findFirst()
            .orElse(null);
    }

    /**
     * Match keys of every name form the owner gives in their curriculum - the citation names are
     * the ones that show up in author lists.
     */
    public Set<String> ownerNameKeys(HydratorCVModel.Curriculum record) {
        var info = identifyingInfo(record);

        if (Objects.isNull(info)) {
            return Set.of();
        }

        var names = new ArrayList<PersonNameDTO>();
        var personInfo = info.personInfo();

        if (Objects.nonNull(personInfo)) {
            names.add(conversionUtil.splitName(personInfo.fullName(), null));
            names.add(conversionUtil.splitName(personInfo.displayName(), null));

            if (Objects.nonNull(personInfo.names()) && Objects.nonNull(personInfo.surnames())) {
                names.add(conversionUtil.personName(personInfo.names(), personInfo.surnames(),
                    null));
            }
        }

        if (Objects.nonNull(info.citationNames()) &&
            Objects.nonNull(info.citationNames().citationName())) {
            info.citationNames().citationName().stream()
                .filter(Objects::nonNull)
                .forEach(citationName ->
                    names.add(conversionUtil.splitName(citationName.value(), null)));
        }

        return names.stream()
            .map(conversionUtil::authorMatchKey)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    public Integer resolveByCienciaId(String cienciaId) {
        if (isBlank(cienciaId)) {
            return null;
        }

        return personIds.computeIfAbsent(cienciaId.trim(), this::lookUp).orElse(null);
    }

    @Override
    public void clearCache() {
        personIds.clear();
    }

    private Optional<Integer> lookUp(String identifier) {
        return personService.findPersonByIdentifier(identifier).map(Person::getId);
    }

    private HydratorCVModel.IdentifyingInfo identifyingInfo(HydratorCVModel.Curriculum record) {
        return Objects.isNull(record.curriculum()) ? null :
            record.curriculum().identifyingInfo();
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
