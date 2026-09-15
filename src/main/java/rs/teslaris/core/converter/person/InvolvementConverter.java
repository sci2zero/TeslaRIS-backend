package rs.teslaris.core.converter.person;

import java.util.Objects;
import java.util.stream.Collectors;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.InvolvementDTO;
import rs.teslaris.core.dto.person.involvement.MembershipDTO;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.Membership;
import rs.teslaris.core.model.person.PersonalInfo;
import rs.teslaris.core.util.search.CollectionOperations;

public class InvolvementConverter {

    public static EducationDTO toDTO(Education education) {
        return toDTO(education, false);
    }

    private static EducationDTO toDTO(Education education, boolean forSnapshot) {
        var dto = new EducationDTO();
        setCommonFields(education, dto, forSnapshot);

        var title = MultilingualContentConverter.getMultilingualContentDTO(
            education.getTitle());
        var abbreviationTitle =
            MultilingualContentConverter.getMultilingualContentDTO(
                education.getAbbreviationTitle());

        if (Objects.nonNull(education.getThesis())) {
            // Reading the ID of a lazy reference does not load it, reading its title does.
            dto.setThesisId(education.getThesis().getId());

            if (!forSnapshot) {
                dto.setThesisTitle(MultilingualContentConverter.getMultilingualContentDTO(
                    education.getThesis().getTitle()));
            }
        }

        if (CollectionOperations.containsValues(education.getSupervisors())) {
            education.getSupervisors().forEach(supervisor -> {
                dto.getSupervisorIds().add(supervisor.getId());

                if (!forSnapshot) {
                    dto.getSupervisorNames().add(supervisor.getName().toText());
                }
            });
        } else {
            dto.setDisplaySupervisors(MultilingualContentConverter.getMultilingualContentDTO(
                education.getDisplayThesisSupervisors()));
        }

        dto.setTitle(title);
        dto.setAbbreviationTitle(abbreviationTitle);
        dto.setCourseCode(MultilingualContentConverter.getMultilingualContentDTO(
            education.getCourseCode()));
        dto.setDegreeClassification(MultilingualContentConverter.getMultilingualContentDTO(
            education.getDegreeClassification()));
        dto.setDegreeType(education.getDegreeType());
        dto.setEducationStatus(education.getEducationStatus());

        education.getResearchAreas().forEach(researchArea -> {
            dto.getResearchAreasId().add(researchArea.getId());

            if (!forSnapshot) {
                dto.getResearchAreas().add(ResearchAreaConverter.toDTO(researchArea));
            }
        });

        return dto;
    }

    public static MembershipDTO toDTO(Membership membership) {
        return toDTO(membership, false);
    }

    private static MembershipDTO toDTO(Membership membership, boolean forSnapshot) {
        var dto = new MembershipDTO();
        setCommonFields(membership, dto, forSnapshot);

        var contributionDescription =
            MultilingualContentConverter.getMultilingualContentDTO(
                membership.getContributionDescription());
        var role = MultilingualContentConverter.getMultilingualContentDTO(
            membership.getRole());

        dto.setContributionDescription(contributionDescription);
        dto.setRole(role);
        dto.setMembershipType(membership.getMembershipType());

        return dto;
    }

    public static EmploymentDTO toDTO(Employment employment) {
        return toDTO(employment, false);
    }

    private static EmploymentDTO toDTO(Employment employment, boolean forSnapshot) {
        var dto = new EmploymentDTO();
        setCommonFields(employment, dto, forSnapshot);

        var role = MultilingualContentConverter.getMultilingualContentDTO(
            employment.getRole());

        dto.setEmploymentPosition(employment.getEmploymentPosition());
        dto.setRole(role);

        if (Objects.nonNull(employment.getEmploymentPositionHierarchy())) {
            dto.setEmploymentPositionId(employment.getEmploymentPositionHierarchy().getId());

            if (!forSnapshot) {
                dto.setEmploymentPositionName(
                    MultilingualContentConverter.getMultilingualContentDTO(
                        employment.getEmploymentPositionHierarchy().getName()));
            }
        }

        return dto;
    }


    private static void setCommonFields(Involvement involvement, InvolvementDTO dto,
                                        boolean forSnapshot) {
        var affiliationStatements =
            MultilingualContentConverter.getMultilingualContentDTO(
                involvement.getDisplayOrganisationUnit());

        dto.setId(involvement.getId());
        dto.setDateFrom(involvement.getDateFrom());
        dto.setDateTo(involvement.getDateTo());

        // Proofs are excluded from person revisions, so a snapshot would load them only to have
        // them stripped again during canonicalisation.
        if (!forSnapshot) {
            dto.setProofs(involvement.getProofs().stream()
                .map(DocumentFileConverter::toDTO).collect(
                    Collectors.toList()));
        }

        dto.setInvolvementType(involvement.getInvolvementType());
        dto.setDisplayOrganisationUnit(affiliationStatements);
        dto.setFavorite(involvement.getFavorite());
        dto.setDescription(MultilingualContentConverter.getMultilingualContentDTO(
            involvement.getDescription()));
        dto.setKeywords(MultilingualContentConverter.getMultilingualContentDTO(
            involvement.getKeywords()));
        dto.setPersonBirthDate(
            Objects.requireNonNullElse(involvement.getPersonInvolved().getPersonalInfo(),
                new PersonalInfo()).getLocalBirthDate());

        involvement.getResearchAreas().forEach(researchArea -> {
            dto.getResearchAreasId().add(researchArea.getId());

            if (!forSnapshot) {
                dto.getResearchAreas().add(ResearchAreaConverter.toDTO(researchArea));
            }
        });

        involvement.getHostInstitutions().forEach(organisationUnit -> {
            dto.getHostInstitutionIds().add(organisationUnit.getId());

            if (!forSnapshot) {
                dto.getHostInstitutionNames().add(
                    MultilingualContentConverter.getMultilingualContentDTO(
                        organisationUnit.getName()));
            }
        });

        if (Objects.nonNull(involvement.getUris())) {
            dto.setUris(involvement.getUris());
        }

        if (Objects.nonNull(involvement.getOrganisationUnit())) {
            dto.setOrganisationUnitId(involvement.getOrganisationUnit().getId());
        }

        if (forSnapshot) {
            return;
        }

        dto.setOrganisationUnitName(MultilingualContentConverter.getMultilingualContentDTO(
            Objects.nonNull(involvement.getOrganisationUnit())
                ? involvement.getOrganisationUnit().getName()
                : involvement.getDisplayOrganisationUnit()));
    }

    public static <R extends InvolvementDTO, T extends Involvement> R toDTO(T cast) {
        return dispatch(cast, false);
    }

    /**
     * The involvement as a person revision captures it.
     * <p>
     * Everything a revision keeps is persisted state; the display fields the read endpoints need -
     * proofs, institution and supervisor names, research area hierarchies, the thesis title - are
     * stripped from the snapshot during canonicalisation anyway. Skipping them here is not only
     * cheaper, it avoids loading whole lazy associations for data that is thrown away, which on a
     * person with many involvements is the difference between one query and dozens.
     */
    public static <R extends InvolvementDTO, T extends Involvement> R toSnapshotDTO(T cast) {
        return dispatch(cast, true);
    }

    @SuppressWarnings("unchecked")
    private static <R extends InvolvementDTO, T extends Involvement> R dispatch(
        T cast, boolean forSnapshot) {
        if (cast instanceof Education education) {
            return (R) toDTO(education, forSnapshot);
        } else if (cast instanceof Membership membership) {
            return (R) toDTO(membership, forSnapshot);
        } else if (cast instanceof Employment employment) {
            return (R) toDTO(employment, forSnapshot);
        } else {
            throw new IllegalArgumentException("Unsupported involvement type");
        }
    }
}
