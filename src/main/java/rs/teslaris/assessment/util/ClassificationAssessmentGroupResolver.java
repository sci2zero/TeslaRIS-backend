package rs.teslaris.assessment.util;

import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;
import rs.teslaris.core.service.interfaces.classification.AssessmentGroupResolver;

@Component
public class ClassificationAssessmentGroupResolver implements AssessmentGroupResolver {

    @Override
    @Nullable
    public String resolveGroupCode(String assessmentCode) {
        return ClassificationPriorityMapping.getGroupCode(assessmentCode);
    }
}
