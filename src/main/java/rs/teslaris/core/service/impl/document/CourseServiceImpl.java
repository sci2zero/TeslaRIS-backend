package rs.teslaris.core.service.impl.document;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.CourseConverter;
import rs.teslaris.core.dto.document.CourseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.document.Course;
import rs.teslaris.core.model.document.PersonContribution;
import rs.teslaris.core.repository.document.CourseRepository;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.repository.document.EventsRelationRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.cruddelegate.CourseJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.CourseService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.functional.Triple;

@Service
@Traceable
@Slf4j
public class CourseServiceImpl extends EventServiceImpl implements CourseService {

    private final CourseJPAServiceImpl courseJPAService;

    private final CourseRepository courseRepository;


    @Autowired
    public CourseServiceImpl(
        EventIndexRepository eventIndexRepository,
        MultilingualContentService multilingualContentService,
        PersonContributionService personContributionService,
        EventRepository eventRepository,
        IndexBulkUpdateService indexBulkUpdateService,
        CommissionRepository commissionRepository,
        EventsRelationRepository eventsRelationRepository,
        SearchService<EventIndex> searchService,
        CountryService countryService,
        OrganisationUnitService organisationUnitService,
        DocumentPublicationIndexRepository documentPublicationIndexRepository,
        ResearchAreaService researchAreaService,
        CourseJPAServiceImpl courseJPAService,
        CourseRepository courseRepository
    ) {
        super(eventIndexRepository, multilingualContentService, personContributionService,
            eventRepository, indexBulkUpdateService, commissionRepository,
            documentPublicationIndexRepository, eventsRelationRepository, searchService,
            countryService, organisationUnitService,
            researchAreaService);

        this.courseJPAService = courseJPAService;
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTO> readAllCourses(Pageable pageable) {
        return courseJPAService.findAll(pageable).map(CourseConverter::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseDTO readCourse(Integer courseId) {
        Course course;
        try {
            course = findCourseById(courseId);
        } catch (NotFoundException e) {
            eventIndexRepository.findByEventTypeAndDatabaseId(EventType.COURSE, courseId)
                .ifPresent(eventIndexRepository::delete);
            throw e;
        }

        return CourseConverter.toDTO(course);
    }

    @Override
    public Page<EventIndex> searchCoursesForImport(List<String> names, String dateFrom,
                                                   String dateTo) {
        return searchEventsImport(names, dateFrom, dateTo);
    }

    @Transactional
    public Course findCourseById(Integer courseId) {
        return courseJPAService.findOne(courseId);
    }

    @Override
    @Transactional
    public Course createCourse(CourseDTO dto, Boolean index) {
        var course = new Course();

        setEventCommonFields(course, dto);
        setCourseFields(course, dto);

        var saved = courseJPAService.save(course);

        if (index) {
            indexCourse(saved, new EventIndex());
        }

        return saved;
    }

    @Override
    @Transactional
    public void updateCourse(Integer id, CourseDTO dto) {
        var course = findCourseById(id);

        clearEventCommonFields(course);
        setEventCommonFields(course, dto);
        setCourseFields(course, dto);

        courseJPAService.save(course);

        var index = eventIndexRepository.findByDatabaseId(id).orElse(new EventIndex());
        clearEventIndexCommonFields(index);
        indexCourse(course, index);
    }

    @Override
    @Transactional
    public void deleteCourse(Integer id) {
        var course = courseJPAService.findOne(id);

        course.getContributions().forEach(c -> {
            c.setDeleted(true);
            personContributionService.save(c);
        });

        courseJPAService.delete(id);
        eventIndexRepository.findByDatabaseId(id).ifPresent(eventIndexRepository::delete);
    }

    @Override
    @Transactional
    public void forceDeleteCourse(Integer courseId) {
        courseJPAService.delete(courseId);

        var index = eventIndexRepository.findByDatabaseId(courseId);
        index.ifPresent(eventIndexRepository::delete);

        documentPublicationIndexRepository.deleteByEventIdAndType(courseId,
            DocumentPublicationType.PROCEEDINGS.name());

        indexBulkUpdateService.removeIdFromRecord("document_publication", "event_id", courseId);
    }

    @Override
    @Async("reindexExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<Void> reindexCourses() {
        FunctionalUtil.performBulkOperation(
            courseJPAService::findAll,
            Sort.by(Sort.Direction.ASC, "id"),
            course -> indexCourse(course, new EventIndex())
        );

        return null;
    }

    @Override
    public void indexCourse(Course course) {
        eventIndexRepository.findByDatabaseId(course.getId())
            .ifPresent(index -> {
                indexCourse(course, index);
                reindexVolatileCourseInformation(index.getDatabaseId());

                eventIndexRepository.save(index);
            });
    }

    @Override
    @Transactional
    public void reindexCourse(Integer courseId) {
        var courseToIndex = courseJPAService.findOne(courseId);
        var indexToUpdate =
            eventIndexRepository.findByDatabaseId(courseId).orElse(new EventIndex());
        indexCourse(courseToIndex, indexToUpdate);
        reindexVolatileCourseInformation(courseId);

        eventIndexRepository.save(indexToUpdate);
    }

    @Override
    @Transactional
    public void reindexVolatileCourseInformation(Integer courseId) {
        eventIndexRepository.findByDatabaseId(courseId).ifPresent(eventIndex -> {
            eventIndex.getRelatedInstitutionIds().addAll(
                eventRepository.findInstitutionIdsByEventIdAndEventContribution(courseId)
                    .stream().toList()
            );

            eventIndex.setClassifiedBy(
                commissionRepository.findCommissionsThatClassifiedEvent(courseId));

            eventIndex.getCommissionAssessments().clear();
            commissionRepository.findAssessmentClassificationBasicInfoForEventAndCommissions(
                courseId, eventIndex.getClassifiedBy()).forEach(assessment ->
                eventIndex.getCommissionAssessments().add(
                    new Triple<>(assessment.commissionId(),
                        assessment.assessmentCode(),
                        assessment.manual())));

            eventIndexRepository.save(eventIndex);
        });
    }

    @Override
    @Transactional
    public void reorderCourseContributions(Integer courseId, Integer contributionId,
                                           Integer oldContributionOrderNumber,
                                           Integer newContributionOrderNumber) {
        var event = courseRepository.findById(courseId);

        if (event.isEmpty()) {
            return;
        }

        var contributions = event.get().getContributions().stream()
            .map(contribution -> (PersonContribution) contribution).collect(
                Collectors.toSet());

        personContributionService.reorderContributions(contributions, contributionId,
            oldContributionOrderNumber, newContributionOrderNumber);
    }

    @Override
    @Transactional
    public boolean isIdentifierInUse(String identifier, Integer courseId) {
        return false; // Always false, until we decide to add course identifiers
    }

    private void setCourseFields(Course course, CourseDTO dto) {
        course.setCourseLevel(dto.getCourseLevel());
        course.setCourseCode(dto.getCourseCode());
        course.setNumberOfCredits(dto.getNumberOfCredits());
        course.setAcademicYear(dto.getAcademicYear());
        course.setNumberOfStudents(dto.getNumberOfStudents());

        course.getGroupName().clear();
        course.getGroupName().addAll(
            multilingualContentService.getMultilingualContent(dto.getGroupName())
        );
    }

    private void indexCourse(Course course, EventIndex index) {
        index.setDatabaseId(course.getId());
        index.setEventType(EventType.COURSE);

        indexEventCommonFields(index, course);
        eventIndexRepository.save(index);
    }
}
