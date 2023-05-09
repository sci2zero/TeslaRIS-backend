package rs.teslaris.core.converter.person;

import rs.teslaris.core.converter.commontypes.MultilingualContentToMultilingualContentDTO;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.InvolvementDTO;
import rs.teslaris.core.dto.person.involvement.MembershipDTO;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.Membership;

public class InvolvementToInvolvementDTO {

    public static EducationDTO toDTO(Education education) {
        var dto = new EducationDTO();
        setCommonFields(education, dto);

        var thesisTitle =
            MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
                education.getThesisTitle());
        var title = MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
            education.getTitle());
        var abbreviationTitle =
            MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
                education.getAbbreviationTitle());

        dto.setThesisTitle(thesisTitle);
        dto.setTitle(title);
        dto.setAbbreviationTitle(abbreviationTitle);

        return dto;
    }

    public static MembershipDTO toDTO(Membership membership) {
        var dto = new MembershipDTO();
        setCommonFields(membership, dto);

        var contributionDescription =
            MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
                membership.getContributionDescription());
        var role = MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
            membership.getRole());

        dto.setContributionDescription(contributionDescription);
        dto.setRole(role);

        return dto;
    }

    public static EmploymentDTO toDTO(Employment employment) {
        var dto = new EmploymentDTO();
        setCommonFields(employment, dto);

        var role = MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
            employment.getRole());

        dto.setEmploymentPosition(employment.getEmploymentPosition());
        dto.setRole(role);

        return dto;
    }


    private static void setCommonFields(Involvement involvement, InvolvementDTO dto) {
        var affiliationStatements =
            MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
                involvement.getAffiliationStatement());

        dto.setDateFrom(involvement.getDateFrom());
        dto.setDateTo(involvement.getDateTo());
        // TODO: ADD PROOFS
        dto.setInvolvementType(involvement.getInvolvementType());
        dto.setAffiliationStatement(affiliationStatements);
        dto.setOrganisationUnitId(involvement.getOrganisationUnit().getId());
    }
}
