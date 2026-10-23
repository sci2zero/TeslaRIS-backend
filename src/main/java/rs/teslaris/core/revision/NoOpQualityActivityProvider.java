package rs.teslaris.core.revision;

import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.OrganisationUnitContributionDTO;
import rs.teslaris.core.dto.document.PersonContributionDTO;

/**
 * Keeps {@code List<QualityActivityProvider>} satisfiable in a deployment where no module
 * contributes a provider, so that the data quality calculator still starts.
 */
@Component
public class NoOpQualityActivityProvider implements QualityActivityProvider {

    @Override
    public String entityType() {
        return UnmatchedEntityType.VALUE;
    }

    @Override
    public List<PersonContributionDTO> personContributions(Integer entityId) {
        return Collections.emptyList();
    }

    @Override
    public List<OrganisationUnitContributionDTO> organisationUnitContributions(Integer entityId) {
        return Collections.emptyList();
    }
}
