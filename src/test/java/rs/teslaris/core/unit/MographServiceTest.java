package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.service.impl.document.MonographServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.MonographJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.document.BookSeriesService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class MographServiceTest {

    @Mock
    private MonographJPAServiceImpl monographJPAService;

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private JournalService journalService;

    @Mock
    private BookSeriesService bookSeriesService;

    @Mock
    private ResearchAreaService researchAreaService;

    @InjectMocks
    private MonographServiceImpl monographService;


    @Test
    void shouldReadMonographByIdWhenMonographIsApproved() {
        // Given
        var monographId = 1;
        var monograph = new Monograph();
        monograph.setId(monographId);
        monograph.setApproveStatus(ApproveStatus.APPROVED);

        when(monographJPAService.findOne(monographId)).thenReturn(monograph);

        // When
        var monographDTO = monographService.readMonographById(monographId);

        // Then
        assertNotNull(monographDTO);
        verify(monographJPAService, times(1)).findOne(monographId);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenMonographIsDeclined() {
        // Given
        var monographId = 1;
        var monograph = new Monograph();
        monograph.setId(monographId);
        monograph.setApproveStatus(ApproveStatus.DECLINED);

        when(monographJPAService.findOne(monographId)).thenReturn(monograph);

        // When
        assertThrows(NotFoundException.class,
            () -> monographService.readMonographById(monographId));

        // Then (NotFoundException should be thrown)
        verify(monographJPAService, times(1)).findOne(monographId);
    }
}
