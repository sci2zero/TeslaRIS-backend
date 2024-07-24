package rs.teslaris.core.exporter.model.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.importer.model.common.OrganisationUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportOrganisationUnit extends OrganisationUnit {

    private ExportOrganisationUnit superOU;
}
