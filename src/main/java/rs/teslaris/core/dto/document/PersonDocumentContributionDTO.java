package rs.teslaris.core.dto.document;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.document.EmploymentTitle;
import rs.teslaris.core.model.document.PersonalTitle;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonDocumentContributionDTO extends PersonContributionDTO {

    @NotNull(message = "You have to specify a contribution type.")
    private DocumentContributionType contributionType;

    @NotNull(message = "You have to specify weather the contributor is the main one.")
    private Boolean isMainContributor;

    @NotNull(message = "You have to specify weather the contributor is the corresponding one.")
    private Boolean isCorrespondingContributor;

    @NotNull(message = "You have to specify weather the contributor is board president.")
    private Boolean isBoardPresident;

    private EmploymentTitle employmentTitle;

    private PersonalTitle personalTitle;
}
