package rs.teslaris.core.importer.model.converter.load.publication;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.importer.model.oaipmh.common.PersonAttributes;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.model.person.InvolvementType;
import rs.teslaris.core.service.interfaces.person.PersonService;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
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

        // Bottleneck, don't know how to speed this up...
        contribution.setInstitutionIds(new ArrayList<>());
        person.getInvolvements().forEach(involvement -> {
            if (involvement.getInvolvementType().equals(InvolvementType.EMPLOYED_AT) ||
                involvement.getInvolvementType().equals(InvolvementType.HIRED_BY)) {
                contribution.getInstitutionIds().add(involvement.getOrganisationUnit().getId());
            }
        });

        if (Objects.nonNull(contributor.getDisplayName())) {
            contribution.setPersonName(
                new PersonNameDTO(null, contributor.getDisplayName(), "", "", null, null));
        }

        contribution.setOrderNumber(orderNumber + 1);

        return contribution;
    }
}
