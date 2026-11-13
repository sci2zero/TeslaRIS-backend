package rs.teslaris.exporter.model.push;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import rs.teslaris.exporter.model.common.BaseExportEntity;
import rs.teslaris.exporter.model.common.ExportEvent;
import rs.teslaris.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.exporter.model.common.ExportPerson;

@Getter
@RequiredArgsConstructor
public enum PushEntityType {

    ORGANISATION_UNITS("org_units", ExportOrganisationUnit.class),
    PERSONS("persons", ExportPerson.class),
    EVENTS("events", ExportEvent.class);

    private final String code;

    private final Class<? extends BaseExportEntity> snapshotType;


    public static Optional<PushEntityType> fromCode(String code) {
        return Arrays.stream(values()).filter(type -> type.code.equals(code)).findFirst();
    }
}
