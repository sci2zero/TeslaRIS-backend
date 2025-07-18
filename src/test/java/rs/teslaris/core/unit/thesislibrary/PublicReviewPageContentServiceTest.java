package rs.teslaris.core.unit.thesislibrary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.thesislibrary.converter.PublicReviewPageContentConverter;
import rs.teslaris.thesislibrary.dto.PublicReviewPageContentDTO;
import rs.teslaris.thesislibrary.model.PageContentType;
import rs.teslaris.thesislibrary.model.PublicReviewPageContent;
import rs.teslaris.thesislibrary.repository.PublicReviewPageContentRepository;
import rs.teslaris.thesislibrary.service.impl.PublicReviewPageContentServiceImpl;

@SpringBootTest
class PublicReviewPageContentServiceTest {

    @Mock
    private PublicReviewPageContentRepository publicReviewPageContentRepository;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @InjectMocks
    private PublicReviewPageContentServiceImpl service;


    @Test
    void shouldReturnDTOsWhenReadingConfigurationForInstitution() {
        var entity = new PublicReviewPageContent();
        var dto = mock(PublicReviewPageContentDTO.class);
        var list = List.of(entity);

        when(publicReviewPageContentRepository.getConfigurationForInstitution(1))
            .thenReturn(list);

        try (MockedStatic<PublicReviewPageContentConverter> converter = mockStatic(
            PublicReviewPageContentConverter.class)) {
            converter.when(() -> PublicReviewPageContentConverter.toDTO(entity))
                .thenReturn(dto);

            List<PublicReviewPageContentDTO> result =
                service.readPageContentConfigurationForInstitution(1);

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());
        }

        verify(publicReviewPageContentRepository).getConfigurationForInstitution(1);
    }

    @Test
    void shouldReturnDTOsWhenReadingConfigurationForInstitutionAndType() {
        var entity = new PublicReviewPageContent();
        var dto = mock(PublicReviewPageContentDTO.class);
        var list = List.of(entity);
        var thesisTypes = List.of(ThesisType.PHD);

        when(publicReviewPageContentRepository.getConfigurationForInstitutionAndThesisTypes(2,
            thesisTypes))
            .thenReturn(list);

        try (MockedStatic<PublicReviewPageContentConverter> converter = mockStatic(
            PublicReviewPageContentConverter.class)) {
            converter.when(() -> PublicReviewPageContentConverter.toDTO(entity))
                .thenReturn(dto);

            List<PublicReviewPageContentDTO> result =
                service.readPageContentConfigurationForInstitutionAndType(2, thesisTypes);

            assertEquals(1, result.size());
            assertEquals(dto, result.getFirst());
        }

        verify(publicReviewPageContentRepository).getConfigurationForInstitutionAndThesisTypes(2,
            thesisTypes);
    }

    @Test
    void shouldSavePageConfiguration() {
        var institutionId = 5;
        var institution = mock(OrganisationUnit.class);

        var contentDto = mock(PublicReviewPageContentDTO.class);

        when(contentDto.contentType()).thenReturn(PageContentType.TEXT);
        when(contentDto.thesisTypes()).thenReturn(List.of(ThesisType.PHD_ART_PROJECT));
        when(contentDto.content()).thenReturn(new ArrayList<>());
        when(organisationUnitService.findOne(institutionId)).thenReturn(institution);
        when(multilingualContentService.getMultilingualContent(any())).thenReturn(new HashSet<>());

        service.savePageConfiguration(List.of(contentDto), institutionId);

        verify(publicReviewPageContentRepository).deleteAllContentForInstitution(institutionId);
        verify(publicReviewPageContentRepository).save(any(PublicReviewPageContent.class));
        verify(organisationUnitService).findOne(institutionId);
        verify(multilingualContentService).getMultilingualContent(any());
    }
}
