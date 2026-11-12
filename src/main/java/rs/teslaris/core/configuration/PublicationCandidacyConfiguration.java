package rs.teslaris.core.configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexrepository.BookSeriesIndexRepository;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.indexrepository.PublisherIndexRepository;
import rs.teslaris.core.repository.document.BookSeriesRepository;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.repository.document.PublisherRepository;
import rs.teslaris.core.repository.institution.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.revision.PublicationCandidacyUpdater;
import rs.teslaris.core.revision.RepositoryPublicationCandidacyUpdater;
import rs.teslaris.core.util.visibility.PublicVisibility;

@Configuration
public class PublicationCandidacyConfiguration {

    @Bean
    public PublicationCandidacyUpdater documentPublicationCandidacyUpdater(
        DocumentRepository documentRepository,
        DocumentPublicationIndexRepository documentPublicationIndexRepository,
        ApplicationEventPublisher applicationEventPublisher) {
        return new RepositoryPublicationCandidacyUpdater<>(
            Arrays.stream(DocumentPublicationType.values())
                .map(DocumentPublicationType::name)
                .collect(Collectors.toSet()),
            documentRepository,
            PublicVisibility::isPubliclyVisible,
            documentPublicationIndexRepository::findDocumentPublicationIndexByDatabaseId,
            documentPublicationIndexRepository,
            applicationEventPublisher);
    }

    @Bean
    public PublicationCandidacyUpdater personPublicationCandidacyUpdater(
        PersonRepository personRepository, PersonIndexRepository personIndexRepository,
        ApplicationEventPublisher applicationEventPublisher) {
        return new RepositoryPublicationCandidacyUpdater<>(
            Set.of(EntityType.PERSON.name()),
            personRepository,
            person -> PublicVisibility.isPubliclyVisible(person.getApproveStatus(),
                person.getPublicationCandidate()),
            personIndexRepository::findByDatabaseId,
            personIndexRepository,
            applicationEventPublisher);
    }

    @Bean
    public PublicationCandidacyUpdater organisationUnitPublicationCandidacyUpdater(
        OrganisationUnitRepository organisationUnitRepository,
        OrganisationUnitIndexRepository organisationUnitIndexRepository,
        ApplicationEventPublisher applicationEventPublisher) {
        return new RepositoryPublicationCandidacyUpdater<>(
            Set.of(EntityType.ORGANISATION_UNIT.name()),
            organisationUnitRepository,
            organisationUnit -> PublicVisibility.isPubliclyVisible(
                organisationUnit.getApproveStatus(), organisationUnit.getPublicationCandidate()),
            organisationUnitIndexRepository::findOrganisationUnitIndexByDatabaseId,
            organisationUnitIndexRepository,
            applicationEventPublisher);
    }

    @Bean
    public PublicationCandidacyUpdater eventPublicationCandidacyUpdater(
        EventRepository eventRepository, EventIndexRepository eventIndexRepository,
        ApplicationEventPublisher applicationEventPublisher) {
        return new RepositoryPublicationCandidacyUpdater<>(
            Set.of(EntityType.CONFERENCE.name(), EntityType.EXHIBITION.name(),
                EntityType.COURSE.name(), EntityType.OTHER_EVENT.name()),
            eventRepository,
            event -> PublicVisibility.isPubliclyVisible(event.getPublicationCandidate()),
            eventIndexRepository::findByDatabaseId,
            eventIndexRepository,
            applicationEventPublisher);
    }

    @Bean
    public PublicationCandidacyUpdater journalPublicationCandidacyUpdater(
        JournalRepository journalRepository, JournalIndexRepository journalIndexRepository,
        ApplicationEventPublisher applicationEventPublisher) {
        return new RepositoryPublicationCandidacyUpdater<>(
            Set.of(EntityType.JOURNAL.name()),
            journalRepository,
            journal -> PublicVisibility.isPubliclyVisible(journal.getPublicationCandidate()),
            journalIndexRepository::findJournalIndexByDatabaseId,
            journalIndexRepository,
            applicationEventPublisher);
    }

    @Bean
    public PublicationCandidacyUpdater bookSeriesPublicationCandidacyUpdater(
        BookSeriesRepository bookSeriesRepository,
        BookSeriesIndexRepository bookSeriesIndexRepository,
        ApplicationEventPublisher applicationEventPublisher) {
        return new RepositoryPublicationCandidacyUpdater<>(
            Set.of(EntityType.BOOK_SERIES.name()),
            bookSeriesRepository,
            bookSeries -> PublicVisibility.isPubliclyVisible(bookSeries.getPublicationCandidate()),
            bookSeriesIndexRepository::findBookSeriesIndexByDatabaseId,
            bookSeriesIndexRepository,
            applicationEventPublisher);
    }

    @Bean
    public PublicationCandidacyUpdater publisherPublicationCandidacyUpdater(
        PublisherRepository publisherRepository,
        PublisherIndexRepository publisherIndexRepository,
        ApplicationEventPublisher applicationEventPublisher) {
        return new RepositoryPublicationCandidacyUpdater<>(
            Set.of(EntityType.PUBLISHER.name()),
            publisherRepository,
            publisher -> PublicVisibility.isPubliclyVisible(publisher.getPublicationCandidate()),
            publisherIndexRepository::findByDatabaseId,
            publisherIndexRepository,
            applicationEventPublisher);
    }
}
