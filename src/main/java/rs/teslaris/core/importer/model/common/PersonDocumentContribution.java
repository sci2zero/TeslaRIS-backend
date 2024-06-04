package rs.teslaris.core.importer.model.common;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.model.document.DocumentContributionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonDocumentContribution {

    @Field("person")
    private Person person;

    @Field("contributionDescription")
    private List<MultilingualContent> contributionDescription = new ArrayList<>();

    @Field("affiliations")
    private List<OrganisationUnit> institutions = new ArrayList<>();

    @Field("orderNumber")
    private Integer orderNumber;

    @Field("contribution_type")
    private DocumentContributionType contributionType;

    @Field("main_contributor")
    private Boolean isMainContributor;

    @Field("corresponding_contributor")
    private Boolean isCorrespondingContributor;
}
