package rs.teslaris.project.revision;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.revision.QualityScope;
import rs.teslaris.core.revision.QualityScopeResolver;
import rs.teslaris.project.indexrepository.funding.FundingIndexRepository;

/**
 * A funding has no contributors of its own; the people and institutions it belongs to are the
 * funded project's, which the funding index carries denormalised. A funding attached to an
 * involvement rather than a project therefore has no scope and is visible only to an
 * administrator - widening that would need the involvement, which the index does not hold.
 */
@Component
@RequiredArgsConstructor
public class FundingQualityScopeResolver implements QualityScopeResolver {

    private final FundingIndexRepository fundingIndexRepository;


    @Override
    public String entityType() {
        return EntityType.FUNDING.name();
    }

    @Override
    public Optional<QualityScope> resolve(Integer entityId) {
        return fundingIndexRepository.findFundingIndexByDatabaseId(entityId)
            .map(index -> new QualityScope(
                index.getPersonIds(),
                index.getOrganisationUnitIds(),
                index.getNameSr(),
                index.getNameOther()
            ));
    }
}
