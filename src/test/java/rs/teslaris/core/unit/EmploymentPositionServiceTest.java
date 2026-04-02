package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.dto.person.involvement.EmploymentPositionDTO;
import rs.teslaris.core.model.person.EmploymentPositionHierarchy;
import rs.teslaris.core.repository.person.EmploymentPositionRepository;
import rs.teslaris.core.service.impl.person.EmploymentPositionServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.ReferenceConstraintException;
import rs.teslaris.core.util.language.LanguageAbbreviations;

@SpringBootTest
class EmploymentPositionServiceTest {

    @Mock
    private EmploymentPositionRepository employmentPositionRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @InjectMocks
    private EmploymentPositionServiceImpl employmentPositionService;

    private EmploymentPositionDTO employmentPositionDTO;
    private EmploymentPositionHierarchy employmentPositionHierarchy;


    @BeforeEach
    void setUp() {
        employmentPositionDTO = new EmploymentPositionDTO(
            null,
            null,
            "Processed Name",
            "Scheme Name",
            null
        );

        employmentPositionHierarchy = new EmploymentPositionHierarchy();
        employmentPositionHierarchy.setId(1);
    }

    @Test
    void shouldCreateEmploymentPositionSuccessfully() {
        // given
        when(multilingualContentService.getMultilingualContent(any())).thenReturn(null);
        when(employmentPositionRepository.save(any(EmploymentPositionHierarchy.class)))
            .thenReturn(employmentPositionHierarchy);

        // when
        var result = employmentPositionService.createEmploymentPosition(employmentPositionDTO);

        // then
        assertNotNull(result);
        assertEquals(1, result.getId());
        verify(multilingualContentService).getMultilingualContent(employmentPositionDTO.name());
        verify(employmentPositionRepository).save(any(EmploymentPositionHierarchy.class));
    }

    @Test
    void shouldCreateEmploymentPositionWithSuperPosition() {
        // given
        var superPositionId = 2;
        var employmentPositionDTOWithSuper = new EmploymentPositionDTO(
            null,
            null,
            "Processed Name",
            "Scheme Name",
            superPositionId
        );

        var superEmploymentPosition = new EmploymentPositionHierarchy();
        superEmploymentPosition.setId(superPositionId);

        when(multilingualContentService.getMultilingualContent(any())).thenReturn(null);
        when(employmentPositionRepository.findById(superPositionId))
            .thenReturn(Optional.of(superEmploymentPosition));
        when(employmentPositionRepository.save(any(EmploymentPositionHierarchy.class)))
            .thenReturn(employmentPositionHierarchy);

        // when
        var result =
            employmentPositionService.createEmploymentPosition(employmentPositionDTOWithSuper);

        // then
        assertNotNull(result);
        verify(employmentPositionRepository).findById(superPositionId);
        verify(employmentPositionRepository).save(any(EmploymentPositionHierarchy.class));
    }

    @Test
    void shouldEditEmploymentPositionSuccessfully() {
        // given
        var employmentPositionId = 1;
        when(employmentPositionRepository.findById(employmentPositionId))
            .thenReturn(Optional.of(employmentPositionHierarchy));
        when(multilingualContentService.getMultilingualContent(any())).thenReturn(null);
        when(employmentPositionRepository.save(any(EmploymentPositionHierarchy.class)))
            .thenReturn(employmentPositionHierarchy);

        // when
        employmentPositionService.editEmploymentPosition(employmentPositionDTO,
            employmentPositionId);

        // then
        verify(employmentPositionRepository).findById(employmentPositionId);
        verify(multilingualContentService).getMultilingualContent(employmentPositionDTO.name());
        verify(employmentPositionRepository).save(employmentPositionHierarchy);
    }

    @Test
    void shouldEditEmploymentPositionWithNullSuperPosition() {
        // given
        var employmentPositionId = 1;
        var employmentPositionDTOWithNullSuper = new EmploymentPositionDTO(
            null,
            null,
            "Processed Name",
            "Scheme Name",
            null
        );

        when(employmentPositionRepository.findById(employmentPositionId))
            .thenReturn(Optional.of(employmentPositionHierarchy));
        when(multilingualContentService.getMultilingualContent(any())).thenReturn(null);
        when(employmentPositionRepository.save(any(EmploymentPositionHierarchy.class)))
            .thenReturn(employmentPositionHierarchy);

        // when
        employmentPositionService.editEmploymentPosition(employmentPositionDTOWithNullSuper,
            employmentPositionId);

        // then
        verify(employmentPositionRepository).findById(employmentPositionId);
        verify(employmentPositionRepository).save(employmentPositionHierarchy);
        assertNull(employmentPositionHierarchy.getSuperEmploymentPosition());
    }

    @Test
    void shouldDeleteEmploymentPositionSuccessfully() {
        // given
        var employmentPositionId = 1;
        when(employmentPositionRepository.getChildEmploymentPositions(employmentPositionId))
            .thenReturn(List.of());
        when(employmentPositionRepository.findById(employmentPositionId))
            .thenReturn(Optional.of(new EmploymentPositionHierarchy()));

        // when
        employmentPositionService.deleteEmploymentPosition(employmentPositionId);

        // then
        verify(employmentPositionRepository).getChildEmploymentPositions(employmentPositionId);
        verify(employmentPositionRepository).save(any());
    }

    @Test
    void shouldThrowExceptionWhenDeletingEmploymentPositionWithChildren() {
        // given
        var employmentPositionId = 1;
        var childPosition = new EmploymentPositionHierarchy();
        childPosition.setId(2);

        when(employmentPositionRepository.getChildEmploymentPositions(employmentPositionId))
            .thenReturn(List.of(childPosition));

        // when & then
        assertThrows(ReferenceConstraintException.class, () ->
            employmentPositionService.deleteEmploymentPosition(employmentPositionId)
        );

        verify(employmentPositionRepository).getChildEmploymentPositions(employmentPositionId);
        verify(employmentPositionRepository, never()).deleteById(employmentPositionId);
    }

    @Test
    void shouldReturnTopLevelEmploymentPositionsWhenParentIdIsNull() {
        // given
        Integer parentId = null;
        var topLevelPositions =
            List.of(new EmploymentPositionHierarchy(), new EmploymentPositionHierarchy());
        when(employmentPositionRepository.getTopLevelEmploymentPositions())
            .thenReturn(topLevelPositions);

        // when
        var result = employmentPositionService.getChildEmploymentPositions(parentId);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(employmentPositionRepository).getTopLevelEmploymentPositions();
        verify(employmentPositionRepository, never()).getChildEmploymentPositions(any());
    }

    @Test
    void shouldReturnTopLevelEmploymentPositionsWhenParentIdIsZero() {
        // given
        Integer parentId = 0;
        var topLevelPositions = List.of(new EmploymentPositionHierarchy());
        when(employmentPositionRepository.getTopLevelEmploymentPositions())
            .thenReturn(topLevelPositions);

        // when
        var result = employmentPositionService.getChildEmploymentPositions(parentId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(employmentPositionRepository).getTopLevelEmploymentPositions();
        verify(employmentPositionRepository, never()).getChildEmploymentPositions(any());
    }

    @Test
    void shouldReturnChildEmploymentPositionsWhenParentIdIsProvided() {
        // given
        Integer parentId = 5;
        var childPositions = List.of(
            new EmploymentPositionHierarchy(),
            new EmploymentPositionHierarchy(),
            new EmploymentPositionHierarchy()
        );
        when(employmentPositionRepository.getChildEmploymentPositions(parentId))
            .thenReturn(childPositions);

        // when
        var result = employmentPositionService.getChildEmploymentPositions(parentId);

        // then
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(employmentPositionRepository).getChildEmploymentPositions(parentId);
        verify(employmentPositionRepository, never()).getTopLevelEmploymentPositions();
    }

    @Test
    void shouldReturnEmptyListWhenNoChildEmploymentPositionsFound() {
        // given
        Integer parentId = 5;
        when(employmentPositionRepository.getChildEmploymentPositions(parentId))
            .thenReturn(List.of());

        // when
        var result = employmentPositionService.getChildEmploymentPositions(parentId);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(employmentPositionRepository).getChildEmploymentPositions(parentId);
        verify(employmentPositionRepository, never()).getTopLevelEmploymentPositions();
    }

    @Test
    void shouldReturnEmptyListWhenNoTopLevelEmploymentPositionsFound() {
        // given
        Integer parentId = null;
        when(employmentPositionRepository.getTopLevelEmploymentPositions())
            .thenReturn(List.of());

        // when
        var result = employmentPositionService.getChildEmploymentPositions(parentId);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(employmentPositionRepository).getTopLevelEmploymentPositions();
        verify(employmentPositionRepository, never()).getChildEmploymentPositions(any());
    }

    @Test
    public void shouldSearchEmploymentPositions() {
        // given
        var searchTerm = "Search Term";
        var employmentPositionPage = new PageImpl<>(List.of(new EmploymentPositionHierarchy()));
        var pageable = PageRequest.of(0, 10);
        when(
            employmentPositionRepository.searchEmploymentPositions(searchTerm.toLowerCase(),
                LanguageAbbreviations.SERBIAN,
                pageable)).thenReturn(
            employmentPositionPage);

        // when
        var result = employmentPositionService.searchEmploymentPositions(pageable, searchTerm,
            LanguageAbbreviations.SERBIAN);

        // then
        assertNotNull(result);
        verify(employmentPositionRepository).searchEmploymentPositions(searchTerm.toLowerCase(),
            LanguageAbbreviations.SERBIAN, pageable);
    }
}
