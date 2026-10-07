package rs.teslaris.migrator.configuration.hydrator;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.migrator.client.fetcher.HttpPagedFetcher;
import rs.teslaris.migrator.client.hydrator.HydratorCVClient;
import rs.teslaris.migrator.client.hydrator.HydratorProjectClient;
import rs.teslaris.migrator.configuration.MigrationSourceProperties;
import rs.teslaris.migrator.converter.hydrator.EmploymentEntityCreator;
import rs.teslaris.migrator.converter.hydrator.FundingEntityCreator;
import rs.teslaris.migrator.converter.hydrator.HydratorEmploymentExtractor;
import rs.teslaris.migrator.converter.hydrator.HydratorFundingExtractor;
import rs.teslaris.migrator.converter.hydrator.HydratorOrganisationUnitExtractor;
import rs.teslaris.migrator.converter.hydrator.HydratorOutputRouter;
import rs.teslaris.migrator.converter.hydrator.HydratorPersonConverter;
import rs.teslaris.migrator.converter.hydrator.HydratorPrizeExtractor;
import rs.teslaris.migrator.converter.hydrator.HydratorProjectConverter;
import rs.teslaris.migrator.converter.hydrator.HydratorSource;
import rs.teslaris.migrator.converter.hydrator.PrizeEntityCreator;
import rs.teslaris.migrator.converter.hydrator.ProjectEntityCreator;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.model.hydrator.HydratorProjectModel;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.pipeline.FailureHandler;
import rs.teslaris.migrator.pipeline.ItemRouter;
import rs.teslaris.migrator.pipeline.MigrationPipeline;
import rs.teslaris.migrator.pipeline.RecordExtractor;
import rs.teslaris.migrator.pipeline.SimpleMapping;
import rs.teslaris.migrator.pipeline.failure.PersonMergeFailureHandler;
import rs.teslaris.migrator.util.MigrationEntityType;

/**
 * Registration of the hydrator pipelines. Everything source-specific about this migration is either
 * here or in the converters - the runner, registry, retry, logging and reporting are shared.
 * <p>
 * Passes, in order:
 * <ol>
 *     <li>{@code ORGANISATION_UNIT};</li>
 *     <li>{@code PERSON} - each person is followed by its prizes and employments in the same
 *     traversal; a request for {@code PERSON_PRIZE} or {@code PERSON_EMPLOYMENT} alone runs this
 *     pipeline filtered to that type;</li>
 *     <li>{@code PROJECT} - SciPROJ projects (flat attributes), each followed by its fundings;</li>
 *     <li>{@code DOCUMENT} - runs after persons exist, so contributions can resolve.</li>
 * </ol>
 */
@Configuration
@RequiredArgsConstructor
public class HydratorPipelineConfiguration {

    private final HydratorCVClient cvClient;

    private final HydratorProjectClient projectClient;

    private final HydratorProjectConverter projectConverter;

    private final ProjectEntityCreator projectEntityCreator;

    private final HydratorFundingExtractor fundingExtractor;

    private final FundingEntityCreator fundingEntityCreator;

    private final MigrationSourceProperties properties;

    private final HydratorOrganisationUnitExtractor organisationUnitExtractor;

    private final HydratorPersonConverter personConverter;

    private final HydratorEmploymentExtractor employmentExtractor;

    private final HydratorPrizeExtractor prizeExtractor;

    private final HydratorOutputRouter outputRouter;

    private final EmploymentEntityCreator employmentEntityCreator;

    private final PrizeEntityCreator prizeEntityCreator;

    private final PersonMergeFailureHandler personMergeFailureHandler;

    private final OrganisationUnitService organisationUnitService;

    private final PersonService personService;

    /**
     * Convenience list documenting the intended pass order for a full migration.
     */
    public static List<MigrationEntityType> passOrder() {
        return List.of(
            MigrationEntityType.ORGANISATION_UNIT,
            MigrationEntityType.PERSON,
            MigrationEntityType.PROJECT,
            MigrationEntityType.DOCUMENT
        );
    }

    @Bean
    public MigrationPipeline<HydratorCVModel.Curriculum> hydratorOrganisationUnitPipeline() {
        return pipeline(MigrationEntityType.ORGANISATION_UNIT, organisationUnitRouter());
    }

    @Bean
    public MigrationPipeline<HydratorCVModel.Curriculum> hydratorPersonPipeline() {
        return pipeline(MigrationEntityType.PERSON, personRouter());
    }

    @Bean
    public MigrationPipeline<HydratorProjectModel.ProjectDocument> hydratorProjectPipeline() {
        var sourceProperties = properties.forSource(HydratorSource.NAME);

        return new MigrationPipeline<>(
            HydratorSource.NAME,
            MigrationEntityType.PROJECT,
            HydratorProjectModel.ProjectDocument.class,
            new HttpPagedFetcher<>((page, size) -> projectClient.getProjects(
                null, page, size, HydratorSource.PROJECTS_SORT)),
            ItemRouter.ordered(List.of(
                new SimpleMapping<>(
                    MigrationEntityType.PROJECT,
                    RecordExtractor.of(projectConverter),
                    projectEntityCreator,
                    FailureHandler.noOp(),
                    projectConverter::keyOf),
                new SimpleMapping<>(
                    MigrationEntityType.PROJECT_FUNDING,
                    fundingExtractor,
                    fundingEntityCreator,
                    FailureHandler.noOp(),
                    fundingExtractor::keyOf))),
            sourceProperties.getRetry().toPolicy(),
            HydratorProjectModel.ProjectDocument::id,
            sourceProperties.batchSizeOrDefault(properties.getDefaultBatchSize()));
    }

    /**
     * Documents fall back to this pipeline unless a subtype registers its own - a request for
     * {@code PROCEEDINGS_PUBLICATION} would resolve here and be filtered to that type, until a
     * dedicated pipeline for it exists.
     */
    @Bean
    public MigrationPipeline<HydratorCVModel.Curriculum> hydratorDocumentPipeline() {
        return pipeline(MigrationEntityType.DOCUMENT, outputRouter);
    }

    private ItemRouter<HydratorCVModel.Curriculum> organisationUnitRouter() {
        return new SimpleMapping<>(
            MigrationEntityType.ORGANISATION_UNIT,
            organisationUnitExtractor,
            EntityCreator.of(
                organisationUnitService::createOrganisationUnit, OrganisationUnitDTO::getId),
            FailureHandler.noOp(),
            (record, dto) -> organisationUnitExtractor.keyOf(dto));
    }

    private ItemRouter<HydratorCVModel.Curriculum> personRouter() {
        return ItemRouter.ordered(List.of(personMapping(), prizeRouter(), employmentRouter()));
    }

    private ItemRouter<HydratorCVModel.Curriculum> personMapping() {
        return new SimpleMapping<>(
            MigrationEntityType.PERSON,
            RecordExtractor.of(personConverter),
            EntityCreator.of(
                personService::importPersonWithBasicInfo, Person::getId),
            personMergeFailureHandler,
            (record, dto) -> record.id());
    }

    private ItemRouter<HydratorCVModel.Curriculum> prizeRouter() {
        return new SimpleMapping<>(
            MigrationEntityType.PERSON_PRIZE,
            prizeExtractor,
            prizeEntityCreator,
            FailureHandler.noOp(),
            prizeExtractor::keyOf);
    }

    private ItemRouter<HydratorCVModel.Curriculum> employmentRouter() {
        return new SimpleMapping<>(
            MigrationEntityType.PERSON_EMPLOYMENT,
            employmentExtractor,
            employmentEntityCreator,
            FailureHandler.noOp(),
            employmentExtractor::keyOf);
    }

    private MigrationPipeline<HydratorCVModel.Curriculum> pipeline(
        MigrationEntityType entityType, ItemRouter<HydratorCVModel.Curriculum> router) {
        var sourceProperties = properties.forSource(HydratorSource.NAME);

        return new MigrationPipeline<>(
            HydratorSource.NAME,
            entityType,
            HydratorCVModel.Curriculum.class,
            new HttpPagedFetcher<>((page, size) -> cvClient.getCurricula(
                null, page, size, HydratorSource.CURRICULA_SORT)),
            router,
            sourceProperties.getRetry().toPolicy(),
            HydratorCVModel.Curriculum::id,
            sourceProperties.batchSizeOrDefault(properties.getDefaultBatchSize()));
    }
}
