package rs.teslaris.core.converter.person;

import java.util.Objects;
import java.util.stream.Collectors;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.InvolvementDTO;
import rs.teslaris.core.dto.person.involvement.MembershipDTO;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.Membership;

public class InvolvementConverter {

    public static EducationDTO toDTO(Education education) {
        var dto = new EducationDTO();
        setCommonFields(education, dto);

        var thesisTitle =
            MultilingualContentConverter.getMultilingualContentDTO(
                education.getThesisTitle());
        var title = MultilingualContentConverter.getMultilingualContentDTO(
            education.getTitle());
        var abbreviationTitle =
            MultilingualContentConverter.getMultilingualContentDTO(
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
            MultilingualContentConverter.getMultilingualContentDTO(
                membership.getContributionDescription());
        var role = MultilingualContentConverter.getMultilingualContentDTO(
            membership.getRole());

        dto.setContributionDescription(contributionDescription);
        dto.setRole(role);

        return dto;
    }

    public static EmploymentDTO toDTO(Employment employment) {
        var dto = new EmploymentDTO();
        setCommonFields(employment, dto);

        var role = MultilingualContentConverter.getMultilingualContentDTO(
            employment.getRole());

        dto.setEmploymentPosition(employment.getEmploymentPosition());
        dto.setRole(role);

        return dto;
    }


    private static void setCommonFields(Involvement involvement, InvolvementDTO dto) {
        var affiliationStatements =
            MultilingualContentConverter.getMultilingualContentDTO(
                involvement.getAffiliationStatement());

        dto.setId(involvement.getId());
        dto.setDateFrom(involvement.getDateFrom());
        dto.setDateTo(involvement.getDateTo());
        dto.setProofs(involvement.getProofs().stream()
            .map(DocumentFileConverter::toDTO).collect(
                Collectors.toList()));
        dto.setInvolvementType(involvement.getInvolvementType());
        dto.setAffiliationStatement(affiliationStatements);

        if (Objects.nonNull(involvement.getOrganisationUnit())) {
            dto.setOrganisationUnitId(involvement.getOrganisationUnit().getId());
            dto.setOrganisationUnitName(MultilingualContentConverter.getMultilingualContentDTO(
                involvement.getOrganisationUnit().getName()));
        } else {
            dto.setOrganisationUnitName(MultilingualContentConverter.getMultilingualContentDTO(
                involvement.getAffiliationStatement()));
        }
    }

    public static <R extends InvolvementDTO, T extends Involvement> R toDTO(T cast) {
        if (cast instanceof Education) {
            return (R) toDTO((Education) cast);
        } else if (cast instanceof Membership) {
            return (R) toDTO((Membership) cast);
        } else if (cast instanceof Employment) {
            return (R) toDTO((Employment) cast);
        } else {
            throw new IllegalArgumentException("Unsupported involvement type");
        }
    }
}
