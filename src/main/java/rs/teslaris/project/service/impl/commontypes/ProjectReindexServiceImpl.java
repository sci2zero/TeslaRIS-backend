package rs.teslaris.project.service.impl.commontypes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import rs.teslaris.core.applicationevent.ProjectEventReindexingEvent;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.project.service.interfaces.commontypes.ProjectReindexService;
import rs.teslaris.project.service.interfaces.funding.FundingApplicationService;
import rs.teslaris.project.service.interfaces.funding.FundingCallService;
import rs.teslaris.project.service.interfaces.funding.FundingProgramService;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectReindexServiceImpl implements ProjectReindexService {

    private final FundingProgramService fundingProgramService;

    private final FundingCallService fundingCallService;

    private final FundingApplicationService fundingApplicationService;

    private final ProjectService projectService;


    @Override
    public void reindexDatabase(List<EntityType> indexesToRepopulate) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        if (indexesToRepopulate.contains(EntityType.FUNDING_PROGRAM)) {
            futures.add(fundingProgramService.reindexFundingPrograms());
        }

        if (indexesToRepopulate.contains(EntityType.FUNDING_CALL)) {
            futures.add(fundingCallService.reindexFundingCalls());
        }

        if (indexesToRepopulate.contains(EntityType.FUNDING_APPLICATION)) {
            futures.add(fundingApplicationService.reindexFundingApplications());
        }

        if (indexesToRepopulate.contains(EntityType.PROJECT)) {
            futures.add(projectService.reindexProject());
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            futures.clear();
        } catch (CompletionException e) {
            log.error("Error during parallel reindexing of project entities. Reason: ", e);
        }
    }

    @Async("taskExecutor")
    @EventListener
    public void handleProjectEventReindexingEvent(ProjectEventReindexingEvent event) {
        reindexDatabase(event.indexesToRepopulate());
    }
}
