package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.model.document.Publisher;
import rs.teslaris.core.repository.document.PublisherRepository;
import rs.teslaris.core.service.impl.document.PublisherServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.PublisherReferenceConstraintViolationException;

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
        when(publisherRepository.hasPublishedDataset(publisherId)).thenReturn(false);
        when(publisherRepository.hasPublishedPatent(publisherId)).thenReturn(false);
        when(publisherRepository.hasPublishedProceedings(publisherId)).thenReturn(false);
        when(publisherRepository.hasPublishedSoftware(publisherId)).thenReturn(false);
        when(publisherRepository.hasPublishedThesis(publisherId)).thenReturn(false);

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
        when(publisherRepository.hasPublishedDataset(publisherId)).thenReturn(publishedDataset);
        when(publisherRepository.hasPublishedPatent(publisherId)).thenReturn(publishedPatent);
        when(publisherRepository.hasPublishedProceedings(publisherId)).thenReturn(
            publishedProceedings);
        when(publisherRepository.hasPublishedSoftware(publisherId)).thenReturn(publishedSoftware);
        when(publisherRepository.hasPublishedThesis(publisherId)).thenReturn(publishedThesis);

        // when
        assertThrows(PublisherReferenceConstraintViolationException.class,
            () -> publisherService.deletePublisher(publisherId));

        // then (PublisherInUseException should be thrown)
    }

    @Test
    public void shouldReadAllPublishers() {
        // given
        var pageable = Pageable.ofSize(5);
        var publisher1 = new Publisher();
        publisher1.setName(new HashSet<>());
        publisher1.setPlace(new HashSet<>());
        publisher1.setState(new HashSet<>());
        var publisher2 = new Publisher();
        publisher2.setName(new HashSet<>());
        publisher2.setPlace(new HashSet<>());
        publisher2.setState(new HashSet<>());

        when(publisherRepository.findAll(pageable)).thenReturn(
            new PageImpl<>(List.of(publisher1, publisher2)));

        // when
        var response = publisherService.readAllPublishers(pageable);

        // then
        assertEquals(2, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
    }
}
