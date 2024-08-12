package rs.teslaris.core.exporter.model.converter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import rs.teslaris.core.exporter.model.common.ExportMultilingualContent;
import rs.teslaris.core.importer.model.oaipmh.common.MultilingualContent;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

public class ExportMultilingualContentConverter extends ExportConverterBase {

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

    public static List<MultilingualContent> toOpenaireModel(
        List<ExportMultilingualContent> multilingualContent) {
        var openaireMCList = new ArrayList<MultilingualContent>();

        multilingualContent.forEach(mc -> {
            var openaireMC = new MultilingualContent();
            openaireMC.setLang(mc.getLanguageTag());
            openaireMC.setValue(mc.getContent());
            openaireMCList.add(openaireMC);
        });

        return openaireMCList;
    }

    public static <R> void setFieldFromPriorityContent(
        Stream<ExportMultilingualContent> contentStream,
        Function<String, R> contentMapper,
        Consumer<R> setter) {
        contentStream
            .max(Comparator.comparingInt(ExportMultilingualContent::getPriority))
            .map(exportMultilingualContent -> contentMapper.apply(
                exportMultilingualContent.getContent()))
            .ifPresent(setter);
    }
}
