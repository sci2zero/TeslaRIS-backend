package rs.teslaris.core.service.impl.person;

import java.util.HashSet;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.AffiliationStatement;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.PersonDocumentContribution;
import rs.teslaris.core.model.person.Contact;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.model.person.PostalAddress;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.service.interfaces.person.PersonService;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonContributionServiceImpl implements PersonContributionService {

    private final PersonService personService;

    private final CountryService countryService;

    private final OrganisationUnitService organisationUnitService;

    private final MultilingualContentService multilingualContentService;


    @Value("${contribution.approved_by_default}")
    private Boolean contributionApprovedByDefault;


    @Override
    public void setPersonDocumentContributionsForDocument(Document document,
                                                          DocumentDTO documentDTO) {
        document.setContributors(new HashSet<>());
        documentDTO.getContributions().forEach(contributionDTO -> {
            var contribution = new PersonDocumentContribution();
            contribution.setContributionType(contributionDTO.getContributionType());
            contribution.setMainContributor(contributionDTO.getIsMainContributor());
            contribution.setCorrespondingContributor(
                contributionDTO.getIsCorrespondingContributor());

            setAffiliationStatement(contribution, contributionDTO);

            contribution.setInstitutions(new HashSet<>());
            contributionDTO.getInstitutionIds().forEach(institutionId -> {
                contribution.getInstitutions()
                    .add(organisationUnitService.findOrganisationUnitById(institutionId));
            });

            contribution.setContributionDescription(
                multilingualContentService.getMultilingualContent(
                    contributionDTO.getContributionDescription()));
            contribution.setOrderNumber(contributionDTO.getOrderNumber());

            if (contributionDTO.getPersonId() != null) {
                contribution.setPerson(personService.findPersonById(contributionDTO.getPersonId()));
            }

            contribution.setApproveStatus(
                contributionApprovedByDefault ? ApproveStatus.APPROVED : ApproveStatus.REQUESTED);

            document.addDocumentContribution(contribution);
        });
    }

    private void setAffiliationStatement(PersonDocumentContribution contribution,
                                         PersonDocumentContributionDTO contributionDTO) {
        var personName = new PersonName(contributionDTO.getPersonName().getFirstname(),
            contributionDTO.getPersonName().getOtherName(),
            contributionDTO.getPersonName().getLastname(),
            contributionDTO.getPersonName().getDateFrom(),
            contributionDTO.getPersonName().getDateTo());

        var countryId = contributionDTO.getPostalAddress().getCountryId();
        var postalAddress =
            new PostalAddress(countryId != null ? countryService.findCountryById(countryId) : null,
                multilingualContentService.getMultilingualContent(
                    contributionDTO.getPostalAddress().getStreetAndNumber()),
                multilingualContentService.getMultilingualContent(
                    contributionDTO.getPostalAddress().getCity()));

        var contact = new Contact(contributionDTO.getContact().getContactEmail(),
            contributionDTO.getContact().getPhoneNumber());

        contribution.setAffiliationStatement(new AffiliationStatement(
            multilingualContentService.getMultilingualContent(
                contributionDTO.getDisplayAffiliationStatement()), personName, postalAddress,
            contact));
    }
}
