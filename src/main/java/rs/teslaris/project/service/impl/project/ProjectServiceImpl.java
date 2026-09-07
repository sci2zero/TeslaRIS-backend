package rs.teslaris.project.service.impl.project;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.model.document.OrganisationUnitContribution;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.*;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.DateRangeException;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.project.converter.project.OrganisationUnitProjectContributionConverter;
import rs.teslaris.project.converter.project.PersonProjectContributionConverter;
import rs.teslaris.project.converter.project.ProjectConverter;
import rs.teslaris.project.converter.project.ProjectsRelationConverter;
import rs.teslaris.project.dto.project.OrganisationUnitProjectContributionDTO;
import rs.teslaris.project.dto.project.PersonProjectContributionDTO;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.dto.project.ProjectsRelationDTO;
import rs.teslaris.project.indexmodel.project.ProjectIndex;
import rs.teslaris.project.indexrepository.project.ProjectIndexRepository;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.model.project.ProjectStatus;
import rs.teslaris.project.repository.project.ProjectDocumentRepository;
import rs.teslaris.project.repository.project.ProjectEventRepository;
import rs.teslaris.project.repository.project.ProjectRepository;
import rs.teslaris.project.service.interfaces.project.OrganisationUnitProjectContributionService;
import rs.teslaris.project.service.interfaces.project.PersonProjectContributionService;
import rs.teslaris.project.service.interfaces.project.ProjectService;
import rs.teslaris.project.service.interfaces.project.ProjectsRelationService;

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

    private final OrganisationUnitProjectContributionService
        organisationUnitProjectContributionService;

    private final ProjectDocumentRepository projectDocumentRepository;

    private final IndexBulkUpdateService indexBulkUpdateService;

    private final ProjectEventRepository projectEventRepository;

    private final PersonProjectContributionService personProjectContributionService;

    private final ProjectsRelationService projectsRelationService;

    private final OrganisationUnitService organisationUnitService;

    @Override
    protected JpaRepository<Project, Integer> getEntityRepository() {
        return projectRepository;
    }

    @Override
    public Page<ProjectIndex> searchProjects(List<String> tokens,
                                             LocalDate dateFrom,
                                             LocalDate dateTo,
                                             boolean onlyActive,
                                             boolean onlyWithoutContributions,
                                             List<ProjectStatus> allowedStatuses,
                                             Pageable pageable) {
        return searchService.runQuery(buildSimpleSearchQuery(tokens, dateFrom, dateTo, onlyActive, onlyWithoutContributions, allowedStatuses),
            pageable, ProjectIndex.class, "project");
    }

    @Override
    public Page<ProjectIndex> findProjectsForPerson(Integer personId, List<String> tokens,
                                                    boolean onlyActive,
                                                    List<ProjectStatus> allowedStatuses,
                                                    Pageable pageable) {
        var contributionFilter = TermQuery.of(t -> t
            .field("person_ids")
            .value(personId)
        )._toQuery();

        return runContributorFilteredQuery(tokens, onlyActive, allowedStatuses, contributionFilter,
            pageable);
    }

    @Override
    public Page<ProjectIndex> findProjectsForOrganisationUnit(Integer organisationUnitId,
                                                              List<String> tokens,
                                                              boolean onlyActive,
                                                              List<ProjectStatus> allowedStatuses,
                                                              Pageable pageable) {

        var organisationUnitIds =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);

        var contributionFilter = TermsQuery.of(t -> t
            .field("organisation_unit_ids")
            .terms(v -> v.value(organisationUnitIds.stream()
                .map(String::valueOf)
                .map(FieldValue::of)
                .toList()))
        )._toQuery();

        return runContributorFilteredQuery(tokens, onlyActive, allowedStatuses, contributionFilter,
            pageable);
    }

    private Page<ProjectIndex> runContributorFilteredQuery(List<String> tokens,
                                                           boolean onlyActive,
                                                           List<ProjectStatus> allowedStatuses,
                                                           Query contributionFilter,
                                                           Pageable pageable) {
        var searchTokens =
            (Objects.isNull(tokens) || tokens.isEmpty()) ? List.of("*") : tokens;

        var combinedQuery = BoolQuery.of(bq -> bq
            .must(buildSimpleSearchQuery(searchTokens, null, null, onlyActive, false,
                allowedStatuses))
            .must(contributionFilter)
        )._toQuery();

        return searchService.runQuery(combinedQuery, pageable, ProjectIndex.class, "project");
    }

    @Override
    @Transactional
    public Long getProjectCount() {
        return projectIndexRepository.count();
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

        buildCollections(savedProject, projectDTO);

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

        var index = projectIndexRepository.findProjectIndexByDatabaseId(projectId);
        index.ifPresent(projectIndexRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public CompletableFuture<Void> reindexProject() {
        projectIndexRepository.deleteAll();

        FunctionalUtil.processAllPages(
            100,
            Sort.by(Sort.Direction.ASC, "id"),
            this::findAll,
            project -> {
                indexProject(project, new ProjectIndex());
                indexProjectDocuments(project.getId());
                indexProjectEvents(project.getId());
            }
        );

        return CompletableFuture.completedFuture(null);
    }

    @Override
    @Transactional
    public void indexProject(Project project, ProjectIndex index) {
        indexCommonFields(project, index);
        projectIndexRepository.save(index);
    }

    private void refreshProjectIndex(Project project) {
        var index = projectIndexRepository.findProjectIndexByDatabaseId(project.getId())
            .orElseGet(ProjectIndex::new);

        indexProject(project, index);
    }

    private void indexProjectDocuments(Integer projectId) {
        List<Integer> documentIds = projectDocumentRepository.findDocumentIdsByProjectId(projectId);
        documentIds.forEach(documentId ->
            indexBulkUpdateService.setIdFieldForRecord(
                "document_publication",
                "databaseId",
                documentId,
                "project_id",
                projectId
            )
        );
    }

    private void indexProjectEvents(Integer projectId) {
        List<Integer> eventIds = projectEventRepository.findEventIdsByProjectId(projectId);
        eventIds.forEach(eventId ->
            indexBulkUpdateService.setIdFieldForRecord(
                "events",
                "databaseId",
                eventId,
                "project_id",
                projectId
            )
        );
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

        project.setUris(projectDTO.getUris());
        project.setDoi(projectDTO.getDoi());
        project.setRaid(projectDTO.getRaid());
        project.setNationalId(projectDTO.getNationalId());
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
    }

    private void clearCommonFields(Project project) {
        project.getName().clear();
        project.getDescription().clear();
        project.getNameAbbreviation().clear();
        project.getKeywords().clear();
        project.getResearchAreas().clear();
    }

    private void buildCollections(Project project, ProjectDTO projectDTO) {
        if (Objects.isNull(project.getPersons())) {
            project.setPersons(new HashSet<>());
        }

        projectDTO.getPersons().forEach(personDto -> project.getPersons().add(
                personProjectContributionService.createContribution(personDto, project)));

        if (Objects.isNull(project.getOrganisations())) {
            project.setOrganisations(new HashSet<>());
        }

        projectDTO.getOrganisations().forEach(organisationDto -> {
            project.getOrganisations().add(organisationUnitProjectContributionService.createContribution(organisationDto, project));
        });

        if (Objects.isNull(project.getRelatedProjects())) {
            project.setRelatedProjects(new HashSet<>());
        }

        projectDTO.getRelations().forEach(relationDto -> {
            project.getRelatedProjects().add(projectsRelationService.createRelation(relationDto, project));
        });
    }

    @Override
    @Transactional
    public PersonProjectContributionDTO addPerson(Integer projectId, PersonProjectContributionDTO personDto) {
        var project = findOne(projectId);
        var person = personProjectContributionService.createContribution(personDto, project);

        project.getPersons().add(person);
        save(project);
        refreshProjectIndex(project);

        return PersonProjectContributionConverter.toDTO(person);
    }

    @Override
    @Transactional
    public void removePerson(Integer projectId, Integer personId) {
        var project = findOne(projectId);
        var person = personProjectContributionService.findOne(personId);

        project.getPersons().remove(person);
        save(project);
        refreshProjectIndex(project);
    }

    @Override
    @Transactional
    public OrganisationUnitProjectContributionDTO addOrganisation(Integer projectId, OrganisationUnitProjectContributionDTO organisationDto) {
        var project = findOne(projectId);
        var organisation = organisationUnitProjectContributionService.createContribution(organisationDto, project);

        project.getOrganisations().add(organisation);
        save(project);
        refreshProjectIndex(project);

        return OrganisationUnitProjectContributionConverter.toDTO(organisation);
    }

    @Override
    @Transactional
    public void removeOrganisation(Integer projectId, Integer organisationId) {
        var project = findOne(projectId);
        var organisation = organisationUnitProjectContributionService.findOne(organisationId);

        project.getOrganisations().remove(organisation);
        save(project);
        refreshProjectIndex(project);
    }

    @Override
    @Transactional
    public ProjectsRelationDTO addProjectRelation(Integer projectId, ProjectsRelationDTO relationDto) {
        var project = findOne(projectId);
        var relation = projectsRelationService.createRelation(relationDto, project);

        project.getRelatedProjects().add(relation);
        save(project);

        return ProjectsRelationConverter.toDTO(relation);
    }

    @Override
    @Transactional
    public void removeProjectRelation(Integer projectId, Integer relationId) {
        var project = findOne(projectId);
        var relation = projectsRelationService.findOne(relationId);

        project.getRelatedProjects().remove(relation);
        save(project);
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
        index.setStatus(project.getStatus());
        index.setHasContributions(project.hasContributions());

        indexContributorIds(project, index);
        indexCoordinatorFields(project, index);

        return index;
    }

    private void indexContributorIds(Project project, ProjectIndex index) {
        index.setPersonIds(project.getPersons().stream()
                .map(PersonContribution::getPerson)
                .filter(Objects::nonNull)
                .map(Person::getId)
                .toList());

        index.setOrganisationUnitIds(project.getOrganisations().stream()
                .map(OrganisationUnitContribution::getOrganisationUnit)
                .filter(Objects::nonNull)
                .map(OrganisationUnit::getId)
                .toList());
    }

    private void indexCoordinatorFields(Project project, ProjectIndex index) {
        var coordinator = project.getCoordinator()
                .map(OrganisationUnitProjectContribution::getOrganisationUnit)
                .orElse(null);

        if (Objects.isNull(coordinator)) {
            index.setCoordinatorNameSr("");
            index.setCoordinatorNameSrSortable("");
            index.setCoordinatorNameOther("");
            index.setCoordinatorNameOtherSortable("");
            index.setCoordinatorId(null);
            return;
        }

        var srContent = new StringBuilder();
        var otherContent = new StringBuilder();

        multilingualContentService.buildLanguageStrings(srContent, otherContent,
                coordinator.getName(), true);

        if (srContent.isEmpty() && !otherContent.isEmpty()) {
            srContent.append(otherContent);
        } else if (!srContent.isEmpty() && otherContent.isEmpty()) {
            otherContent.append(srContent);
        }

        StringUtil.removeTrailingDelimiters(srContent, otherContent);

        index.setCoordinatorNameSr(
                !srContent.isEmpty() ? srContent.toString() : otherContent.toString());
        index.setCoordinatorNameSrSortable(index.getCoordinatorNameSr());
        index.setCoordinatorNameOther(
                !otherContent.isEmpty() ? otherContent.toString() : srContent.toString());
        index.setCoordinatorNameOtherSortable(index.getCoordinatorNameOther());
        index.setCoordinatorId(coordinator.getId());
    }

    private Query buildSimpleSearchQuery(List<String> tokens,
                                         LocalDate dateFrom,
                                         LocalDate dateTo,
                                         boolean onlyActive,
                                         boolean onlyWithoutContributions,
                                         List<ProjectStatus> allowedStatuses) {
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
                                        .should(sb -> sb.matchPhrase(
                                            mq -> mq.field("coordinator_name_sr")
                                                .query(token.replace("\"", ""))))
                                        .should(sb -> sb.matchPhrase(
                                            mq -> mq.field("coordinator_name_other")
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
                                    .should(sb -> sb.wildcard(
                                        mq -> mq.field("coordinator_name_sr")
                                            .value(StringUtil.performSimpleLatinPreprocessing(
                                                wildcard) + "*")
                                            .caseInsensitive(true)))
                                    .should(sb -> sb.wildcard(
                                        mq -> mq.field("coordinator_name_other")
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
                                    .should(sb -> sb.wildcard(
                                        mq -> mq.field("coordinator_name_sr")
                                            .value(
                                                StringUtil.performSimpleLatinPreprocessing(token) +
                                                    "*")
                                            .caseInsensitive(true)))
                                    .should(sb -> sb.wildcard(
                                        mq -> mq.field("coordinator_name_other")
                                            .value(wildcard)
                                            .caseInsensitive(true)))
                                    .should(sb -> sb.match(
                                        mq -> mq.field("coordinator_name_sr")
                                            .query(token)))
                                    .should(sb -> sb.match(
                                        mq -> mq.field("coordinator_name_other")
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

            if (onlyActive) {
                var today = LocalDate.now().toString();
                b.must(sb -> sb.bool(activeBool -> activeBool
                    .must(m -> m.range(r -> r.field("date_from").lte(JsonData.of(today))))
                    .must(m -> m.range(r -> r.field("date_to").gte(JsonData.of(today))))
                ));
            }

            if (onlyWithoutContributions) {
                b.filter(sb -> sb.term(t -> t
                    .field("has_contributions")
                    .value(false)
                ));
            }

            if (Objects.nonNull(allowedStatuses) && !allowedStatuses.isEmpty()) {
                b.filter(sb -> sb.terms(t -> t
                    .field("status")
                    .terms(tv -> tv.value(
                        allowedStatuses.stream()
                            .map(status -> FieldValue.of(status.name()))
                            .toList()
                    ))
                ));
            }

            return b;
        })))._toQuery();
    }
}
