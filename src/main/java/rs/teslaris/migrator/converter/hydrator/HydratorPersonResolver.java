package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
     * Every name form the owner gives in their curriculum - the citation names are the ones that
     * show up in author lists.
     */
    public List<PersonNameDTO> ownerNames(HydratorCVModel.Curriculum record) {
        var names = new ArrayList<PersonNameDTO>();
        var info = identifyingInfo(record);

        if (Objects.isNull(info)) {
            return names;
        }

        var personInfo = info.personInfo();

        if (Objects.nonNull(personInfo) && Objects.nonNull(personInfo.names()) &&
            Objects.nonNull(personInfo.surnames())) {
            names.add(conversionUtil.personName(personInfo.names(), personInfo.surnames(), null));
        }

        ownerNameForms(info).forEach(form -> names.add(conversionUtil.splitName(form, null)));
        names.removeIf(Objects::isNull);

        return names;
    }

    // The owner's name forms read surname first ("Montillet Jean Philippe"), which some write
    public List<PersonNameDTO> ownerSurnameFirstNames(HydratorCVModel.Curriculum record) {
        var info = identifyingInfo(record);

        if (Objects.isNull(info)) {
            return List.of();
        }

        return ownerNameForms(info).stream()
            .filter(form -> !form.contains(","))
            .map(form -> form.trim().split("\\s+", 2))
            .filter(words -> words.length == 2)
            .map(words -> conversionUtil.personName(words[1], words[0], null))
            .toList();
    }

    private List<String> ownerNameForms(HydratorCVModel.IdentifyingInfo info) {
        var forms = new ArrayList<String>();
        var personInfo = info.personInfo();

        if (Objects.nonNull(personInfo)) {
            forms.add(personInfo.fullName());
            forms.add(personInfo.displayName());
        }

        if (Objects.nonNull(info.citationNames()) &&
            Objects.nonNull(info.citationNames().citationName())) {
            info.citationNames().citationName().stream()
                .filter(Objects::nonNull)
                .forEach(citationName -> forms.add(citationName.value()));
        }

        forms.removeIf(form -> Objects.isNull(form) || form.isBlank());

        return forms;
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
