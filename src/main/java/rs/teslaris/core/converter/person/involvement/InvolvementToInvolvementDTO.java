package rs.teslaris.core.converter.person.involvement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.converter.commontypes.MultilingualContentToMultilingualContentDTO;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.InvolvementDTO;
import rs.teslaris.core.dto.person.involvement.MembershipDTO;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.Membership;

@Component
@RequiredArgsConstructor
public class InvolvementToInvolvementDTO {

    private final MultilingualContentToMultilingualContentDTO multilingualContentConverter;

    public EducationDTO toDTO(Education education) {
        var dto = new EducationDTO();
        setCommonFields(education, dto);

        var thesisTitle =
            multilingualContentConverter.getMultilingualContentDTO(education.getThesisTitle());
        var title = multilingualContentConverter.getMultilingualContentDTO(education.getTitle());
        var abbreviationTitle = multilingualContentConverter.getMultilingualContentDTO(
            education.getAbbreviationTitle());

        dto.setThesisTitle(thesisTitle);
        dto.setTitle(title);
        dto.setAbbreviationTitle(abbreviationTitle);

        return dto;
    }

    public MembershipDTO toDTO(Membership membership) {
        var dto = new MembershipDTO();
        setCommonFields(membership, dto);

        var contributionDescription =
            multilingualContentConverter.getMultilingualContentDTO(
                membership.getContributionDescription());
        var role = multilingualContentConverter.getMultilingualContentDTO(membership.getRole());

        dto.setContributionDescription(contributionDescription);
        dto.setRole(role);

        return dto;
    }

    public EmploymentDTO toDTO(Employment employment) {
        var dto = new EmploymentDTO();
        setCommonFields(employment, dto);

        var role = multilingualContentConverter.getMultilingualContentDTO(employment.getRole());

        dto.setEmploymentPosition(employment.getEmploymentPosition());
        dto.setRole(role);

        return dto;
    }


    private void setCommonFields(Involvement involvement, InvolvementDTO dto) {
        var affiliationStatements =
            multilingualContentConverter.getMultilingualContentDTO(
                involvement.getAffiliationStatement());

        dto.setDateFrom(involvement.getDateFrom());
        dto.setDateTo(involvement.getDateTo());
        // TODO: ADD PROOFS
        dto.setInvolvementType(involvement.getInvolvementType());
        dto.setAffiliationStatement(affiliationStatements);
        dto.setOrganisationUnitId(involvement.getOrganisationUnit().getId());
    }
}
