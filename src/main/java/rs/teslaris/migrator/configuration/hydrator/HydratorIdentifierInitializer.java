package rs.teslaris.migrator.configuration.hydrator;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.identifier.Identifier;
import rs.teslaris.core.repository.identifier.IdentifierRepository;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.migrator.converter.hydrator.HydratorSource;

/**
 * Creates the identifier definitions hydrator values are stored under, so an automatic run does
 * not depend on an admin creating them first. Seed data runs only in the test profile, hence here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HydratorIdentifierInitializer {

    private static final String SCIPROJ_URL_TEMPLATE =
        "https://sciproj.ptcris.pt/" + IdentifierUtil.ID_PLACEHOLDER + "/";

    private static final List<Definition> DEFINITIONS = List.of(
        new Definition(HydratorSource.SCIPROJ_PROJECT_IDENTIFIER_CODE,
            HydratorSource.LEGACY_SCIPROJ_PROJECT_IDENTIFIER_CODE, "SciPROJ Project ID",
            "Identifier of the project in SciPROJ", ApplicableEntityType.PROJECT,
            SCIPROJ_URL_TEMPLATE),
        new Definition(HydratorSource.SCIPROJ_FUNDING_IDENTIFIER_CODE, null,
            "SciPROJ Funding ID", "Identifier of the funding in SciPROJ",
            ApplicableEntityType.FUNDING, SCIPROJ_URL_TEMPLATE),
        // The curriculum's own id of an output has no public page to link to
        new Definition(HydratorSource.SOURCE_WORK_IDENTIFIER_CODE, null,
            "CIÊNCIA VITAE source work ID",
            "Identifier of the output in its CIÊNCIA VITAE curriculum",
            ApplicableEntityType.DOCUMENT, null));

    private final IdentifierRepository identifierRepository;

    private final LanguageTagService languageTagService;


    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void ensureIdentifiers() {
        DEFINITIONS.forEach(this::ensure);
    }

    private void ensure(Definition definition) {
        var existing = findOrRename(definition);
        if (Objects.nonNull(existing)) {
            addMissingValues(existing, definition);
            return;
        }

        var english = languageTagService.findLanguageTagByValue(LanguageAbbreviations.ENGLISH);
        if (Objects.isNull(english.getId())) {
            log.error("Language tag '{}' does not exist, so identifier '{}' was not created and "
                    + "its ids will not be attached.", LanguageAbbreviations.ENGLISH,
                definition.code());
            return;
        }

        var identifier = new Identifier();
        identifier.setCode(definition.code());
        identifier.setTitle(
            new HashSet<>(Set.of(new MultiLingualContent(english, definition.title(), 1))));
        identifier.setDescription(new HashSet<>(Set.of(
            new MultiLingualContent(english, definition.description(), 1))));
        identifier.setAccessLevel(AccessLevel.OPEN);
        identifier.setApplicableTypes(new HashSet<>(Set.of(definition.type())));
        identifier.setUrlTemplate(definition.urlTemplate());

        identifierRepository.save(identifier);
        log.info("Created identifier '{}'.", definition.code());
    }

    // A definition created under the old code keeps its values and attached ids
    private Identifier findOrRename(Definition definition) {
        var existing = identifierRepository.findByCode(definition.code());
        if (Objects.nonNull(existing) || Objects.isNull(definition.legacyCode())) {
            return existing;
        }

        var legacy = identifierRepository.findByCode(definition.legacyCode());
        if (Objects.isNull(legacy)) {
            return null;
        }

        legacy.setCode(definition.code());
        identifierRepository.save(legacy);
        log.info("Renamed identifier '{}' to '{}'.", definition.legacyCode(), definition.code());

        return legacy;
    }

    // Values set by an admin are left as they are
    private void addMissingValues(Identifier identifier, Definition definition) {
        var changed = false;

        var hasUrlTemplate = Objects.nonNull(identifier.getUrlTemplate()) &&
            !identifier.getUrlTemplate().isBlank();

        if (Objects.nonNull(definition.urlTemplate()) && !hasUrlTemplate) {
            identifier.setUrlTemplate(definition.urlTemplate());
            changed = true;
        }

        if (identifier.getDescription().isEmpty()) {
            var english = languageTagService.findLanguageTagByValue(LanguageAbbreviations.ENGLISH);
            if (Objects.nonNull(english.getId())) {
                identifier.getDescription()
                    .add(new MultiLingualContent(english, definition.description(), 1));
                changed = true;
            }
        }

        if (changed) {
            identifierRepository.save(identifier);
            log.info("Added missing values to identifier '{}'.", definition.code());
        }
    }

    private record Definition(
        String code,
        String legacyCode,
        String title,
        String description,
        ApplicableEntityType type,
        String urlTemplate
    ) {
    }
}
