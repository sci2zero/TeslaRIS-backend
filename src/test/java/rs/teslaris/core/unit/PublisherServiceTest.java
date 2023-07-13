package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.model.document.Publisher;
import rs.teslaris.core.repository.document.PublisherRepository;
import rs.teslaris.core.service.impl.document.PublisherServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.PublisherInUseException;

@SpringBootTest
public class PublisherServiceTest {

    @Mock
    private PublisherRepository publisherRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @InjectMocks
    private PublisherServiceImpl publisherService;

    private static Stream<Arguments> argumentSources() {
        return Stream.of(Arguments.of(true, false, false, false, false),
            Arguments.of(false, true, false, false, false),
            Arguments.of(false, false, true, false, false),
            Arguments.of(false, false, false, true, false),
            Arguments.of(false, false, false, false, true));
    }

    @Test
    public void shouldCreatePublisherWhenProvidedWithValidData() {
        // given
        var publisherDTO = new PublisherDTO();
        publisherDTO.setName(new ArrayList<>());
        publisherDTO.setPlace(new ArrayList<>());
        publisherDTO.setState(new ArrayList<>());
        var publisher = new Publisher();

        when(publisherRepository.save(any())).thenReturn(publisher);

        // when
        var result = publisherService.createPublisher(publisherDTO);

        // then
        assertEquals(publisher, result);
        verify(publisherRepository, times(1)).save(any());
    }

    @Test
    public void shouldUpdatePublisherWhenProvidedWithValidData() {
        // given
        var publisherDTO = new PublisherDTO();
        publisherDTO.setName(new ArrayList<>());
        publisherDTO.setPlace(new ArrayList<>());
        publisherDTO.setState(new ArrayList<>());
        var publisher = new Publisher();

        when(publisherRepository.findById(1)).thenReturn(Optional.of(publisher));

        // when
        publisherService.updatePublisher(publisherDTO, 1);

        // then
        verify(publisherRepository, times(1)).save(any());
    }

    @Test
    public void shouldReturnPublisherWhenItExists() {
        // given
        var expected = new Publisher();
        when(publisherRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var result = publisherService.findPublisherById(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenPublisherDoesNotExist() {
        // given
        when(publisherRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> publisherService.findPublisherById(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldDeletePublisherWhenUnused() {
        // given
        Integer publisherId = 1;
        Publisher publisher = new Publisher();
        publisher.setId(publisherId);

        when(publisherRepository.findById(publisherId)).thenReturn(Optional.of(publisher));
        when(publisherRepository.publishedDataset(publisherId)).thenReturn(false);
        when(publisherRepository.publishedPatent(publisherId)).thenReturn(false);
        when(publisherRepository.publishedProceedings(publisherId)).thenReturn(false);
        when(publisherRepository.publishedSoftware(publisherId)).thenReturn(false);
        when(publisherRepository.publishedThesis(publisherId)).thenReturn(false);

        // when
        publisherService.deletePublisher(publisherId);

        // then
        verify(publisherRepository).delete(publisher);
    }

    @ParameterizedTest
    @MethodSource("argumentSources")
    public void shouldThrowExceptionWhenPublisherIsUsed(boolean publishedDataset,
                                                        boolean publishedPatent,
                                                        boolean publishedProceedings,
                                                        boolean publishedSoftware,
                                                        boolean publishedThesis) {
        // given
        Integer publisherId = 1;
        Publisher publisher = new Publisher();
        publisher.setId(publisherId);

        when(publisherRepository.findById(publisherId)).thenReturn(Optional.of(publisher));
        when(publisherRepository.publishedDataset(publisherId)).thenReturn(publishedDataset);
        when(publisherRepository.publishedPatent(publisherId)).thenReturn(publishedPatent);
        when(publisherRepository.publishedProceedings(publisherId)).thenReturn(
            publishedProceedings);
        when(publisherRepository.publishedSoftware(publisherId)).thenReturn(publishedSoftware);
        when(publisherRepository.publishedThesis(publisherId)).thenReturn(publishedThesis);

        // when
        assertThrows(PublisherInUseException.class,
            () -> publisherService.deletePublisher(publisherId));

        // then (PublisherInUseException should be thrown)
    }
}
