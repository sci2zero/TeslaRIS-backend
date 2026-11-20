package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.service.interfaces.document.PerformanceRelatedOutputService;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.pipeline.EntityUpdater;
import rs.teslaris.migrator.pipeline.ExistingReader;
import rs.teslaris.migrator.pipeline.FailureHandler;
import rs.teslaris.migrator.pipeline.ItemRouter;
import rs.teslaris.migrator.pipeline.MigrationItem;
import rs.teslaris.migrator.pipeline.UpdatePlan;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.ResolvedToExistingException;

/**
 * Curriculum services the crosswalk maps to outputs (CW-001-MAP-000844, 000854, 000945): oral
 * presentations, broadcast and text interviews, as performance related outputs.
 */
@Component
@RequiredArgsConstructor
public class HydratorServiceRouter implements ItemRouter<HydratorCVModel.Curriculum> {

    private static final MigrationEntityType TYPE = MigrationEntityType.PERFORMANCE_RELATED_OUTPUT;

    private final HydratorOutputConverters converters;

    private final HydratorProceedingsResolver eventResolver;

    private final HydratorDocumentResolver documentResolver;

    private final HydratorContributionLinker contributionLinker;

    private final HydratorPersonResolver personResolver;

    private final PerformanceRelatedOutputService performanceRelatedOutputService;


    @Override
    public List<MigrationItem<?>> route(HydratorCVModel.Curriculum record) {
        if (Objects.isNull(record.curriculum()) ||
            Objects.isNull(record.curriculum().services()) ||
            Objects.isNull(record.curriculum().services().service())) {
            return List.of();
        }

        var items = new ArrayList<MigrationItem<?>>();

        record.curriculum().services().service().forEach(service -> {
            if (Objects.isNull(service)) {
                return;
            }

            var item = routeService(record, service);

            if (Objects.nonNull(item)) {
                items.add(item);
            }
        });

        return items;
    }

    private MigrationItem<PerformanceRelatedOutputDTO> routeService(
        HydratorCVModel.Curriculum record, HydratorCVModel.Service service) {
        if (Objects.nonNull(service.nonResearchPresentation())) {
            var dto = converters.toNonResearchPresentation(record, service);

            return Objects.isNull(dto) ? null :
                item(record, service, dto, (presentation, run) -> {
                    resolveSameWork(record, presentation);
                    var source = service.nonResearchPresentation();
                    presentation.setEventId(eventResolver.createServiceEvent(
                        converters.serviceKey(record, service), null,
                        source.conferenceEvent(), source.presentationDate(), null,
                        converters.languageOf(record), source.conferenceEventLocation(),
                        source.organizationPresentedTo(),
                        converters.speakerContributions(record, source), run.isPerformIndex()));

                    var id = performanceRelatedOutputService.createPerformanceRelatedOutput(
                        presentation, run.isPerformIndex()).getId();
                    documentResolver.register(TYPE, id, presentation);

                    return id;
                });
        }

        if (Objects.nonNull(service.broadcastInterview())) {
            var dto = converters.toBroadcastInterview(record, service);

            return Objects.isNull(dto) ? null :
                item(record, service, dto, (interview, run) -> {
                    var source = service.broadcastInterview();
                    interview.setEventId(eventResolver.createServiceEvent(
                        converters.serviceKey(record, service), null,
                        source.program(), source.firstBroadcastDate(), source.endDate(),
                        converters.languageOf(record), null, null, List.of(),
                        run.isPerformIndex()));

                    return performanceRelatedOutputService.createPerformanceRelatedOutput(
                        interview, run.isPerformIndex()).getId();
                });
        }

        if (Objects.nonNull(service.textInterview())) {
            var dto = converters.toTextInterview(record, service);

            return Objects.isNull(dto) ? null :
                item(record, service, dto, (interview, run) ->
                    performanceRelatedOutputService.createPerformanceRelatedOutput(interview,
                        run.isPerformIndex()).getId());
        }

        return null;
    }

    // The event is created on creation, and an update keeps the present one
    private MigrationItem<PerformanceRelatedOutputDTO> item(
        HydratorCVModel.Curriculum record, HydratorCVModel.Service service,
        PerformanceRelatedOutputDTO dto, EntityCreator<PerformanceRelatedOutputDTO> creator) {
        return new MigrationItem<>(
            TYPE,
            converters.serviceKey(record, service),
            null,
            dto,
            creator,
            UpdatePlan.<PerformanceRelatedOutputDTO>of(
                ExistingReader.of(
                    performanceRelatedOutputService::readPerformanceRelatedOutputById),
                EntityUpdater.of(performanceRelatedOutputService::editPerformanceRelatedOutput)),
            FailureHandler.resolvedToExisting());
    }

    // The same presentation from a co-author's curriculum links the owner on the existing one
    private void resolveSameWork(HydratorCVModel.Curriculum record,
                                 PerformanceRelatedOutputDTO dto) {
        var existing = documentResolver.find(TYPE, dto, personResolver.resolveOwner(record));

        if (existing.isPresent()) {
            throw new ResolvedToExistingException(existing.get(),
                "same work in a co-author's curriculum: " +
                    contributionLinker.link(existing.get(), dto,
                        performanceRelatedOutputService::readPerformanceRelatedOutputById,
                        performanceRelatedOutputService::editPerformanceRelatedOutput));
        }
    }
}
