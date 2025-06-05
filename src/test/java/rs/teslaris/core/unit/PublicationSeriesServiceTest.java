package rs.teslaris.core.unit;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.document.PersonPublicationSeriesContribution;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.repository.document.PublicationSeriesRepository;
import rs.teslaris.core.service.impl.document.PublicationSeriesServiceImpl;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;

@SpringBootTest
public class PublicationSeriesServiceTest {

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private PublicationSeriesRepository publicationSeriesRepository;

    @InjectMocks
    private PublicationSeriesServiceImpl publicationSeriesService;


    @Test
    void shouldReorderPublicationSeriesContributionsCorrectly() {
        // Given
        var publicationSeriesId = 1;
        var contributionId = 42;
        var oldOrder = 2;
        var newOrder = 0;

        var contribution1 = mock(PersonPublicationSeriesContribution.class);
        when(contribution1.getId()).thenReturn(10);

        var contribution2 = mock(PersonPublicationSeriesContribution.class);
        when(contribution2.getId()).thenReturn(42);

        var contribution3 = mock(PersonPublicationSeriesContribution.class);
        when(contribution3.getId()).thenReturn(99);

        var publicationSeries = mock(PublicationSeries.class);
        when(publicationSeries.getContributions()).thenReturn(
            new LinkedHashSet<>(List.of(contribution1, contribution2, contribution3))
        );

        when(publicationSeriesRepository.findById(publicationSeriesId)).thenReturn(
            Optional.of(publicationSeries));

        // When
        publicationSeriesService.reorderPublicationSeriesContributions(
            publicationSeriesId, contributionId, oldOrder, newOrder
        );

        // Then
        verify(personContributionService).reorderContributions(
            argThat(set -> set.containsAll(List.of(contribution1, contribution2, contribution3))),
            eq(contributionId),
            eq(oldOrder),
            eq(newOrder)
        );
    }
}
