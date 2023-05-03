package rs.teslaris.core.converter.involvement;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.InvolvementDTO;
import rs.teslaris.core.dto.person.involvement.MembershipDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.Membership;

@Component
public class InvolvementToInvolvementDTO {

    public EducationDTO toDTO(Education education) {
        var dto = new EducationDTO();
        setCommonFields(education, dto);

        var thesisTitle = getMultilingualContentDTO(education.getThesisTitle());
        var title = getMultilingualContentDTO(education.getTitle());
        var abbreviationTitle = getMultilingualContentDTO(education.getAbbreviationTitle());

        dto.setThesisTitle(thesisTitle);
        dto.setTitle(title);
        dto.setAbbreviationTitle(abbreviationTitle);

        return dto;
    }

    public MembershipDTO toDTO(Membership membership) {
        var dto = new MembershipDTO();
        setCommonFields(membership, dto);

        var contributionDescription =
            getMultilingualContentDTO(membership.getContributionDescription());
        var role = getMultilingualContentDTO(membership.getRole());

        dto.setContributionDescription(contributionDescription);
        dto.setRole(role);

        return dto;
    }

    public EmploymentDTO toDTO(Employment employment) {
        var dto = new EmploymentDTO();
        setCommonFields(employment, dto);

        var role = getMultilingualContentDTO(employment.getRole());

        dto.setEmploymentPosition(employment.getEmploymentPosition());
        dto.setRole(role);

        return dto;
    }

    private List<MultilingualContentDTO> getMultilingualContentDTO(
        Set<MultiLingualContent> multilingualContent) {
        return multilingualContent.stream().map(mc ->
            new MultilingualContentDTO(
                mc.getLanguage().getId(),
                mc.getContent(),
                mc.getPriority()
            )).collect(Collectors.toList());
    }

    private void setCommonFields(Involvement involvement, InvolvementDTO dto) {
        var affiliationStatements =
            getMultilingualContentDTO(involvement.getAffiliationStatement());

        dto.setDateFrom(involvement.getDateFrom());
        dto.setDateTo(involvement.getDateTo());
        // TODO: ADD PROOFS
        dto.setInvolvementType(involvement.getInvolvementType());
        dto.setAffiliationStatement(affiliationStatements);
        dto.setOrganisationUnitId(involvement.getOrganisationUnit().getId());
    }
}
