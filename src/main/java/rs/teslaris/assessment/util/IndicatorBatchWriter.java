package rs.teslaris.assessment.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.assessment.model.indicator.PublicationSeriesIndicator;
import rs.teslaris.assessment.repository.indicator.PublicationSeriesIndicatorRepository;

@Component
@RequiredArgsConstructor
public class IndicatorBatchWriter implements BatchWriter {

    private static final int BATCH_SIZE = 2000;

    private final PublicationSeriesIndicatorRepository repository;

    @PersistenceContext
    private final EntityManager entityManager;

    private final List<PublicationSeriesIndicator> buffer =
        Collections.synchronizedList(new ArrayList<>());


    public synchronized void bufferIndicator(PublicationSeriesIndicator indicator) {
        buffer.add(indicator);
        if (buffer.size() >= BATCH_SIZE) {
            flushBatch();
        }
    }

    public synchronized void flushBatch() {
        if (!buffer.isEmpty()) {
            repository.saveAll(buffer);
            repository.flush();
            entityManager.clear(); // free Hibernate’s first-level cache
            buffer.clear();
        }
    }
}
