package rs.teslaris.revisioner.converter;

import java.util.LinkedHashMap;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.revisioner.dto.DataQualityProfileDTO;
import rs.teslaris.revisioner.dto.DataQualityRemarkDTO;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;

public class DataQualityProfileConverter {

    public static DataQualityProfileDTO toDTO(
        String profileName,
        DataQualityAssessmentConfigurationLoader.DataQualityProfile dataQualityProfile,
        LanguageTagService languageTagService) {

        var dataQualityRemarks = new LinkedHashMap<String, DataQualityRemarkDTO>();

        dataQualityProfile.dataQualityRemarks().forEach((key, remark) -> {
            var targetWeight =
                dataQualityProfile.targetWeights().getOrDefault(remark.target(), 1.0);

            dataQualityRemarks.put(
                key,
                new DataQualityRemarkDTO(
                    StringUtil.buildMultilingualContentDTO(languageTagService, remark.title()),
                    StringUtil.buildMultilingualContentDTO(languageTagService, remark.message()),
                    remark.target(),
                    targetWeight,
                    remark.severity(),
                    remark.dimension(),
                    remark.blocking(),
                    remark.points(),
                    remark.usedForFairCompliance(),
                    remark.constraints()
                )
            );
        });

        return new DataQualityProfileDTO(profileName, dataQualityProfile.version(),
            dataQualityRemarks);
    }
}
