package rs.teslaris.migrator.configuration.hydrator;

import java.util.HashSet;
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

    private static final String SCIPROJ_DESCRIPTION = "Identifier of the project in SciPROJ";

    private final IdentifierRepository identifierRepository;

    private final LanguageTagService languageTagService;


    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void ensureIdentifiers() {
        var existing = identifierRepository.findByCode(HydratorSource.SCIPROJ_IDENTIFIER_CODE);
        if (Objects.nonNull(existing)) {
            addMissingValues(existing);
            return;
        }

        var english = languageTagService.findLanguageTagByValue(LanguageAbbreviations.ENGLISH);
        if (Objects.isNull(english.getId())) {
            log.error("Language tag '{}' does not exist, so identifier '{}' was not created and "
                    + "SciPROJ ids will not be attached to projects.", LanguageAbbreviations.ENGLISH,
                HydratorSource.SCIPROJ_IDENTIFIER_CODE);
            return;
        }

        var identifier = new Identifier();
        identifier.setCode(HydratorSource.SCIPROJ_IDENTIFIER_CODE);
        identifier.setTitle(new HashSet<>(Set.of(new MultiLingualContent(english, "SciPROJ ID", 1))));
        identifier.setDescription(new HashSet<>(Set.of(
            new MultiLingualContent(english, SCIPROJ_DESCRIPTION, 1))));
        identifier.setAccessLevel(AccessLevel.OPEN);
        identifier.setApplicableTypes(new HashSet<>(Set.of(ApplicableEntityType.PROJECT)));
        identifier.setUrlTemplate(SCIPROJ_URL_TEMPLATE);

        identifierRepository.save(identifier);
        log.info("Created identifier '{}'.", HydratorSource.SCIPROJ_IDENTIFIER_CODE);
    }

    // Values set by an admin are left as they are
    private void addMissingValues(Identifier identifier) {
        var changed = false;

        if (Objects.isNull(identifier.getUrlTemplate()) || identifier.getUrlTemplate().isBlank()) {
            identifier.setUrlTemplate(SCIPROJ_URL_TEMPLATE);
            changed = true;
        }

        if (identifier.getDescription().isEmpty()) {
            var english = languageTagService.findLanguageTagByValue(LanguageAbbreviations.ENGLISH);
            if (Objects.nonNull(english.getId())) {
                identifier.getDescription()
                    .add(new MultiLingualContent(english, SCIPROJ_DESCRIPTION, 1));
                changed = true;
            }
        }

        if (changed) {
            identifierRepository.save(identifier);
            log.info("Added missing values to identifier '{}'.",
                HydratorSource.SCIPROJ_IDENTIFIER_CODE);
        }
    }
}
