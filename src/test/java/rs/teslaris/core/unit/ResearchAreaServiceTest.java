package rs.teslaris.core.unit;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.institution.ResearchAreaDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.repository.commontypes.ResearchAreaRepository;
import rs.teslaris.core.service.impl.commontypes.ResearchAreaServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.ResearchAreaReferenceConstraintViolationException;

@SpringBootTest
public class ResearchAreaServiceTest {

    @Mock
    private ResearchAreaRepository researchAreaRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @InjectMocks
    private ResearchAreaServiceImpl researchAreaService;


    @Test
    void shouldReturnResearchAreaWhenIsInvoked() {
        // Arrange
        var pageable = Pageable.ofSize(10).withPage(0);
        var researchArea1 = new ResearchArea();
        researchArea1.setId(1);

        ResearchArea researchArea2 = new ResearchArea();
        researchArea2.setId(2);

        var researchAreasPage = new PageImpl<>(List.of(researchArea1, researchArea2));

        when(researchAreaRepository.findAll(pageable)).thenReturn(researchAreasPage);

        // when
        var resultPage = researchAreaService.findAll(pageable);

        // then
        assertEquals(2, resultPage.getTotalElements());
        assertEquals(1, resultPage.getContent().get(0).getId());
        assertEquals(0, resultPage.getContent().get(0).getName().size());
        assertEquals(2, resultPage.getContent().get(1).getId());
        assertEquals(0, resultPage.getContent().get(1).getName().size());

        verify(researchAreaRepository, times(1)).findAll(pageable);
        verifyNoMoreInteractions(researchAreaRepository);
    }

    @Test
    void shouldCreateResearchAreaWhenProvidedWithValidData() {
        // given
        var researchAreaDTO = new ResearchAreaDTO();
        researchAreaDTO.setName(List.of(new MultilingualContentDTO(1, "EN", "name", 1)));
        researchAreaDTO.setDescription(
            List.of(new MultilingualContentDTO(1, "EN", "description", 1)));
        researchAreaDTO.setSuperResearchAreaId(1);

        var nameMultilingualContent = Set.of(new MultiLingualContent());
        var descriptionMultilingualContent = Set.of(new MultiLingualContent());

        when(multilingualContentService.getMultilingualContent(
            researchAreaDTO.getName())).thenReturn(nameMultilingualContent);
        when(multilingualContentService.getMultilingualContent(
            researchAreaDTO.getDescription())).thenReturn(descriptionMultilingualContent);
        when(researchAreaRepository.save(any(ResearchArea.class))).thenAnswer(invocation -> {
            ResearchArea savedResearchArea = invocation.getArgument(0);
            savedResearchArea.setId(1);
            var superRA = new ResearchArea();
            superRA.setId(1);
            savedResearchArea.setSuperResearchArea(superRA);
            return savedResearchArea;
        });

        // when
        var resultResearchArea = researchAreaService.createResearchArea(researchAreaDTO);

        // then
        assertEquals(nameMultilingualContent, resultResearchArea.getName());
        assertEquals(descriptionMultilingualContent, resultResearchArea.getDescription());
        assertEquals(1, resultResearchArea.getSuperResearchArea().getId());

        verify(multilingualContentService, times(1)).getMultilingualContent(
            researchAreaDTO.getName());
        verify(multilingualContentService, times(1)).getMultilingualContent(
            researchAreaDTO.getDescription());
        verify(researchAreaRepository, times(1)).save(any(ResearchArea.class));
    }

    @Test
    void shouldEditResearchAreaWhenProvidedWithValidData() {
        // given
        var researchAreaDTO = new ResearchAreaDTO();
        researchAreaDTO.setName(List.of(new MultilingualContentDTO(1, "EN", "name", 1)));
        researchAreaDTO.setDescription(
            List.of(new MultilingualContentDTO(1, "EN", "description", 1)));
        researchAreaDTO.setSuperResearchAreaId(1);

        var nameMultilingualContent = Set.of(new MultiLingualContent());
        var descriptionMultilingualContent = Set.of(new MultiLingualContent());

        when(multilingualContentService.getMultilingualContent(
            researchAreaDTO.getName())).thenReturn(nameMultilingualContent);
        when(multilingualContentService.getMultilingualContent(
            researchAreaDTO.getDescription())).thenReturn(descriptionMultilingualContent);
        when(researchAreaRepository.getReferenceById(1)).thenReturn(
            new ResearchArea(new HashSet<>(), new HashSet<>(), null));

        // when
        researchAreaService.editResearchArea(researchAreaDTO, 1);

        // then
        verify(multilingualContentService, times(1)).getMultilingualContent(
            researchAreaDTO.getName());
        verify(multilingualContentService, times(1)).getMultilingualContent(
            researchAreaDTO.getDescription());
        verify(researchAreaRepository, times(1)).save(any(ResearchArea.class));
    }

    @Test
    void shouldDeleteResearchAreaIfAllChecksPass() {
        // given
        var researchAreaId = 1;
        var researchArea = new ResearchArea();

        when(researchAreaRepository.findById(researchAreaId)).thenReturn(Optional.of(researchArea));
        when(researchAreaRepository.isSuperArea(researchAreaId)).thenReturn(false);
        when(researchAreaRepository.isResearchedBySomeone(researchAreaId)).thenReturn(false);
        when(researchAreaRepository.isResearchedInMonograph(researchAreaId)).thenReturn(false);
        when(researchAreaRepository.isResearchedInThesis(researchAreaId)).thenReturn(false);

        // when
        researchAreaService.deleteResearchArea(researchAreaId);

        // then
        verify(researchAreaRepository, times(1)).isSuperArea(researchAreaId);
        verify(researchAreaRepository, times(1)).isResearchedBySomeone(researchAreaId);
        verify(researchAreaRepository, times(1)).isResearchedInMonograph(researchAreaId);
        verify(researchAreaRepository, times(1)).isResearchedInThesis(researchAreaId);
        verify(researchAreaRepository, times(1)).save(researchArea);
        verify(researchAreaRepository, times(1)).findById(any());
        verifyNoMoreInteractions(researchAreaRepository);
    }

    @Test
    void shouldThrowResearchAreaInUseExceptionIfNotAllChecksPass() {
        // given
        var researchAreaId = 1;

        when(researchAreaRepository.isSuperArea(researchAreaId)).thenReturn(true);

        // when
        assertThrows(ResearchAreaReferenceConstraintViolationException.class, () -> {
            researchAreaService.deleteResearchArea(researchAreaId);
        });

        // then
        verify(researchAreaRepository, times(1)).isSuperArea(researchAreaId);
        verifyNoMoreInteractions(researchAreaRepository);
    }

    @Test
    public void shouldListResearchAreas() {
        // Given
        var researchArea1 = new ResearchArea();
        researchArea1.setId(1);
        var superResearchArea1 = new ResearchArea();
        superResearchArea1.setId(10);
        researchArea1.setSuperResearchArea(superResearchArea1);

        var researchArea2 = new ResearchArea();
        researchArea2.setId(2);
        var superResearchArea2 = new ResearchArea();
        superResearchArea2.setId(20);
        researchArea2.setSuperResearchArea(superResearchArea2);

        var researchAreas = Arrays.asList(researchArea1, researchArea2);
        when(researchAreaRepository.findAll()).thenReturn(researchAreas);

        // When
        var result = researchAreaService.listResearchAreas();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        ResearchAreaDTO dto1 = result.getFirst();
        assertEquals(1, dto1.getId());
        assertEquals(10, dto1.getSuperResearchAreaId());

        ResearchAreaDTO dto2 = result.get(1);
        assertEquals(2, dto2.getId());
        assertEquals(20, dto2.getSuperResearchAreaId());
    }
}
