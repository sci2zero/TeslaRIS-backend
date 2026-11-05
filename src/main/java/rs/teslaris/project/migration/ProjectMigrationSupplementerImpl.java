package rs.teslaris.project.migration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.project.PersonProjectContributionDTO;
import rs.teslaris.core.dto.project.ProjectDTO;
import rs.teslaris.core.migration.ProjectMigrationSupplementer;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.project.model.project.PersonProjectContribution;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.repository.project.ProjectRepository;
import rs.teslaris.project.service.interfaces.project.PersonProjectContributionService;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@Service
@RequiredArgsConstructor
public class ProjectMigrationSupplementerImpl implements ProjectMigrationSupplementer {

    private final ProjectRepository projectRepository;

    private final ProjectService projectService;

    private final PersonProjectContributionService personProjectContributionService;

    private final MultilingualContentService multilingualContentService;

    private final ResearchAreaService researchAreaService;


    @Override
    @Transactional
    public List<String> supplement(Integer projectId, ProjectDTO source,
                                   List<PersonProjectContributionDTO> team) {
        var project = projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project " + projectId + " not found."));

        var dropped = new ArrayList<String>();
        supplementProject(project, source);
        supplementTeam(project, team, dropped);
        projectRepository.save(project);
        projectService.refreshProjectIndex(projectId);

        return dropped;
    }

    private void supplementProject(Project project, ProjectDTO source) {
        if (project.getDescription().isEmpty()) {
            project.setDescription(
                multilingualContentService.getMultilingualContent(source.getDescription()));
        }
        if (project.getKeywords().isEmpty()) {
            project.setKeywords(
                multilingualContentService.getMultilingualContent(source.getKeywords()));
        }
        if (project.getResearchAreas().isEmpty() && !source.getResearchAreasId().isEmpty()) {
            project.setResearchAreas(new HashSet<>(researchAreaService.getResearchAreasByIds(
                source.getResearchAreasId().stream().toList())));
        }
        if (Objects.isNull(project.getDateFrom())) {
            project.setDateFrom(source.getDateFrom());
        }
        if (Objects.isNull(project.getDateTo())) {
            project.setDateTo(source.getDateTo());
        }

        // National id is unique; it can only be missing here when the match was by QREN. Core
        // stores a missing one as an empty string
        if (!StringUtil.valueExists(project.getNationalId()) &&
            StringUtil.valueExists(source.getNationalId()) &&
            !projectRepository.existsByNationalId(source.getNationalId(), project.getId())) {
            project.setNationalId(source.getNationalId());
        }
        project.getInternalIdentifiers().addAll(source.getInternalIdentifiers());
    }

    // A person already in the team gets the missing role and dates; others are added at the end
    private void supplementTeam(Project project, List<PersonProjectContributionDTO> team,
                                List<String> dropped) {
        var nextOrderNumber = project.getPersons().stream()
            .map(PersonProjectContribution::getOrderNumber)
            .filter(Objects::nonNull)
            .max(Integer::compare)
            .orElse(0) + 1;

        for (var member : team) {
            var existing = existingContribution(project, member);

            if (existing.isPresent()) {
                supplementMember(existing.get(), member, dropped);
                continue;
            }

            member.setOrderNumber(nextOrderNumber++);
            project.getPersons().add(
                personProjectContributionService.createContribution(member, project));
        }
    }

    private void supplementMember(PersonProjectContribution contribution,
                                  PersonProjectContributionDTO member, List<String> dropped) {
        if (Objects.nonNull(member.getContributionType()) &&
            member.getContributionType() != contribution.getContributionType()) {
            dropped.add("contribution type " + member.getContributionType() + " of contribution " +
                contribution.getId() + " not set: it already has " +
                contribution.getContributionType());
        }

        if (Objects.isNull(contribution.getInvestigationRole())) {
            contribution.setInvestigationRole(member.getInvestigationRole());
        } else if (Objects.nonNull(member.getInvestigationRole()) &&
            member.getInvestigationRole() != contribution.getInvestigationRole()) {
            dropped.add("investigation role " + member.getInvestigationRole() + " of contribution " +
                contribution.getId() + " not set: it already has " +
                contribution.getInvestigationRole());
        }

        if (Objects.isNull(contribution.getDateFrom())) {
            contribution.setDateFrom(member.getDateFrom());
        }
        if (Objects.isNull(contribution.getDateTo())) {
            contribution.setDateTo(member.getDateTo());
        }
    }

    // Linked persons are matched by id, name-only ones by their normalised display name
    private Optional<PersonProjectContribution> existingContribution(
        Project project, PersonProjectContributionDTO member) {
        if (Objects.nonNull(member.getPersonId())) {
            return project.getPersons().stream()
                .filter(contribution -> Objects.nonNull(contribution.getPerson()) &&
                    member.getPersonId().equals(contribution.getPerson().getId()))
                .findFirst();
        }

        var name = Objects.isNull(member.getPersonName()) ? null :
            member.getPersonName().getLastname();
        if (Objects.isNull(name)) {
            return Optional.empty();
        }

        var key = normalise(name);
        return project.getPersons().stream()
            .filter(contribution -> Objects.isNull(contribution.getPerson()) &&
                Objects.nonNull(contribution.getAffiliationStatement()) &&
                key.equals(nameKey(contribution.getAffiliationStatement().getDisplayPersonName())))
            .findFirst();
    }

    private String nameKey(PersonName name) {
        if (Objects.isNull(name)) {
            return null;
        }

        var joined = Stream.of(name.getFirstname(), name.getOtherName(), name.getLastname())
            .filter(part -> Objects.nonNull(part) && !part.isBlank())
            .collect(Collectors.joining(" "));
        return joined.isEmpty() ? null : normalise(joined);
    }

    private String normalise(String value) {
        return value.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
