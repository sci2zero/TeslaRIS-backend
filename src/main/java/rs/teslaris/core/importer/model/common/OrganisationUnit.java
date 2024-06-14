package rs.teslaris.core.importer.model.common;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnit {

    private List<MultilingualContent> name = new ArrayList<>();

    private String nameAbbreviation;

    private String scopusAfid;
}
