package rs.teslaris.core.revision;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.applicationevent.PublicVisibilityChangedEvent;
import rs.teslaris.core.indexmodel.PubliclyVisibleIndex;
import rs.teslaris.core.model.commontypes.PublicationCandidacyHolder;

@Slf4j
public class RepositoryPublicationCandidacyUpdater<E extends PublicationCandidacyHolder,
    I extends PubliclyVisibleIndex> implements PublicationCandidacyUpdater {

    private final Set<String> entityTypes;

    private final CrudRepository<E, Integer> entityRepository;

    private final Predicate<E> publicVisibility;

    private final Function<Integer, Optional<I>> indexLookup;

    private final CrudRepository<I, String> indexRepository;

    private final ApplicationEventPublisher applicationEventPublisher;


    @Autowired
    public RepositoryPublicationCandidacyUpdater(
        Set<String> entityTypes,
        CrudRepository<E, Integer> entityRepository,
        Predicate<E> publicVisibility,
        Function<Integer, Optional<I>> indexLookup,
        CrudRepository<I, String> indexRepository,
        ApplicationEventPublisher applicationEventPublisher) {
        this.entityTypes = Set.copyOf(entityTypes);
        this.entityRepository = entityRepository;
        this.publicVisibility = publicVisibility;
        this.indexLookup = indexLookup;
        this.indexRepository = indexRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public Set<String> entityTypes() {
        return entityTypes;
    }

    @Override
    @Transactional
    public void apply(String entityType, Integer entityId, boolean publicationCandidate) {
        var entity = entityRepository.findById(entityId);

        if (entity.isEmpty()) {
            log.debug("Skipping publication candidacy of missing record {} {}.", entityType,
                entityId);
            return;
        }

        var record = entity.get();

        if (Objects.equals(record.getPublicationCandidate(), publicationCandidate)) {
            return;
        }

        record.setPublicationCandidate(publicationCandidate);
        entityRepository.save(record);

        var visible = publicVisibility.test(record);
        indexLookup.apply(entityId).ifPresent(index -> {
            index.setIsPubliclyVisible(visible);
            indexRepository.save(index);
        });

        applicationEventPublisher.publishEvent(
            new PublicVisibilityChangedEvent(entityType, entityId, visible));
    }
}
