package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.BrandingInformationDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.BrandingInformation;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.commontypes.BrandingInformationRepository;
import rs.teslaris.core.service.impl.commontypes.BrandingInformationServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;

@SpringBootTest
public class BrandingInformationServiceTest {

    @Mock
    private BrandingInformationRepository brandingInformationRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @InjectMocks
    private BrandingInformationServiceImpl brandingInformationService;


    @Test
    public void shouldReturnBrandingInformationDTOWhenBrandingInformationExists() {
        // given
        var brandingInformation = new BrandingInformation();
        var dummyMc = new MultiLingualContent();
        dummyMc.setLanguage(new LanguageTag());
        brandingInformation.setTitle(Set.of(dummyMc));
        brandingInformation.setDescription(Set.of(dummyMc));

        when(brandingInformationRepository.findById(1)).thenReturn(
            Optional.of(brandingInformation));

        // when
        var result = brandingInformationService.readBrandingInformation();

        // then
        assertNotNull(result);
        verify(multilingualContentService, never()).getMultilingualContent(any());
    }

    @Test
    public void shouldUpdateBrandingInformationWhenDTOIsProvided() {
        // given
        var brandingInformationDTO = new BrandingInformationDTO(
            List.of(new MultilingualContentDTO()), List.of(new MultilingualContentDTO())
        );

        var existingBrandingInformation = new BrandingInformation();
        existingBrandingInformation.setTitle(Set.of(new MultiLingualContent()));
        existingBrandingInformation.setDescription(Set.of(new MultiLingualContent()));

        when(brandingInformationRepository.findAll()).thenReturn(
            List.of(existingBrandingInformation));
        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));

        // when
        brandingInformationService.updateBrandingInformation(brandingInformationDTO);

        // then
        verify(brandingInformationRepository, times(1)).findAll();
        verify(multilingualContentService, times(2)).getMultilingualContent(any());
    }

    @Test
    public void shouldCreateNewBrandingInformationWhenNoneExists() {
        // given
        var brandingInformationDTO = new BrandingInformationDTO(
            List.of(new MultilingualContentDTO()), List.of(new MultilingualContentDTO())
        );

        when(brandingInformationRepository.findAll()).thenReturn(Collections.emptyList());
        when(multilingualContentService.getMultilingualContent(any())).thenReturn(
            Set.of(new MultiLingualContent()));

        // when
        brandingInformationService.updateBrandingInformation(brandingInformationDTO);

        // then
        verify(brandingInformationRepository, times(1)).findAll();
        verify(multilingualContentService, times(2)).getMultilingualContent(any());
    }
}
