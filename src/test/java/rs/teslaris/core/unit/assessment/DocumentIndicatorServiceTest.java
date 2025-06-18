package rs.teslaris.core.unit.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.assessment.dto.indicator.DocumentIndicatorDTO;
import rs.teslaris.assessment.model.indicator.DocumentIndicator;
import rs.teslaris.assessment.model.indicator.Indicator;
import rs.teslaris.assessment.repository.indicator.DocumentIndicatorRepository;
import rs.teslaris.assessment.service.impl.cruddelegate.DocumentIndicatorJPAServiceImpl;
import rs.teslaris.assessment.service.impl.indicator.DocumentIndicatorServiceImpl;
import rs.teslaris.assessment.service.interfaces.indicator.IndicatorService;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.user.UserService;

@SpringBootTest
public class DocumentIndicatorServiceTest {

    @Mock
    private IndicatorService indicatorService;

    @Mock
    private DocumentIndicatorJPAServiceImpl documentIndicatorJPAService;

    @Mock
    private DocumentIndicatorRepository documentIndicatorRepository;

    @Mock
    private DocumentPublicationService documentPublicationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private DocumentIndicatorServiceImpl documentIndicatorService;


    @ParameterizedTest
    @EnumSource(value = AccessLevel.class, names = {"OPEN", "CLOSED", "ADMIN_ONLY"})
    void shouldReadAllDocumentIndicatorsForDocument(AccessLevel accessLevel) {
        // Given
        var documentId = 1;

        var indicator = new Indicator();
        indicator.setAccessLevel(AccessLevel.OPEN);

        var documentIndicator1 = new DocumentIndicator();
        documentIndicator1.setNumericValue(12d);
        documentIndicator1.setIndicator(indicator);

        var documentIndicator2 = new DocumentIndicator();
        documentIndicator2.setNumericValue(11d);
        documentIndicator2.setIndicator(indicator);

        when(
            documentIndicatorRepository.findIndicatorsForDocumentAndIndicatorAccessLevel(documentId,
                accessLevel)).thenReturn(
            List.of(documentIndicator1, documentIndicator2));

        // When
        var response =
            documentIndicatorService.getIndicatorsForDocument(documentId,
                accessLevel);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
    }

    @Test
    void shouldCreateDocumentIndicator() {
        var documentIndicatorDTO = new DocumentIndicatorDTO();
        documentIndicatorDTO.setNumericValue(20d);
        documentIndicatorDTO.setFromDate(LocalDate.of(2023, 3, 4));
        documentIndicatorDTO.setToDate(LocalDate.of(2023, 4, 4));
        documentIndicatorDTO.setDocumentId(1);
        documentIndicatorDTO.setIndicatorId(1);

        var newDocumentIndicator = new DocumentIndicator();
        newDocumentIndicator.setIndicator(new Indicator());

        when(documentPublicationService.findOne(1)).thenReturn(new Monograph());
        when(documentIndicatorJPAService.save(any(DocumentIndicator.class)))
            .thenReturn(newDocumentIndicator);
        when(indicatorService.findOne(1)).thenReturn(new Indicator());
        when(userService.findOne(1)).thenReturn(new User());

        var result = documentIndicatorService.createDocumentIndicator(
            documentIndicatorDTO, 1);

        assertNotNull(result);
        verify(documentIndicatorJPAService).save(any(DocumentIndicator.class));
        verify(userService).findOne(1);
    }

    @Test
    void shouldUpdateDocumentIndicator() {
        var documentIndicatorId = 1;
        var documentIndicatorDTO = new DocumentIndicatorDTO();
        documentIndicatorDTO.setNumericValue(20d);
        documentIndicatorDTO.setFromDate(LocalDate.of(2023, 3, 4));
        documentIndicatorDTO.setToDate(LocalDate.of(2023, 4, 4));
        documentIndicatorDTO.setDocumentId(1);
        documentIndicatorDTO.setIndicatorId(1);

        var existingDocumentIndicator = new DocumentIndicator();
        existingDocumentIndicator.setIndicator(new Indicator());

        when(documentIndicatorJPAService.findOne(documentIndicatorId)).thenReturn(
            existingDocumentIndicator);
        when(documentPublicationService.findOne(1)).thenReturn(new Monograph());
        when(indicatorService.findOne(1)).thenReturn(new Indicator());

        documentIndicatorService.updateDocumentIndicator(documentIndicatorId,
            documentIndicatorDTO);

        verify(documentIndicatorJPAService).findOne(documentIndicatorId);
        verify(documentIndicatorJPAService).save(existingDocumentIndicator);
    }
}
