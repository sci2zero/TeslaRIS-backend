package rs.teslaris.revisioner.converter;

import java.util.Comparator;
import java.util.Objects;
import rs.teslaris.revisioner.dto.RevisionDTO;
import rs.teslaris.revisioner.model.EntityRevision;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;

public class RevisionConverter {

    public static RevisionDTO toDTO(EntityRevision revision) {
        var assessments = revision.getAssessments().stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(DataQualityAssessment::getProfileName)
                .thenComparing(DataQualityAssessment::getProfileVersion))
            .map(DataQualityAssessmentConverter::toSimpleDTO)
            .toList();

        return new RevisionDTO(
            revision.getRevisionTimestamp(),
            revision.getMajorVersion(),
            revision.getMinorVersion(),
            revision.getAdminNote(),
            revision.getUpdatedBy(),
            assessments
        );
    }
}
