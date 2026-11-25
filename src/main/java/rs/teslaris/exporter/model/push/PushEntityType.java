package rs.teslaris.exporter.model.push;

import static rs.teslaris.exporter.model.common.ExportPublicationType.BOOK_SERIES;
import static rs.teslaris.exporter.model.common.ExportPublicationType.GENETIC_MATERIAL;
import static rs.teslaris.exporter.model.common.ExportPublicationType.INTANGIBLE_PRODUCT;
import static rs.teslaris.exporter.model.common.ExportPublicationType.INTELLECTUAL_PROPERTY;
import static rs.teslaris.exporter.model.common.ExportPublicationType.JOURNAL;
import static rs.teslaris.exporter.model.common.ExportPublicationType.JOURNAL_PUBLICATION;
import static rs.teslaris.exporter.model.common.ExportPublicationType.MATERIAL_PRODUCT;
import static rs.teslaris.exporter.model.common.ExportPublicationType.MONOGRAPH;
import static rs.teslaris.exporter.model.common.ExportPublicationType.MONOGRAPH_PUBLICATION;
import static rs.teslaris.exporter.model.common.ExportPublicationType.PERFORMANCE_RELATED_OUTPUT;
import static rs.teslaris.exporter.model.common.ExportPublicationType.PROCEEDINGS;
import static rs.teslaris.exporter.model.common.ExportPublicationType.PROCEEDINGS_PUBLICATION;
import static rs.teslaris.exporter.model.common.ExportPublicationType.THESIS;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import rs.teslaris.exporter.model.common.BaseExportEntity;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportEvent;
import rs.teslaris.exporter.model.common.ExportFunding;
import rs.teslaris.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.exporter.model.common.ExportPerson;
import rs.teslaris.exporter.model.common.ExportProject;
import rs.teslaris.exporter.model.common.ExportPublicationType;
import rs.teslaris.exporter.model.common.ExportResearchArea;

@Getter
@RequiredArgsConstructor
public enum PushEntityType {

    // First, so the concepts are in the target before the entities that link to them.
    RESEARCH_AREAS("research_areas", ExportResearchArea.class),
    ORGANISATION_UNITS("org_units", ExportOrganisationUnit.class),
    PERSONS("persons", ExportPerson.class),
    EVENTS("events", ExportEvent.class),
    // Not a push set. Journals and book series are pushed with "documents", but they have their
    // own ID sequence, so their states need their own type.
    PUBLICATION_SERIES(null, ExportDocument.class),
    DOCUMENTS("documents", ExportDocument.class),
    PROJECTS("projects", ExportProject.class),
    FUNDINGS("fundings", ExportFunding.class);

    private final String code;

    private final Class<? extends BaseExportEntity> snapshotType;


    public static Optional<PushEntityType> fromCode(String code) {
        return Arrays.stream(values())
            .filter(type -> Objects.nonNull(type.code) && type.code.equals(code))
            .findFirst();
    }

    /**
     * The groups are pushed in this order, so that parents (journals, proceedings, monographs)
     * are in VIVO before the documents that link to them. An empty type list means all
     * snapshots of the class.
     */
    public List<PushGroup> groups() {
        if (this == DOCUMENTS) {
            return List.of(
                new PushGroup(PUBLICATION_SERIES, List.of(JOURNAL, BOOK_SERIES)),
                new PushGroup(DOCUMENTS, List.of(PROCEEDINGS, MONOGRAPH)),
                new PushGroup(DOCUMENTS, List.of(JOURNAL_PUBLICATION, PROCEEDINGS_PUBLICATION,
                    MONOGRAPH_PUBLICATION, THESIS, INTELLECTUAL_PROPERTY, INTANGIBLE_PRODUCT,
                    MATERIAL_PRODUCT, GENETIC_MATERIAL, PERFORMANCE_RELATED_OUTPUT)));
        }

        return List.of(new PushGroup(this, List.of()));
    }

    /**
     * @param stateType        the entity type of the saved states of this group.
     * @param publicationTypes the snapshot types of this group, empty for all.
     */
    public record PushGroup(PushEntityType stateType,
                            List<ExportPublicationType> publicationTypes) {
    }
}
