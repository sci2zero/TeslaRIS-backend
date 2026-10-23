package rs.teslaris.core.service.interfaces.classification;

import jakarta.annotation.Nullable;

/**
 * Maps a commission's assessment classification code onto the group it belongs to, so that core can
 * record the group on a document index without knowing how classifications are organised. The
 * mapping itself is assessment configuration and stays in the assessment module.
 */
public interface AssessmentGroupResolver {

    @Nullable
    String resolveGroupCode(String assessmentCode);
}
