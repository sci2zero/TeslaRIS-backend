package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;

/**
 * Matches an institution to an existing organisation unit by identifier (MAP-000046/047); without
 * a match the institution stays a display name.
 */
@Component
@RequiredArgsConstructor
public class HydratorInstitutionResolver {

    static final String NO_IDENTIFIER = "institution has no supported identifier";

    static final String NOT_FOUND = "identifier not found";

    // Types the import lookup searches by
    private static final Set<String> IDENTIFIER_TYPES = Set.of("RINGGOLD", "ROR");

    private final OrganisationUnitService organisationUnitService;

    // Empty value = looked up, not found
    private final Map<String, Optional<Integer>> organisationUnitIds = new ConcurrentHashMap<>();


    public Match resolve(HydratorCVModel.Institution institution) {
        var identifiers = supportedIdentifiersOf(institution);

        if (identifiers.isEmpty()) {
            return Match.missing(NO_IDENTIFIER);
        }

        for (var identifier : identifiers) {
            var organisationUnitId =
                organisationUnitIds.computeIfAbsent(identifier, this::lookUp);

            if (organisationUnitId.isPresent()) {
                return new Match(organisationUnitId.get(), null);
            }
        }

        return Match.missing(NOT_FOUND);
    }

    public void clearCache() {
        organisationUnitIds.clear();
    }

    // Returns the first hit if several organisation units share the identifier
    private Optional<Integer> lookUp(String identifier) {
        var index = organisationUnitService.findOrganisationUnitByImportId(identifier);
        return Objects.isNull(index) ? Optional.empty() :
            Optional.ofNullable(index.getDatabaseId());
    }

    private List<String> supportedIdentifiersOf(HydratorCVModel.Institution institution) {
        var identifiers = new ArrayList<HydratorCVModel.InstitutionIdentifier>();

        if (Objects.isNull(institution)) {
            return List.of();
        }

        if (Objects.nonNull(institution.identifier())) {
            identifiers.add(institution.identifier());
        }

        if (Objects.nonNull(institution.otherIdentifiers()) &&
            Objects.nonNull(institution.otherIdentifiers().identifiers())) {
            identifiers.addAll(institution.otherIdentifiers().identifiers());
        }

        return identifiers.stream()
            .filter(identifier -> Objects.nonNull(identifier) &&
                Objects.nonNull(identifier.type()) &&
                IDENTIFIER_TYPES.contains(identifier.type().trim().toUpperCase(Locale.ROOT)) &&
                Objects.nonNull(identifier.identifier()) && !identifier.identifier().isBlank())
            .map(identifier -> identifier.identifier().trim())
            .distinct()
            .toList();
    }

    public record Match(Integer organisationUnitId, String reason) {

        static Match missing(String reason) {
            return new Match(null, reason);
        }

        public boolean found() {
            return Objects.nonNull(organisationUnitId);
        }
    }
}
