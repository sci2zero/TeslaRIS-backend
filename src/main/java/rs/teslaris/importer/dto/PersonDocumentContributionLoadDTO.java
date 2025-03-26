package rs.teslaris.importer.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.document.DocumentContributionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonDocumentContributionLoadDTO {

    private PersonLoadDTO person;

    private List<MultilingualContentDTO> contributionDescription = new ArrayList<>();

    private List<OrganisationUnitLoadDTO> institutions = new ArrayList<>();

    private Integer orderNumber;

    private DocumentContributionType contributionType;

    private Boolean isMainContributor;

    private Boolean isCorrespondingContributor;
}
