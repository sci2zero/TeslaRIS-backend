package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.PrizeDTO;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.Prize;
import rs.teslaris.core.repository.person.PrizeRepository;
import rs.teslaris.core.service.impl.person.PrizeServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.DocumentFileService;
import rs.teslaris.core.service.interfaces.person.PersonService;

@SpringBootTest
public class PrizeServiceTest {

    @Mock
    private PrizeRepository prizeRepository;

    @Mock
    private PersonService personService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private DocumentFileService documentFileService;

    @InjectMocks
    private PrizeServiceImpl prizeService;


    @Test
    public void shouldAddPrize() {
        // Given
        var dto = new PrizeDTO();

        var person = new Person();

        var newPrize = new Prize();

        when(personService.findOne(1)).thenReturn(person);
        when(prizeRepository.save(any(Prize.class))).thenReturn(
            newPrize);

        // When
        var responseDTO = prizeService.addPrize(1, dto);

        // Then
        assertNotNull(responseDTO);
    }

    @Test
    public void shouldUpdatePrize() {
        // Given
        PrizeDTO dto = new PrizeDTO();

        Prize prize = new Prize();
        prize.setId(1);

        when(prizeRepository.findById(1)).thenReturn(
            Optional.of(new Prize()));
        when(prizeRepository.save(any(Prize.class))).thenReturn(
            prize);

        // When
        var responseDTO = prizeService.updatePrize(1, dto);

        // Then
        assertNotNull(responseDTO);
    }

    @Test
    public void shouldDeletePrize() {
        // Given
        var person = new Person();
        var prize = new Prize();
        prize.setId(1);
        var prizes = new HashSet<Prize>();
        prizes.add(prize);
        person.setPrizes(prizes);

        when(personService.findOne(1)).thenReturn(person);
        when(prizeRepository.findById(1)).thenReturn(
            Optional.of(new Prize()));
        doNothing().when(prizeRepository).deleteById(1);

        // When
        prizeService.deletePrize(1, 1);

        // Then
        assertTrue(person.getPrizes().isEmpty());
    }

    @Test
    public void shouldAddProof() {
        // Given
        var proof = new DocumentFileDTO();
        var prize = new Prize();
        prize.setId(1);
        prize.setPerson(new Person());
        var documentFile = new DocumentFile();
        documentFile.setId(1);

        when(prizeRepository.findById(1)).thenReturn(Optional.of(prize));
        when(documentFileService.saveNewPersonalDocument(proof, false,
            prize.getPerson())).thenReturn(documentFile);

        // When
        var responseDTO = prizeService.addProof(1, proof);

        // Then
        assertNotNull(responseDTO);
        assertEquals(1, responseDTO.getId());
    }

    @Test
    public void shouldUpdateProof() {
        // Given
        var updatedProof = new DocumentFileDTO();
        updatedProof.setId(1);

        var documentFile = new DocumentFileResponseDTO();
        documentFile.setId(1);

        when(documentFileService.editDocumentFile(updatedProof, false)).thenReturn(documentFile);

        // When
        var responseDTO = prizeService.updateProof(updatedProof);

        // Then
        assertNotNull(responseDTO);
        assertEquals(1, responseDTO.getId());
    }

    @Test
    public void shouldDeleteProof() {
        // Given
        Prize prize = new Prize();
        DocumentFile documentFile = new DocumentFile();
        documentFile.setId(1);
        prize.getProofs().add(documentFile);

        when(prizeRepository.findById(1)).thenReturn(Optional.of(prize));
        when(documentFileService.findDocumentFileById(1)).thenReturn(documentFile);
        doNothing().when(documentFileService).deleteDocumentFile(anyString());

        // When
        prizeService.deleteProof(1, 1);

        // Then
        assertTrue(prize.getProofs().isEmpty());
    }
}
