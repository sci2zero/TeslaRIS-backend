package rs.teslaris.migrator.converter.hydrator;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.migrator.converter.hydrator.ProjectMigrationDTO.ConsortiumEntry;

/**
 * The record log key of one consortium member.
 * <p>
 * A member has no identifier in the source, so the key is built from its identity: the organisation
 * unit it resolves to, else its normalised name, else its position. A new order of the members
 * therefore no longer moves the key to a different member.
 */
@Component
@RequiredArgsConstructor
public class HydratorConsortiumKeyFactory {

    private final HydratorInstitutionResolver institutionResolver;

    private final HydratorConversionUtil conversionUtil;


    public String keyOf(String projectSourceKey, ConsortiumEntry entry) {
        return projectSourceKey + "#consortium#" + identityOf(entry) + "#" + entry.type();
    }

    public String legacyKeyOf(String projectSourceKey, Integer orderNumber) {
        return ProjectMigrationDTO.consortiumKeyOf(projectSourceKey, orderNumber);
    }

    private String identityOf(ConsortiumEntry entry) {
        var match = institutionResolver.resolve(entry.institution());

        if (match.found()) {
            return "ou-" + match.organisationUnitId();
        }

        var name = Objects.isNull(entry.institution()) ? null : entry.institution().name();

        if (Objects.nonNull(name) && !name.isBlank()) {
            return "name-" + conversionUtil.normalise(name);
        }

        return "order-" + entry.orderNumber();
    }
}
