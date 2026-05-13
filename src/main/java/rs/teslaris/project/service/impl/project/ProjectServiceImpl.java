package rs.teslaris.project.service.impl.project;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.util.exceptionhandling.exception.DateRangeException;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.project.converter.project.ProjectConverter;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.indexmodel.project.ProjectIndex;
import rs.teslaris.project.indexrepository.project.ProjectIndexRepository;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.repository.project.ProjectRepository;
import rs.teslaris.project.service.interfaces.project.OrganisationUnitProjectContributionService;
import rs.teslaris.project.service.interfaces.project.PersonProjectContributionService;
import rs.teslaris.project.service.interfaces.project.ProjectService;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl extends JPAServiceImpl<Project> implements ProjectService {

    private final ProjectRepository projectRepository;

    private final MultilingualContentService multilingualContentService;

    private final ResearchAreaService researchAreaService;

    private final CurrencyService currencyService;

    private final ProjectIndexRepository projectIndexRepository;

    private final SearchService<ProjectIndex> searchService;

    private final OrganisationUnitProjectContributionService organisationUnitProjectContributionService;

    private final PersonProjectContributionService personProjectContributionService;

    @Override
    protected JpaRepository<Project, Integer> getEntityRepository() {
        return projectRepository;
    }

    @Override
    public Page<ProjectIndex> searchProjects(List<String> tokens, LocalDate dateFrom,
                                                     LocalDate dateTo, Pageable pageable) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens, dateFrom, dateTo),
                pageable, ProjectIndex.class, "project");
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDTO readProject(Integer projectId) {
        return ProjectConverter.toDTO(findOne(projectId));
    }

    @Override
    @Transactional
    public Project createProject(ProjectDTO projectDTO) {
        var newProject = new Project();

        setCommonFields(newProject, projectDTO);

        var savedProject = save(newProject);

        projectIndexRepository.save(
                indexCommonFields(savedProject, new ProjectIndex()));

        return savedProject;
    }

    @Override
    @Transactional
    public void updateProject(Integer projectId,
                                  ProjectDTO projectDTO) {
        var projectToUpdate = findOne(projectId);

        clearCommonFields(projectToUpdate);
        setCommonFields(projectToUpdate, projectDTO);

        projectIndexRepository.findProjectIndexByDatabaseId(projectId)
                .ifPresent(index -> {
                    indexCommonFields(projectToUpdate, index);
                    projectIndexRepository.save(index);
                });
    }

    @Override
    @Transactional
    public void deleteProject(Integer projectId) {
        delete(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Void> reindexProject() {
        FunctionalUtil.processAllPages(
                100,
                Sort.by(Sort.Direction.ASC, "id"),
                this::findAll,
                project -> indexProject(project, new ProjectIndex())
        );

        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Transactional(readOnly = true)
    public void indexProject(Project project, ProjectIndex index) {
        indexCommonFields(project, index);
        projectIndexRepository.save(index);
    }

    private void setCommonFields(Project project, ProjectDTO projectDTO) {
        if (Objects.nonNull(projectDTO.getDateFrom()) &&
                Objects.nonNull(projectDTO.getDateTo()) &&
                projectDTO.getDateTo().isBefore(projectDTO.getDateFrom())) {
            throw new DateRangeException(
                    "Project must start before it ends.");
        }

        project.setName(
                multilingualContentService.getMultilingualContent(projectDTO.getName()));
        project.setDescription(
                multilingualContentService.getMultilingualContent(projectDTO.getDescription()));
        project.setNameAbbreviation(
                multilingualContentService.getMultilingualContent(projectDTO.getNameAbbreviation()));
        project.setKeywords(
                multilingualContentService.getMultilingualContent(projectDTO.getKeywords()));

        var researchAreas = researchAreaService.getResearchAreasByIds(
                projectDTO.getResearchAreasId().stream().toList());
        project.setResearchAreas(new HashSet<>(researchAreas));

        var consortium = organisationUnitProjectContributionService.getOrganisationUnitsByIds(
                projectDTO.getConsortiumIds().stream().toList());
        project.setConsortium(new HashSet<>(consortium));

        project.setUris(projectDTO.getUris());
        project.setDoi(projectDTO.getDoi());
        project.setRaid(projectDTO.getRaid());
        project.setDateFrom(projectDTO.getDateFrom());
        project.setDateTo(projectDTO.getDateTo());
        project.setStatus(projectDTO.getStatus());
        project.setCollaborationType(projectDTO.getCollaborationType());
        project.setResearchType(projectDTO.getResearchType());
        project.setNotFunded(projectDTO.getNotFunded());
        project.setInternalIdentifiers(projectDTO.getInternalIdentifiers());

        if (Objects.nonNull(projectDTO.getCosts())) {
            if (Objects.isNull(project.getCosts())) {
                project.setCosts(new MonetaryAmount());
            }
            project.getCosts().setCurrency(
                    currencyService.findOne(projectDTO.getCosts().getCurrencyId()));
            project.getCosts().setAmount(projectDTO.getCosts().getAmount());
        } else {
            project.setCosts(null);
        }

        rebuildTeam(project, projectDTO);
    }

    private void clearCommonFields(Project project) {
        project.getName().clear();
        project.getDescription().clear();
        project.getNameAbbreviation().clear();
        project.getKeywords().clear();
        project.getResearchAreas().clear();
        project.getTeam().clear();
    }

    private void rebuildTeam(Project project, ProjectDTO projectDTO) {
        if (Objects.isNull(project.getTeam())) {
            project.setTeam(new HashSet<>());
        }
        projectDTO.getTeam().forEach(memberDto ->
                project.getTeam().add(
                        personProjectContributionService.createContribution(memberDto, project)));
    }

    private ProjectIndex indexCommonFields(Project project, ProjectIndex index) {
        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
                project.getName(), true);

        if (srContent.isEmpty() && !otherContent.isEmpty()) {
            srContent.append(otherContent);
        } else if (!srContent.isEmpty() && otherContent.isEmpty()) {
            otherContent.append(srContent);
        }

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
                project.getNameAbbreviation(), false);

        StringUtil.removeTrailingDelimiters(srContent, otherContent);
        index.setNameSr(!srContent.isEmpty() ? srContent.toString() : otherContent.toString());
        index.setNameSrSortable(index.getNameSr());
        index.setNameOther(
                !otherContent.isEmpty() ? otherContent.toString() : srContent.toString());
        index.setNameOtherSortable(index.getNameOther());

        index.setDateFrom(project.getDateFrom());
        index.setDateTo(project.getDateTo());
        index.setDatabaseId(project.getId());

        return index;
    }

    private Query buildSimpleSearchQuery(List<String> tokens, LocalDate dateFrom,
                                         LocalDate dateTo) {
        var minShouldMatch = (Objects.isNull(tokens) || tokens.isEmpty())
                ? 0
                : (int) Math.ceil(tokens.size() * 0.8);

        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            if (Objects.nonNull(tokens) && !tokens.isEmpty()) {
                b.must(bq -> {
                    bq.bool(eq -> {
                        tokens.forEach(token -> {
                            if (token.startsWith("\"") && token.endsWith("\"")) {
                                eq.must(mp ->
                                        mp.bool(m -> m
                                                .should(sb -> sb.matchPhrase(
                                                        mq -> mq.field("name_sr")
                                                                .query(token.replace("\"", ""))))
                                                .should(sb -> sb.matchPhrase(
                                                        mq -> mq.field("name_other")
                                                                .query(token.replace("\"", ""))))
                                        )
                                );
                            } else if (token.endsWith("*")) {
                                var wildcard = token.replace("*", "").replace(".", "");

                                eq.should(mp -> mp.bool(m -> m
                                        .should(sb -> sb.wildcard(
                                                mq -> mq.field("name_sr")
                                                        .value(StringUtil.performSimpleLatinPreprocessing(
                                                                wildcard) + "*")
                                                        .caseInsensitive(true)))
                                        .should(sb -> sb.wildcard(
                                                mq -> mq.field("name_other")
                                                        .value(wildcard + "*")
                                                        .caseInsensitive(true)))
                                ));
                            } else {
                                var wildcard = token + "*";

                                eq.should(mp -> mp.bool(m -> m
                                        .should(sb -> sb.wildcard(
                                                mq -> mq.field("name_sr")
                                                        .value(
                                                                StringUtil.performSimpleLatinPreprocessing(token) +
                                                                        "*")
                                                        .caseInsensitive(true)))
                                        .should(sb -> sb.wildcard(
                                                mq -> mq.field("name_other")
                                                        .value(wildcard)
                                                        .caseInsensitive(true)))
                                        .should(sb -> sb.match(
                                                mq -> mq.field("name_sr")
                                                        .query(token)))
                                        .should(sb -> sb.match(
                                                mq -> mq.field("name_other")
                                                        .query(token)))
                                ));
                            }
                        });

                        return eq.minimumShouldMatch(Integer.toString(minShouldMatch));
                    });
                    return bq;
                });
            }

            if (Objects.nonNull(dateFrom) || Objects.nonNull(dateTo)) {
                b.must(sb -> sb.bool(dateBool -> {
                    if (Objects.nonNull(dateFrom)) {
                        dateBool.must(m -> m.range(r ->
                                r.field("date_from")
                                        .gte(JsonData.of(dateFrom.toString()))
                        ));
                    }
                    if (Objects.nonNull(dateTo)) {
                        dateBool.must(m -> m.range(r ->
                                r.field("date_to")
                                        .lte(JsonData.of(dateTo.toString()))
                        ));
                    }
                    return dateBool;
                }));
            }

            return b;
        })))._toQuery();
    }
}
