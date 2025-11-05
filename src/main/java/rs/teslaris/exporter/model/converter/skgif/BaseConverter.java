package rs.teslaris.exporter.model.converter.skgif;

import java.util.List;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.exporter.model.common.ExportMultilingualContent;

public class BaseConverter {

    protected static String extractTitleFromMC(List<ExportMultilingualContent> exportMC) {
        return exportMC.stream()
            .filter(
                mc -> mc.getLanguageTag().equalsIgnoreCase(LanguageAbbreviations.ENGLISH))
            .findFirst()
            .orElse(
                exportMC.stream().findFirst().orElse(
                    new ExportMultilingualContent(LanguageAbbreviations.ENGLISH, "",
                        1))).getContent();
    }
}
