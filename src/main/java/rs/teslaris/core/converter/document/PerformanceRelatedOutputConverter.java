package rs.teslaris.core.converter.document;

import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.LanguageTagResponseDTO;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.model.document.PerformanceRelatedOutput;

public class PerformanceRelatedOutputConverter extends DocumentPublicationConverter {

    public static PerformanceRelatedOutputDTO toDTO(
        PerformanceRelatedOutput performanceRelatedOutput) {
        var performanceRelatedOutputDTO = new PerformanceRelatedOutputDTO();

        setCommonFields(performanceRelatedOutput, performanceRelatedOutputDTO);

        performanceRelatedOutputDTO.setType(performanceRelatedOutput.getType());

        performanceRelatedOutputDTO.setProducer(
            MultilingualContentConverter.getMultilingualContentDTO(
                performanceRelatedOutput.getProducer()));
        performanceRelatedOutputDTO.setDistributor(
            MultilingualContentConverter.getMultilingualContentDTO(
                performanceRelatedOutput.getDistributor()));
        performanceRelatedOutputDTO.setSourceTitle(
            MultilingualContentConverter.getMultilingualContentDTO(
                performanceRelatedOutput.getSourceTitle()));
        performanceRelatedOutputDTO.setOtherActors(
            MultilingualContentConverter.getMultilingualContentDTO(
                performanceRelatedOutput.getOtherActors()));

        if (Objects.nonNull(performanceRelatedOutput.getLanguages())) {
            performanceRelatedOutput.getLanguages().forEach(languageTag -> {
                performanceRelatedOutputDTO.getLanguageTagIds().add(languageTag.getId());
                performanceRelatedOutputDTO.getLanguageTags().add(
                    new LanguageTagResponseDTO(languageTag.getId(), languageTag.getLanguageTag(),
                        languageTag.getDisplay()));
            });
        }

        return performanceRelatedOutputDTO;
    }
}
