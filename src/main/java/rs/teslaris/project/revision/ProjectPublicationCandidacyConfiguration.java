package rs.teslaris.project.revision;

import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.revision.PublicationCandidacyUpdater;
import rs.teslaris.core.revision.RepositoryPublicationCandidacyUpdater;
import rs.teslaris.core.util.visibility.PublicVisibility;
import rs.teslaris.project.indexrepository.funding.FundingIndexRepository;
import rs.teslaris.project.indexrepository.project.ProjectIndexRepository;
import rs.teslaris.project.repository.funding.FundingRepository;
import rs.teslaris.project.repository.project.ProjectRepository;

@Configuration
public class ProjectPublicationCandidacyConfiguration {

    @Bean
    public PublicationCandidacyUpdater projectPublicationCandidacyUpdater(
        ProjectRepository projectRepository, ProjectIndexRepository projectIndexRepository,
        ApplicationEventPublisher applicationEventPublisher) {
        return new RepositoryPublicationCandidacyUpdater<>(
            Set.of(EntityType.PROJECT.name()),
            projectRepository,
            project -> PublicVisibility.isPubliclyVisible(project.getPublicationCandidate()),
            projectIndexRepository::findProjectIndexByDatabaseId,
            projectIndexRepository,
            applicationEventPublisher);
    }

    @Bean
    public PublicationCandidacyUpdater fundingPublicationCandidacyUpdater(
        FundingRepository fundingRepository, FundingIndexRepository fundingIndexRepository,
        ApplicationEventPublisher applicationEventPublisher) {
        return new RepositoryPublicationCandidacyUpdater<>(
            Set.of(EntityType.FUNDING.name()),
            fundingRepository,
            funding -> PublicVisibility.isPubliclyVisible(funding.getPublicationCandidate()),
            fundingIndexRepository::findFundingIndexByDatabaseId,
            fundingIndexRepository,
            applicationEventPublisher);
    }
}
