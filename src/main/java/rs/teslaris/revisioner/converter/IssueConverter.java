package rs.teslaris.revisioner.converter;

import java.util.List;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.revisioner.dto.DataQualityIssueDTO;
import rs.teslaris.revisioner.indexmodel.DataQualityAssessmentIndex;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;

public class IssueConverter {

    public static DataQualityIssueDTO toDTO(DataQualityAssessmentIndex assessment, String ruleKey) {
        var remark = DataQualityAssessmentConfigurationLoader.getIssue(
            assessment.getProfileName(), assessment.getProfileVersion(), ruleKey);

        return new DataQualityIssueDTO(
            Integer.valueOf(assessment.getId()),
            assessment.getEntityType(),
            assessment.getEntityId(),
            remark.target(),
            assessment.getRecordMajorVersion(),
            assessment.getRecordMinorVersion(),
            assessment.getAssessmentDate(),
            ruleKey,
            remark.dimension(),
            remark.severity(),
            remark.blocking(),
            MultilingualContentConverter.getMultilingualContentDTO(
                DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                    assessment.getProfileName(), assessment.getProfileVersion(), ruleKey)),
            MultilingualContentConverter.getMultilingualContentDTO(
                DataQualityAssessmentConfigurationLoader.getDataQualityRemark(
                    assessment.getProfileName(), assessment.getProfileVersion(), ruleKey,
                    List.of())),
            assessment.getEntityNameSr(),
            assessment.getEntityNameOther()
        );
    }
}
