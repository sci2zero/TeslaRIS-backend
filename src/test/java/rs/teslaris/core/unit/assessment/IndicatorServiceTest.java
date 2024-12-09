package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import rs.teslaris.core.assessment.dto.IndicatorDTO;
import rs.teslaris.core.assessment.model.ApplicableEntityType;
import rs.teslaris.core.assessment.model.Indicator;
import rs.teslaris.core.assessment.repository.IndicatorRepository;
import rs.teslaris.core.assessment.service.impl.IndicatorServiceImpl;
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

        when(indicatorRepository.findAll(any(Pageable.class))).thenReturn(
            new PageImpl<>(List.of(indicator1, indicator2)));

        var response =
            indicatorService.readAllIndicators(PageRequest.of(0, 10));

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

        var result = indicatorService.readIndicatorAccessLEvel(indicatorId);

        assertEquals(AccessLevel.CLOSED, result);
        verify(indicatorRepository).findById(indicatorId);
    }

    @Test
    void shouldCreateIndicator() {
        var indicatorDTO = new IndicatorDTO(null, "rule", List.of(new MultilingualContentDTO()),
            List.of(new MultilingualContentDTO()), AccessLevel.CLOSED,
            List.of(ApplicableEntityType.ALL));
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
            List.of(ApplicableEntityType.DOCUMENT, ApplicableEntityType.PERSON));
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
}
