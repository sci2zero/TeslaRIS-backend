package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.RunScopedCache;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.MigrationEntityType;

/**
 * Links output authors to persons migrated in the PERSON pass.
 * <p>
 * The curriculum owner is found through the record log, keyed by the curriculum id. A co-author
 * is reachable only through their Ciência ID, which the person pass stores as the national
 * science id. The same co-author shows up in many curricula, so lookups are cached for the run.
 */
@Component
@RequiredArgsConstructor
public class HydratorPersonResolver implements RunScopedCache {

    private static final String CIENCIA_ID_CODE = "CIENCIAID";

    private final MigrationIdResolver migrationIdResolver;

    private final PersonService personService;

    private final HydratorConversionUtil conversionUtil;

    private final Map<String, Optional<Integer>> personIdsByCienciaId = new ConcurrentHashMap<>();


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
        if (Objects.isNull(cienciaId) || cienciaId.isBlank()) {
            return null;
        }

        return personIdsByCienciaId.computeIfAbsent(cienciaId.trim(),
                key -> personService.findPersonByIdentifier(key).map(Person::getId))
            .orElse(null);
    }

    private HydratorCVModel.IdentifyingInfo identifyingInfo(HydratorCVModel.Curriculum record) {
        return Objects.isNull(record.curriculum()) ? null :
            record.curriculum().identifyingInfo();
    }

    @Override
    public void clearCache() {
        personIdsByCienciaId.clear();
    }
}
