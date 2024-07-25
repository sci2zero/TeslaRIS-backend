package rs.teslaris.core.exporter.model.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import rs.teslaris.core.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

public class ExportMultilingualContentConverter {

    public static List<ExportMultilingualContent> toCommonExportModel(
        Set<MultiLingualContent> multiLingualContent) {
        var commonExportMC = new ArrayList<ExportMultilingualContent>();

        multiLingualContent.forEach(mc -> {
            var exportMC = new ExportMultilingualContent();
            exportMC.setLanguageTag(mc.getLanguage().getLanguageTag());
            exportMC.setContent(mc.getContent());
            exportMC.setPriority(mc.getPriority());

            commonExportMC.add(exportMC);
        });

        return commonExportMC;
    }
}
