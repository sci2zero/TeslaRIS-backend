package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.dto.document.OtherEventDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.document.OtherEvent;
import rs.teslaris.core.model.document.OtherEventType;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.repository.document.OtherEventRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.OtherEventServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.OtherEventJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class OtherEventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventIndexRepository eventIndexRepository;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private OtherEventRepository otherEventRepository;

    @Mock
    private OtherEventJPAServiceImpl otherEventJPAService;

    @Mock
    private SearchService<EventIndex> searchService;

    @Mock
    private CountryService countryService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private IndexBulkUpdateService indexBulkUpdateService;

    @Mock
    private CommissionRepository commissionRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private OtherEventServiceImpl otherEventService;


    @Test
    void shouldReturnOtherEventWhenExists() {
        var event = new OtherEvent();
        when(otherEventJPAService.findOne(1)).thenReturn(event);

        var result = otherEventService.findOtherEventById(1);

        assertEquals(event, result);
    }

    @Test
    void shouldThrowWhenOtherEventDoesNotExist() {
        when(otherEventJPAService.findOne(1)).thenThrow(NotFoundException.class);

        assertThrows(NotFoundException.class, () -> otherEventService.findOtherEventById(1));
    }

    @Test
    void shouldReadOtherEvent() {
        var event = new OtherEvent();
        event.setType(OtherEventType.CEREMONY);

        when(otherEventJPAService.findOne(1)).thenReturn(event);

        var result = otherEventService.readOtherEvent(1);

        assertEquals(OtherEventType.CEREMONY, result.getType());
    }

    @Test
    void shouldCreateOtherEvent() {
        var dto = new OtherEventDTO();
        dto.setName(new ArrayList<>());
        dto.setNameAbbreviation(new ArrayList<>());
        dto.setPlace(new ArrayList<>());
        dto.setCountryId(1);
        dto.setDateFrom(LocalDate.now());
        dto.setDateTo(LocalDate.now());
        dto.setContributions(new ArrayList<>());

        when(otherEventJPAService.save(any())).thenReturn(new OtherEvent());
        when(countryService.findOne(1)).thenReturn(new Country());

        var result = otherEventService.createOtherEvent(dto, true);

        assertNotNull(result);
        verify(otherEventJPAService).save(any());
    }

    @Test
    void shouldUpdateOtherEvent() {
        var event = new OtherEvent();
        var dto = new OtherEventDTO();
        dto.setName(new ArrayList<>());
        dto.setNameAbbreviation(new ArrayList<>());
        dto.setPlace(new ArrayList<>());
        dto.setDateFrom(LocalDate.now());
        dto.setDateTo(LocalDate.now());
        dto.setCountryId(1);

        when(otherEventJPAService.findOne(1)).thenReturn(event);
        when(countryService.findOne(1)).thenReturn(new Country());

        otherEventService.updateOtherEvent(1, dto);

        verify(otherEventJPAService).save(any());
    }

    @Test
    void shouldDeleteOtherEvent() {
        var event = new OtherEvent();
        event.setContributions(new HashSet<>());

        when(otherEventJPAService.findOne(1)).thenReturn(event);

        otherEventService.deleteOtherEvent(1);

        verify(otherEventJPAService).delete(1);
        verify(eventIndexRepository).findByDatabaseId(1);
    }

    @Test
    void shouldReindexOtherEvent() {
        var event = new OtherEvent();
        event.setId(1);
        event.setDateFrom(LocalDate.now());
        event.setDateTo(LocalDate.now());

        when(otherEventJPAService.findOne(1)).thenReturn(event);
        when(eventIndexRepository.findByDatabaseId(1)).thenReturn(Optional.of(new EventIndex()));

        otherEventService.reindexOtherEvent(1);

        verify(eventIndexRepository, atLeastOnce()).save(any());
    }

    @Test
    void shouldReindexAllOtherEvents() {
        var otherEvent1 = new OtherEvent();
        otherEvent1.setDateFrom(LocalDate.now());
        otherEvent1.setDateTo(LocalDate.now());

        var page = new PageImpl<>(List.of(otherEvent1));

        when(otherEventJPAService.findAll(any(PageRequest.class))).thenReturn(page);

        otherEventService.reindexOtherEvents();

        verify(otherEventJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(eventIndexRepository, atLeastOnce()).save(any());
    }
}
