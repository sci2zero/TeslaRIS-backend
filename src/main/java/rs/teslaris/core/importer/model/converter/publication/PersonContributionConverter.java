package rs.teslaris.core.importer.model.converter.publication;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.importer.model.common.PersonAttributes;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.service.interfaces.person.PersonService;

@Component
@RequiredArgsConstructor
@Slf4j
public class PersonContributionConverter {

    private final PersonService personService;

    public <T extends PersonAttributes> void addContributors(List<T> contributors,
                                                             DocumentContributionType contributionType,
                                                             List<PersonDocumentContributionDTO> contributions) {
        if (Objects.isNull(contributors)) {
            return;
        }

        for (int i = 0; i < contributors.size(); i++) {
            var contributor = contributors.get(i);
            var contribution = createContribution(contributor, contributionType, i);
            if (Objects.nonNull(contribution)) {
                contribution.setIsMainContributor(i == 0);
                contribution.setIsCorrespondingContributor(false);
                contributions.add(contribution);
            }
        }
    }

    @Nullable
    private <T extends PersonAttributes> PersonDocumentContributionDTO createContribution(
        T contributor,
        DocumentContributionType contributionType,
        int orderNumber) {
        var contribution = new PersonDocumentContributionDTO();
        contribution.setContributionType(contributionType);
        contribution.setContributionDescription(new ArrayList<>());
        contribution.setDisplayAffiliationStatement(new ArrayList<>());

        var person = personService.findPersonByOldId(
            OAIPMHParseUtility.parseBISISID(contributor.getPerson().getOldId()));
        if (Objects.isNull(person)) {
            log.warn("No saved person with id: " + contributor.getPerson().getOldId());
            return null;
        }
        contribution.setPersonId(person.getId());

        if (Objects.nonNull(contributor.getDisplayName())) {
            contribution.setPersonName(
                new PersonNameDTO(null, contributor.getDisplayName(), "", "", null, null));
        }

        contribution.setOrderNumber(orderNumber);

        return contribution;
    }
}
