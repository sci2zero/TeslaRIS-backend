package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.dto.document.CourseDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.model.document.Course;

public interface CourseService {

    Page<CourseDTO> readAllCourses(Pageable pageable);

    Page<EventIndex> searchCoursesForImport(List<String> names, String dateFrom, String dateTo);

    CourseDTO readCourse(Integer courseId);

    Course createCourse(CourseDTO dto, Boolean index);

    void updateCourse(Integer id, CourseDTO dto);

    void deleteCourse(Integer id);

    void forceDeleteCourse(Integer courseId);

    CompletableFuture<Void> reindexCourses();

    void reindexCourse(Integer courseId);

    void reindexVolatileCourseInformation(Integer courseId);

    void reorderCourseContributions(Integer courseId, Integer contributionId,
                                    Integer oldContributionOrderNumber,
                                    Integer newContributionOrderNumber);

    boolean isIdentifierInUse(String identifier, Integer courseId);
}
