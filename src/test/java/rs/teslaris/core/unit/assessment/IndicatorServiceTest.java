package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rs.teslaris.assessment.dto.indicator.IndicatorDTO;
import rs.teslaris.assessment.model.indicator.ApplicableEntityType;
import rs.teslaris.assessment.model.indicator.Indicator;
import rs.teslaris.assessment.model.indicator.IndicatorContentType;
import rs.teslaris.assessment.repository.indicator.IndicatorRepository;
import rs.teslaris.assessment.service.impl.indicator.IndicatorServiceImpl;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.IndicatorReferenceConstraintViolationException;

@SpringBootTest
public class IndicatorServiceTest {

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private IndicatorRepository indicatorRepository;

    @InjectMocks
    private IndicatorServiceImpl indicatorService;


    @Test
    void shouldReadAllIndicators() {
        var indicator1 = new Indicator();
        indicator1.setCode("code1");
        indicator1.setTitle(Set.of(new MultiLingualContent(new LanguageTag(), "Content 1", 1)));

        var indicator2 = new Indicator();
        indicator2.setCode("code2");
        indicator2.setTitle(Set.of(new MultiLingualContent(new LanguageTag(), "Content 2", 1)));

        when(indicatorRepository.readAll(eq("EN"), any(Pageable.class))).thenReturn(
            new PageImpl<>(List.of(indicator1, indicator2)));

        var response =
            indicatorService.readAllIndicators(PageRequest.of(0, 10), "EN");

        assertNotNull(response);
        assertEquals(2, response.getSize());
    }

    @Test
    void shouldReadIndicator() {
        var indicatorId = 1;
        var indicator = new Indicator();
        indicator.setTitle(
            Set.of(new MultiLingualContent(new LanguageTag(), "Title", 1)));
        indicator.setDescription(
            Set.of(new MultiLingualContent(new LanguageTag(), "Description", 1)));
        indicator.setCode("code");

        when(indicatorRepository.findById(indicatorId))
            .thenReturn(Optional.of(indicator));

        var result = indicatorService.readIndicatorById(indicatorId);

        assertEquals("code", result.code());
        verify(indicatorRepository).findById(indicatorId);
    }

    @Test
    void shouldReadAccessLevelForIndicator() {
        var indicatorId = 1;
        var indicator = new Indicator();
        indicator.setAccessLevel(AccessLevel.CLOSED);

        when(indicatorRepository.findById(indicatorId))
            .thenReturn(Optional.of(indicator));

        var result = indicatorService.readIndicatorAccessLevel(indicatorId);

        assertEquals(AccessLevel.CLOSED, result);
        verify(indicatorRepository).findById(indicatorId);
    }

    @Test
    void shouldCreateIndicator() {
        var indicatorDTO = new IndicatorDTO(null, "rule", List.of(new MultilingualContentDTO()),
            List.of(new MultilingualContentDTO()), AccessLevel.CLOSED,
            List.of(ApplicableEntityType.ALL), IndicatorContentType.TEXT);
        var newIndicator = new Indicator();

        when(indicatorRepository.save(any(Indicator.class)))
            .thenReturn(newIndicator);

        var result = indicatorService.createIndicator(
            indicatorDTO);

        assertNotNull(result);
        verify(indicatorRepository).save(any(Indicator.class));
    }

    @Test
    void shouldUpdateIndicator() {
        var indicatorId = 1;
        var indicatorDTO = new IndicatorDTO(null, "rule", List.of(new MultilingualContentDTO()),
            List.of(new MultilingualContentDTO()), AccessLevel.CLOSED,
            List.of(ApplicableEntityType.DOCUMENT, ApplicableEntityType.PERSON),
            IndicatorContentType.NUMBER);
        var existingIndicator = new Indicator();

        when(indicatorRepository.findById(indicatorId))
            .thenReturn(Optional.of(existingIndicator));

        indicatorService.updateIndicator(indicatorId,
            indicatorDTO);

        verify(indicatorRepository).findById(indicatorId);
        verify(indicatorRepository).save(existingIndicator);
    }

    @Test
    void shouldDeleteIndicator() {
        // Given
        var indicatorId = 1;

        when(indicatorRepository.isInUse(indicatorId)).thenReturn(false);
        when(indicatorRepository.findById(indicatorId)).thenReturn(Optional.of(new Indicator()));

        // When
        indicatorService.deleteIndicator(indicatorId);

        // Then
        verify(indicatorRepository).save(any());
    }

    @Test
    void shouldThrowExceptionWhenDeletingIndicatorInUse() {
        // Given
        var indicatorId = 1;

        when(indicatorRepository.isInUse(indicatorId)).thenReturn(true);

        // When
        assertThrows(IndicatorReferenceConstraintViolationException.class, () ->
            indicatorService.deleteIndicator(indicatorId));

        // Then (IndicatorReferenceConstraintViolationException should be thrown)
    }

    @Test
    public void shouldGetIndicatorsApplicableToEntity() {
        // given
        var mockIndicator1 = new Indicator();
        mockIndicator1.setId(1);
        var mockIndicator2 = new Indicator();
        mockIndicator2.setId(2);

        var applicableEntityTypes =
            new ArrayList<>(List.of(ApplicableEntityType.DOCUMENT, ApplicableEntityType.PERSON));
        var mockIndicators = List.of(mockIndicator1, mockIndicator2);

        // Mock repository call
        when(indicatorRepository.getIndicatorsApplicableToEntity(applicableEntityTypes))
            .thenReturn(mockIndicators);

        // when
        var result = indicatorService.getIndicatorsApplicableToEntity(applicableEntityTypes);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(indicatorRepository).getIndicatorsApplicableToEntity(applicableEntityTypes);
    }

    @Test
    public void shouldReturnEmptyListWhenNoIndicatorsFound() {
        // given
        var applicableEntityTypes = new ArrayList<>(List.of(ApplicableEntityType.DOCUMENT));

        when(indicatorRepository.getIndicatorsApplicableToEntity(applicableEntityTypes))
            .thenReturn(List.of());

        // when
        var result = indicatorService.getIndicatorsApplicableToEntity(applicableEntityTypes);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(indicatorRepository).getIndicatorsApplicableToEntity(applicableEntityTypes);
    }
}
