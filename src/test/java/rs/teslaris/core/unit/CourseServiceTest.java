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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.dto.document.CourseDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.document.Course;
import rs.teslaris.core.repository.document.CourseRepository;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.CourseServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.CourseJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class CourseServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventIndexRepository eventIndexRepository;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseJPAServiceImpl courseJPAService;

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

    @InjectMocks
    private CourseServiceImpl courseService;


    @Test
    void shouldReturnCourseWhenExists() {
        var course = new Course();
        when(courseJPAService.findOne(1)).thenReturn(course);

        var result = courseService.findCourseById(1);

        assertEquals(course, result);
    }

    @Test
    void shouldThrowWhenCourseDoesNotExist() {
        when(courseJPAService.findOne(1)).thenThrow(NotFoundException.class);

        assertThrows(NotFoundException.class, () -> courseService.findCourseById(1));
    }

    @Test
    void shouldReadCourse() {
        var course = new Course();
        course.setCourseCode("CS101");

        when(courseJPAService.findOne(1)).thenReturn(course);

        var result = courseService.readCourse(1);

        assertEquals("CS101", result.getCourseCode());
    }

    @Test
    void shouldCreateCourse() {
        var dto = new CourseDTO();
        dto.setName(new ArrayList<>());
        dto.setNameAbbreviation(new ArrayList<>());
        dto.setPlace(new ArrayList<>());
        dto.setCountryId(1);
        dto.setDateFrom(LocalDate.now());
        dto.setDateTo(LocalDate.now());
        dto.setContributions(new ArrayList<>());

        when(courseJPAService.save(any())).thenReturn(new Course());
        when(countryService.findOne(1)).thenReturn(new Country());

        var result = courseService.createCourse(dto, true);

        assertNotNull(result);
        verify(courseJPAService).save(any());
    }

    @Test
    void shouldUpdateCourse() {
        var course = new Course();
        var dto = new CourseDTO();
        dto.setName(new ArrayList<>());
        dto.setNameAbbreviation(new ArrayList<>());
        dto.setPlace(new ArrayList<>());
        dto.setDateFrom(LocalDate.now());
        dto.setDateTo(LocalDate.now());
        dto.setCountryId(1);

        when(courseJPAService.findOne(1)).thenReturn(course);
        when(countryService.findOne(1)).thenReturn(new Country());

        courseService.updateCourse(1, dto);

        verify(courseJPAService).save(any());
    }

    @Test
    void shouldDeleteCourse() {
        var course = new Course();
        course.setContributions(new HashSet<>());

        when(courseJPAService.findOne(1)).thenReturn(course);

        courseService.deleteCourse(1);

        verify(courseJPAService).delete(1);
        verify(eventIndexRepository).findByDatabaseId(1);
    }

    @Test
    void shouldReindexCourse() {
        var course = new Course();
        course.setId(1);
        course.setDateFrom(LocalDate.now());
        course.setDateTo(LocalDate.now());

        when(courseJPAService.findOne(1)).thenReturn(course);
        when(eventIndexRepository.findByDatabaseId(1)).thenReturn(Optional.of(new EventIndex()));

        courseService.reindexCourse(1);

        verify(eventIndexRepository, atLeastOnce()).save(any());
    }

    @Test
    void shouldReindexAllCourses() {
        var course1 = new Course();
        course1.setDateFrom(LocalDate.now());
        course1.setDateTo(LocalDate.now());

        var page = new PageImpl<>(List.of(course1));

        when(courseJPAService.findAll(any(PageRequest.class))).thenReturn(page);

        courseService.reindexCourses();

        verify(courseJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(eventIndexRepository, atLeastOnce()).save(any());
    }
}
