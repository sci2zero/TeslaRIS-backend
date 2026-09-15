package rs.teslaris.core.converter.person;

import org.jbibtex.BibTeXEntry;
import org.jbibtex.StringValue;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.dto.document.PersonContributionDTO;
import rs.teslaris.core.dto.document.PersonDocumentContributionDTO;
import rs.teslaris.core.dto.document.PersonEventContributionDTO;
import rs.teslaris.core.dto.document.PersonPublicationSeriesContributionDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.*;
import rs.teslaris.project.dto.funding.PersonFundingCallContributionDTO;
import rs.teslaris.project.model.funding.PersonFundingCallContribution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PersonContributionConverter {

    public static ArrayList<PersonDocumentContributionDTO> documentContributionToDTO(
        Set<PersonDocumentContribution> contributions) {
        var contributionDTOs = new ArrayList<PersonDocumentContributionDTO>();
        contributions.stream().filter(c -> c.getApproveStatus().equals(ApproveStatus.APPROVED) &&
                c.getDeleted().equals(false))
            .forEach((c) -> {
                var contribution = new PersonDocumentContributionDTO();
                setCommonFields(contribution, c);

                contribution.setContributionType(c.getContributionType());
                contribution.setIsMainContributor(c.getIsMainContributor());
                contribution.setIsCorrespondingContributor(c.getIsCorrespondingContributor());
                contribution.setIsBoardPresident(c.getIsBoardPresident());
                contribution.setPersonalTitle(c.getPersonalTitle());
                contribution.setEmploymentTitle(c.getEmploymentTitle());
                contribution.setDateFrom(c.getDateFrom());
                contribution.setDateTo(c.getDateTo());

                c.getResearchAreas().forEach(researchArea -> {
                    contribution.getResearchAreasId().add(researchArea.getId());
                    contribution.getResearchAreas().add(ResearchAreaConverter.toDTO(researchArea));
                });

                contributionDTOs.add(contribution);
            });
        return contributionDTOs;
    }

    public static ArrayList<PersonPublicationSeriesContributionDTO> publicationSeriesContributionToDTO(
        Set<PersonPublicationSeriesContribution> contributions) {
        var contributionDTOs = new ArrayList<PersonPublicationSeriesContributionDTO>();
        contributions.stream().filter(c -> c.getApproveStatus().equals(ApproveStatus.APPROVED))
            .forEach((c) -> {
                var contribution = new PersonPublicationSeriesContributionDTO();
                setCommonFields(contribution, c);

                contribution.setContributionType(c.getContributionType());
                contribution.setDateFrom(c.getDateFrom());
                contribution.setDateTo(c.getDateTo());
                contribution.setIsMainContributor(c.getIsMainContributor());

                contributionDTOs.add(contribution);
            });
        return contributionDTOs;
    }

    public static ArrayList<PersonEventContributionDTO> eventContributionToDTO(
        Set<PersonEventContribution> contributions) {
        var contributionDTOs = new ArrayList<PersonEventContributionDTO>();
        contributions.stream().filter(c -> c.getApproveStatus().equals(ApproveStatus.APPROVED))
            .forEach((c) -> {
                var contribution = new PersonEventContributionDTO();
                setCommonFields(contribution, c);

                contribution.setEventContributionType(c.getContributionType());
                contribution.setLectureHoursPerWeek(c.getLectureHoursPerWeek());
                contribution.setTutorialHoursPerWeek(c.getTutorialHoursPerWeek());
                contribution.setLabHoursPerWeek(c.getLabHoursPerWeek());
                contribution.setOtherContactHoursPerWeek(c.getOtherContactHoursPerWeek());
                contribution.setNumberOfReviewsOrAssessment(c.getNumberOfReviewsOrAssessment());
                contribution.setCaseName(
                    MultilingualContentConverter.getMultilingualContentDTO(c.getCaseName()));
                contribution.setLocationJurisdiction(
                    MultilingualContentConverter.getMultilingualContentDTO(
                        c.getLocationJurisdiction()));
                contribution.setMainArguer(c.getMainArguer());

                contributionDTOs.add(contribution);
            });
        return contributionDTOs;
    }

    private static void setCommonFields(PersonContributionDTO contributionDTO,
                                        PersonContribution contribution) {
        contributionDTO.setId(contribution.getId());

        contributionDTO.setContributionDescription(
            MultilingualContentConverter.getMultilingualContentDTO(
                contribution.getContributionDescription()));
        contributionDTO.setDisplayAffiliationStatement(
            MultilingualContentConverter.getMultilingualContentDTO(
                contribution.getAffiliationStatement().getDisplayAffiliationStatement()));

        contributionDTO.setOrderNumber(contribution.getOrderNumber());

        if (Objects.nonNull(contribution.getPerson())) {
            contributionDTO.setPersonId(contribution.getPerson().getId());
        }

        contributionDTO.setInstitutionIds(new ArrayList<>());
        contribution.getInstitutions()
            .forEach(i -> {
                contributionDTO.getInstitutionIds().add(i.getId());
                contributionDTO.getDisplayInstitutionNames()
                    .add(MultilingualContentConverter.getMultilingualContentDTO(i.getName()));
            });

        contributionDTO.setPersonName(PersonNameConverter.toDTO(
            contribution.getAffiliationStatement().getDisplayPersonName()));
        contributionDTO.setPostalAddress(PostalAddressConverter.toDto(
            contribution.getAffiliationStatement().getPostalAddress()));
        contributionDTO.setContact(
            ContactConverter.toDTO(contribution.getAffiliationStatement().getContact()));
    }

    public static void toBibTexAuthors(Set<PersonDocumentContribution> contributions,
                                       BibTeXEntry entry) {
        var authors = contributions.stream()
            .filter(c -> c.getContributionType().equals(DocumentContributionType.AUTHOR))
            .sorted(Comparator.comparingInt(PersonDocumentContribution::getOrderNumber))
            .map((contribution) -> contribution.getAffiliationStatement().getDisplayPersonName()
                .toString()).collect(Collectors.joining(" and "));

        entry.addField(BibTeXEntry.KEY_AUTHOR, new StringValue(authors, StringValue.Style.BRACED));
    }

    public static void toTaggedAuthors(Set<PersonDocumentContribution> contributions,
                                       StringBuilder sb, boolean refMan) {
        contributions.stream()
            .filter(c -> c.getContributionType().equals(DocumentContributionType.AUTHOR))
            .sorted(Comparator.comparingInt(PersonDocumentContribution::getOrderNumber))
            .forEach(contribution -> {
                    var lastname =
                        contribution.getAffiliationStatement().getDisplayPersonName().getLastname();

                    sb.append(refMan ? "AU  - " : "%A ")
                        .append(lastname)
                        .append((Objects.nonNull(lastname) && !lastname.isBlank()) ? ", " : "")
                        .append(contribution.getAffiliationStatement().getDisplayPersonName()
                            .getFirstname())
                        .append("\n");
                }
            );
    }

    public static ArrayList<PersonFundingCallContributionDTO> fundingCallContributionToDTO(
        Set<PersonFundingCallContribution> contributions) {
        var contributionDTOs = new ArrayList<PersonFundingCallContributionDTO>();
        contributions.stream().filter(c -> c.getApproveStatus().equals(ApproveStatus.APPROVED))
                .forEach((c) -> {
                    var contribution = new PersonFundingCallContributionDTO();
                    setCommonFields(contribution, c);

                    contribution.setContributionType(c.getContributionType());

                    contributionDTOs.add(contribution);
                });
        return contributionDTOs;
    }
}
