package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.dto.document.PublisherBasicAdditionDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.indexmodel.PublisherIndex;
import rs.teslaris.core.indexrepository.PublisherIndexRepository;
import rs.teslaris.core.model.document.Publisher;
import rs.teslaris.core.repository.document.PublisherRepository;
import rs.teslaris.core.service.impl.document.PublisherServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.PublisherReferenceConstraintViolationException;

@SpringBootTest
public class PublisherServiceTest {

    @Mock
    PublisherIndexRepository publisherIndexRepository;

    @Mock
    private PublisherRepository publisherRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private EmailUtil emailUtil;

    @Mock
    private SearchService<PublisherIndex> searchService;

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
        // Given
        var publisherDTO = new PublisherDTO();
        publisherDTO.setName(new ArrayList<>());
        publisherDTO.setPlace(new ArrayList<>());
        publisherDTO.setState(new ArrayList<>());

        var publisher = new Publisher();
        publisher.setName(new HashSet<>());
        publisher.setPlace(new HashSet<>());
        publisher.setState(new HashSet<>());

        when(publisherRepository.save(any())).thenReturn(publisher);

        // When
        var result = publisherService.createPublisher(publisherDTO, true);

        // Then
        assertEquals(publisher, result);
        verify(publisherRepository, times(1)).save(any());
    }

    @Test
    public void shouldCreatePublisherBasicWhenProvidedWithValidData() {
        // given
        var publisherDTO = new PublisherBasicAdditionDTO();
        publisherDTO.setName(new ArrayList<>());
        publisherDTO.setState(new ArrayList<>());
        var publisher = new Publisher();
        publisher.setId(1);

        when(publisherRepository.save(any())).thenReturn(publisher);

        // when
        var result = publisherService.createPublisher(publisherDTO);

        // then
        assertEquals(publisher, result);
        verify(publisherRepository, times(1)).save(any());
        verify(publisherIndexRepository, times(1)).save(any());
        verify(emailUtil, times(1)).notifyInstitutionalEditor(1, "publisher");
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
        verify(publisherIndexRepository, times(1)).save(any());
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
        // Given
        var publisherId = 1;
        var publisher = new Publisher();
        publisher.setId(publisherId);

        when(publisherRepository.findById(publisherId)).thenReturn(Optional.of(publisher));
        when(publisherRepository.hasPublishedDataset(publisherId)).thenReturn(false);
        when(publisherRepository.hasPublishedPatent(publisherId)).thenReturn(false);
        when(publisherRepository.hasPublishedProceedings(publisherId)).thenReturn(false);
        when(publisherRepository.hasPublishedSoftware(publisherId)).thenReturn(false);
        when(publisherRepository.hasPublishedThesis(publisherId)).thenReturn(false);
        when(publisherIndexRepository.findByDatabaseId(publisherId)).thenReturn(
            Optional.of(new PublisherIndex()));

        // When
        publisherService.deletePublisher(publisherId);

        // Then
        verify(publisherRepository).save(publisher);
        verify(publisherIndexRepository, times(1)).delete(any());
        verify(publisherRepository, never()).delete(publisher);
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

    @Test
    public void shouldFindPublishersWhenSearchingWithSimpleQuery() {
        // Given
        var tokens = Arrays.asList("ključna", "ријеч", "keyword");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(
                List.of(new PublisherIndex(), new PublisherIndex())));

        // When
        var result = publisherService.searchPublishers(new ArrayList<>(tokens), pageable);

        // Then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    public void shouldReindexPublishers() {
        // Given
        var publisher1 = new Publisher();
        publisher1.setName(new HashSet<>());
        publisher1.setPlace(new HashSet<>());
        publisher1.setState(new HashSet<>());
        var publisher2 = new Publisher();
        publisher2.setName(new HashSet<>());
        publisher2.setPlace(new HashSet<>());
        publisher2.setState(new HashSet<>());
        var publisher3 = new Publisher();
        publisher3.setName(new HashSet<>());
        publisher3.setPlace(new HashSet<>());
        publisher3.setState(new HashSet<>());
        var publishers = Arrays.asList(publisher1, publisher2, publisher3);
        var page1 =
            new PageImpl<>(publishers.subList(0, 2), PageRequest.of(0, 10), publishers.size());
        var page2 =
            new PageImpl<>(publishers.subList(2, 3), PageRequest.of(1, 10), publishers.size());

        when(publisherRepository.findAll(any(PageRequest.class))).thenReturn(page1, page2);

        // When
        publisherService.reindexPublishers();

        // Then
        verify(publisherIndexRepository, times(1)).deleteAll();
        verify(publisherRepository, atLeastOnce()).findAll(any(PageRequest.class));
        verify(publisherIndexRepository, atLeastOnce()).save(any(PublisherIndex.class));
    }
}
