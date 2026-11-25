package rs.teslaris.exporter.model.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.exporter.model.common.ExportResearchArea;

public class ExportResearchAreaConverter extends ExportConverterBase {

    public static ExportResearchArea toCommonExportModel(ResearchArea researchArea,
                                                         boolean computeRelations) {
        var commonExportResearchArea = new ExportResearchArea();

        setBaseFields(commonExportResearchArea, researchArea);
        if (commonExportResearchArea.getDeleted()) {
            return commonExportResearchArea;
        }

        commonExportResearchArea.setName(
            ExportMultilingualContentConverter.toCommonExportModel(researchArea.getName()));
        commonExportResearchArea.setDescription(
            ExportMultilingualContentConverter.toCommonExportModel(
                researchArea.getDescription()));
        if (Objects.nonNull(researchArea.getSuperResearchArea())) {
            commonExportResearchArea.setSuperResearchAreaId(
                researchArea.getSuperResearchArea().getId());
        }
        commonExportResearchArea.setSchemeName(researchArea.getSchemeName());

        return commonExportResearchArea;
    }

    /**
     * The IDs of the given research areas, sorted, for the researchAreaIds field of the other
     * snapshots. Null gives an empty list.
     */
    public static List<Integer> researchAreaIds(Collection<ResearchArea> researchAreas) {
        if (Objects.isNull(researchAreas)) {
            return new ArrayList<>();
        }

        return new ArrayList<>(researchAreas.stream().filter(Objects::nonNull)
            .map(ResearchArea::getId).filter(Objects::nonNull).distinct().sorted().toList());
    }
}
