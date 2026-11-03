package rs.teslaris.core.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import rs.teslaris.core.configuration.MongoIndexContributor;
import rs.teslaris.core.configuration.MongoIndexInitializer;

public class MongoIndexInitializerTest {

    private MongoTemplate mongoTemplate;

    private IndexOperations indexOperations;

    private MongoIndexInitializer initializer;

    private final Index firstIndex = new Index().on("first", Sort.Direction.ASC);


    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        indexOperations = mock(IndexOperations.class);
        when(mongoTemplate.indexOps(any(Class.class))).thenReturn(indexOperations);

        MongoIndexContributor contributor = () -> List.of(
            new MongoIndexContributor.MongoIndex(String.class, firstIndex),
            new MongoIndexContributor.MongoIndex(String.class,
                new Index().on("second", Sort.Direction.ASC)),
            new MongoIndexContributor.MongoIndex(String.class,
                new Index().on("third", Sort.Direction.ASC)));

        ObjectProvider<MongoIndexContributor> contributors = mock(ObjectProvider.class);
        when(contributors.stream()).thenReturn(Stream.of(contributor));

        initializer = new MongoIndexInitializer(mongoTemplate, contributors);
    }

    @Test
    public void shouldCreateEveryIndex() {
        // When
        initializer.ensureIndexes();

        // Then
        verify(indexOperations, times(3)).ensureIndex(any());
    }

    @Test
    public void shouldSkipRemainingIndexesWhenMongoIsNotReachable() {
        // Given
        when(indexOperations.ensureIndex(any()))
            .thenThrow(new DataAccessResourceFailureException("Timed out"));

        // When
        initializer.ensureIndexes();

        // Then
        verify(indexOperations, times(1)).ensureIndex(any());
    }

    @Test
    public void shouldContinueWhenOneIndexFailsForAnotherReason() {
        // Given
        when(indexOperations.ensureIndex(eq(firstIndex)))
            .thenThrow(new DuplicateKeyException("Duplicate key"));

        // When
        initializer.ensureIndexes();

        // Then
        verify(indexOperations, times(3)).ensureIndex(any());
    }
}
