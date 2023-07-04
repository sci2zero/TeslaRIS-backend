package rs.teslaris.core;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.institution.ResearchAreaDTO;
import rs.teslaris.core.exception.ResearchAreaInUseException;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.repository.commontypes.ResearchAreaRepository;
import rs.teslaris.core.service.MultilingualContentService;
import rs.teslaris.core.service.impl.ResearchAreaServiceImpl;

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
        researchArea1.setName(new HashSet<>());
        researchArea1.setDescription(new HashSet<>());

        ResearchArea researchArea2 = new ResearchArea();
        researchArea2.setId(2);
        researchArea2.setName(new HashSet<>());
        researchArea2.setDescription(new HashSet<>());

        var researchAreasPage = new PageImpl<>(List.of(researchArea1, researchArea2));

        when(researchAreaRepository.findAllByDeletedIsFalse(pageable)).thenReturn(researchAreasPage);

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
        researchAreaDTO.setName(List.of(new MultilingualContentDTO(1, "name", 1)));
        researchAreaDTO.setDescription(List.of(new MultilingualContentDTO(1, "description", 1)));
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
        researchAreaDTO.setName(List.of(new MultilingualContentDTO(1, "name", 1)));
        researchAreaDTO.setDescription(List.of(new MultilingualContentDTO(1, "description", 1)));
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

        when(researchAreaRepository.findByIdAndDeletedIsFalse(researchAreaId)).thenReturn(null);
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
        verify(researchAreaRepository, times(1)).delete(any());
        verify(researchAreaRepository, times(1)).getReferenceById(any());
        verifyNoMoreInteractions(researchAreaRepository);
    }

    @Test
    void shouldThrowResearchAreaInUseExceptionIfNotAllChecksPass() {
        // given
        var researchAreaId = 1;

        when(researchAreaRepository.isSuperArea(researchAreaId)).thenReturn(true);

        // when
        assertThrows(ResearchAreaInUseException.class, () -> {
            researchAreaService.deleteResearchArea(researchAreaId);
        });

        // then
        verify(researchAreaRepository, times(1)).isSuperArea(researchAreaId);
        verifyNoMoreInteractions(researchAreaRepository);
    }

}
