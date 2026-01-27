package rs.teslaris.importer.model.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private Set<OrganisationUnit> institutions = new HashSet<>();

    @Field("orderNumber")
    private Integer orderNumber;

    @Field("contribution_type")
    private DocumentContributionType contributionType;

    @Field("main_contributor")
    private Boolean isMainContributor;

    @Field("corresponding_contributor")
    private Boolean isCorrespondingContributor;
}
