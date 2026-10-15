package rs.teslaris.migrator.converter.hydrator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.model.hydrator.HydratorProjectModel;
import rs.teslaris.migrator.pipeline.RecordExtractor;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.model.project.OrganisationUnitProjectContributionType;
import rs.teslaris.project.model.project.PersonProjectContributionType;
import rs.teslaris.project.model.project.ProjectCollaborationType;
import rs.teslaris.project.model.project.ProjectResearchType;
import rs.teslaris.project.model.project.ProjectStatus;

/**
 * SciPROJ project to {@link ProjectDTO}: flat attributes only (CW-002); relations come later.
 */
@Component
@RequiredArgsConstructor
public class HydratorProjectConverter implements
    RecordExtractor.RecordConverter<HydratorProjectModel.ProjectDocument, ProjectMigrationDTO> {

    // Required but absent from SciPROJ (CW-002-MAP-000016/000035): provisional until team decides
    static final ProjectCollaborationType PROVISIONAL_COLLABORATION_TYPE =
        ProjectCollaborationType.NATIONAL;

    static final ProjectResearchType PROVISIONAL_RESEARCH_TYPE = ProjectResearchType.OTHER;

    private static final Set<String> PROJECT_IDENTIFIER_TYPES = Set.of("ProjectReference", "QREN");

    private static final String ENGLISH = "en";

    // Wikidata property used by SciPROJ for the Ciência ID
    private static final String CIENCIA_ID_TYPE = "Q122584897";

    private final HydratorConversionUtil conversionUtil;

    private final MigrationLog migrationLog;


    @Override
    public ProjectMigrationDTO toDTO(HydratorProjectModel.ProjectDocument record) {
        var project = projectOf(record);
        if (Objects.isNull(project) || isBlank(project.projectId())) {
            return null;
        }

        var key = project.projectId();
        var dto = new ProjectDTO();

        dto.setName(multilingual(project.titles()));
        dto.setDescription(multilingual(project.abstracts()));
        dto.setKeywords(multilingual(project.keywords()));

        // CW-002-MAP-000017
        if (!isBlank(project.acronym())) {
            dto.setNameAbbreviation(conversionUtil.multilingualContent(project.acronym(), null));
        }

        dto.setInternalIdentifiers(identifiers(key, project.identifiers()));
        dto.setResearchAreasId(new HashSet<>(researchAreas(key, project.subjects())));

        String rejection = null;
        try {
            dto.setDateFrom(conversionUtil.isoDate(project.startDate()));
            dto.setDateTo(conversionUtil.isoDate(project.endDate()));
        } catch (InvalidSourceValueException e) {
            rejection = e.getMessage();
        }

        dto.setStatus(provisionalStatus(dto.getDateTo()));
        dto.setCollaborationType(PROVISIONAL_COLLABORATION_TYPE);
        dto.setResearchType(PROVISIONAL_RESEARCH_TYPE);
        dropped(key, HydratorMappingRule.sciproj(16, 35),
            "required field without source, provisional value: status, collaborationType, " +
                "researchType");

        return new ProjectMigrationDTO(key, dto, consortium(key, project.consortium()),
            team(key, project.team()), rejection);
    }

    // CW-002-MAP-000022…000027: order Coordinator → Contractor → Partner, source order per role
    private List<ProjectMigrationDTO.ConsortiumEntry> consortium(
        String key, HydratorProjectModel.Consortium consortium) {
        var entries = new ArrayList<ProjectMigrationDTO.ConsortiumEntry>();

        if (Objects.isNull(consortium)) {
            return entries;
        }

        addMembers(key, entries, consortium.coordinator(),
            OrganisationUnitProjectContributionType.COORDINATOR);
        addMembers(key, entries, consortium.contractor(),
            OrganisationUnitProjectContributionType.CONTRACTOR);
        addMembers(key, entries, consortium.partner(),
            OrganisationUnitProjectContributionType.PARTNER);

        return entries;
    }

    private void addMembers(String key, List<ProjectMigrationDTO.ConsortiumEntry> entries,
                            List<HydratorProjectModel.ConsortiumMember> members,
                            OrganisationUnitProjectContributionType type) {
        if (Objects.isNull(members)) {
            return;
        }

        members.stream()
            .filter(member -> Objects.nonNull(member) && Objects.nonNull(member.orgUnit()))
            .forEach(member -> {
                if (Objects.nonNull(member.orgUnit().amount())) {
                    dropped(key, HydratorMappingRule.sciproj(22), type + " amount not mapped");
                }
                entries.add(new ProjectMigrationDTO.ConsortiumEntry(type,
                    conversionUtil.institution(member.orgUnit()), entries.size() + 1));
            });
    }

    // CW-002-MAP-000028…000031: order PI → Contact → Member, source order within a role
    private List<ProjectMigrationDTO.TeamEntry> team(String key, HydratorProjectModel.Team team) {
        var entries = new ArrayList<ProjectMigrationDTO.TeamEntry>();

        if (Objects.isNull(team)) {
            return entries;
        }

        addTeam(key, entries, team.principalInvestigator(),
            PersonProjectContributionType.PRINCIPLE_INVESTIGATOR);
        addTeam(key, entries, Objects.isNull(team.contact()) ? null : List.of(team.contact()),
            PersonProjectContributionType.CONTACT);
        addTeam(key, entries, team.members(), PersonProjectContributionType.TEAM_MEMBER);

        return entries;
    }

    private void addTeam(String key, List<ProjectMigrationDTO.TeamEntry> entries,
                         List<HydratorProjectModel.TeamMember> members,
                         PersonProjectContributionType type) {
        if (Objects.isNull(members)) {
            return;
        }

        members.stream()
            .filter(member -> Objects.nonNull(member) && Objects.nonNull(member.person()))
            .forEach(member -> {
                var person = member.person();
                var cienciaId = cienciaId(person);
                var orcid = orcid(person);
                var name = isBlank(person.personName()) ? null : person.personName().trim();

                if (Objects.isNull(name) && Objects.isNull(cienciaId) && Objects.isNull(orcid)) {
                    dropped(key, HydratorMappingRule.sciproj(28),
                        type + " without name and identifier");
                    return;
                }

                entries.add(new ProjectMigrationDTO.TeamEntry(type, name, cienciaId, orcid,
                    entries.size() + 1));
            });
    }

    private String cienciaId(HydratorProjectModel.Person person) {
        if (Objects.isNull(person.identifier())) {
            return null;
        }

        return person.identifier().stream()
            .filter(identifier -> Objects.nonNull(identifier) &&
                Objects.nonNull(identifier.type()) &&
                identifier.type().endsWith(CIENCIA_ID_TYPE) && !isBlank(identifier.value()))
            .map(identifier -> identifier.value().trim())
            .findFirst()
            .orElse(null);
    }

    // ORCID sits on the person or nested in an identifier, depending on source element order
    private String orcid(HydratorProjectModel.Person person) {
        var orcid = person.orcid();

        if (isBlank(orcid) && Objects.nonNull(person.identifier())) {
            orcid = person.identifier().stream()
                .filter(identifier -> Objects.nonNull(identifier) &&
                    Objects.nonNull(identifier.item()) && !isBlank(identifier.item().orcid()))
                .map(identifier -> identifier.item().orcid())
                .findFirst()
                .orElse(null);
        }

        return conversionUtil.normaliseOrcid(orcid);
    }

    private HydratorProjectModel.Project projectOf(HydratorProjectModel.ProjectDocument record) {
        if (Objects.isNull(record) || Objects.isNull(record.record()) ||
            Objects.isNull(record.record().metadata())) {
            return null;
        }

        return record.record().metadata().project();
    }

    // CW-002-MAP-000018/000033/000034: one entry per language, in source order
    private List<MultilingualContentDTO> multilingual(List<HydratorProjectModel.LangValue> values) {
        var result = new ArrayList<MultilingualContentDTO>();

        if (Objects.isNull(values)) {
            return result;
        }

        values.stream()
            .filter(value -> Objects.nonNull(value) && !isBlank(value.value()))
            .forEach(value -> conversionUtil.multilingualContent(value.value(), value.language())
                .forEach(content -> {
                    content.setPriority(result.size() + 1);
                    result.add(content);
                }));

        return result;
    }

    // CW-002-MAP-000019; the FundingProgram identifier belongs to the funding
    private Set<String> identifiers(String key, List<HydratorProjectModel.TypedValue> identifiers) {
        var result = new LinkedHashSet<String>();

        if (Objects.isNull(identifiers)) {
            return result;
        }

        identifiers.stream()
            .filter(identifier -> Objects.nonNull(identifier) && !isBlank(identifier.value()))
            .forEach(identifier -> {
                if (PROJECT_IDENTIFIER_TYPES.contains(typeName(identifier.type()))) {
                    result.add(identifier.value().trim());
                } else {
                    dropped(key, HydratorMappingRule.sciproj(19),
                        "identifier type '" + typeName(identifier.type()) + "' not mapped");
                }
            });

        return result;
    }

    // CW-002-MAP-000032: the English label is matched; the Portuguese one is the same field
    private Set<Integer> researchAreas(String key, List<HydratorProjectModel.Subject> subjects) {
        if (Objects.isNull(subjects)) {
            return Set.of();
        }

        var classifications = subjects.stream()
            .filter(subject -> Objects.nonNull(subject) && ENGLISH.equalsIgnoreCase(
                Objects.requireNonNullElse(subject.language(), "").trim()))
            .map(subject -> new HydratorCVModel.ResearchClassification("Subject", subject.value()))
            .toList();

        return conversionUtil.researchAreaIds(classifications,
            reason -> dropped(key, HydratorMappingRule.sciproj(32), reason));
    }

    private ProjectStatus provisionalStatus(LocalDate dateTo) {
        return Objects.nonNull(dateTo) && dateTo.isBefore(LocalDate.now()) ?
            ProjectStatus.CONCLUDED : ProjectStatus.ONGOING;
    }

    private String typeName(String type) {
        return conversionUtil.typeName(type);
    }

    private void dropped(String key, String rule, String reason) {
        migrationLog.valueDropped(HydratorSource.NAME, MigrationEntityType.PROJECT.name(), key,
            rule, reason);
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
