package rs.teslaris.core.converter.document;

import java.util.Objects;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.LanguageTagResponseDTO;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.model.document.PerformanceRelatedOutput;
import rs.teslaris.core.util.persistence.IdentifierUtil;

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

    public static BibTeXEntry toBibTexEntry(PerformanceRelatedOutput performanceRelatedOutput,
                                            String defaultLanguageTag) {
        var entry = new BibTeXEntry(BibTeXEntry.TYPE_MISC,
            new Key(IdentifierUtil.identifierPrefix + performanceRelatedOutput.getId().toString()));

        setCommonFields(performanceRelatedOutput, entry, defaultLanguageTag);

        if (Objects.nonNull(performanceRelatedOutput.getType())) {
            entry.addField(BibTeXEntry.KEY_TYPE,
                new StringValue(performanceRelatedOutput.getType().name(),
                    StringValue.Style.BRACED));
        }

        return entry;
    }

    public static String toTaggedFormat(PerformanceRelatedOutput performanceRelatedOutput,
                                        String defaultLanguageTag,
                                        boolean refMan) {
        var sb = new StringBuilder();
        sb.append(refMan ? "TY  - " : "%0 ").append(refMan ? "GEN" : "Generic")
            .append("\n");

        setCommonTaggedFields(performanceRelatedOutput, sb, defaultLanguageTag, refMan);

        if (Objects.nonNull(performanceRelatedOutput.getType())) {
            sb.append(refMan ? "KW  - " : "%K ")
                .append("Type: ")
                .append(performanceRelatedOutput.getType().name())
                .append("\n");
        }

        if (refMan) {
            sb.append("ER  -\n");
        }

        return sb.toString();
    }
}
