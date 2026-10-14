package rs.teslaris.project.revision;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.revision.QualityIdentifierField;
import rs.teslaris.core.revision.QualityUniquenessChecker;
import rs.teslaris.project.repository.funding.FundingRepository;

@Component
@RequiredArgsConstructor
public class FundingQualityUniquenessChecker implements QualityUniquenessChecker {

    private final FundingRepository fundingRepository;


    @Override
    public String entityType() {
        return EntityType.FUNDING.name();
    }

    @Override
    public boolean isDuplicate(QualityIdentifierField field, String value, Integer entityId) {
        return switch (field) {
            case DOI -> fundingRepository.existsByDoi(value, entityId);
            // A funding carries neither; its national reference is the funded project's.
            case RAID, NATIONAL_ID -> false;
        };
    }
}
