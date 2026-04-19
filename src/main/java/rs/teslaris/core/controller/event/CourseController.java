package rs.teslaris.core.controller.event;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.ReorderContributionRequestDTO;
import rs.teslaris.core.dto.document.CourseDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.document.CourseService;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.search.StringUtil;

@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
@Traceable
public class CourseController {

    private final CourseService courseService;

    private final DeduplicationService deduplicationService;

    private final JwtUtil tokenUtil;


    @GetMapping("/{courseId}/can-edit")
    @PreAuthorize("hasAuthority('EDIT_COURSES')")
    public boolean canEditCourse() {
        return true;
    }

    @GetMapping("/{courseId}/can-classify")
    @PreAuthorize("hasAuthority('EDIT_EVENT_ASSESSMENT_CLASSIFICATION') and hasAuthority('EDIT_EVENT_INDICATORS')")
    public boolean canClassifyCourse() {
        return true;
    }

    @GetMapping
    public Page<CourseDTO> readAll(Pageable pageable) {
        return courseService.readAllCourses(pageable);
    }

    @GetMapping("/import-search")
    Page<EventIndex> searchCoursesImport(
        @RequestParam("names") List<String> names,
        @RequestParam("dateFrom") String dateFrom,
        @RequestParam("dateTo") String dateTo) {
        StringUtil.sanitizeTokens(names);
        return courseService.searchCoursesForImport(names, dateFrom, dateTo);
    }

    @GetMapping("/{courseId}")
    public CourseDTO readCourse(@PathVariable Integer courseId) {
        return courseService.readCourse(courseId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('CREATE_COURSES', 'EDIT_COURSES')")
    @Idempotent
    public CourseDTO createCourse(@RequestBody @Valid CourseDTO courseDTO,
                                  @RequestHeader(value = "Authorization", required = false)
                                  String bearerToken) {
        if (!tokenUtil.extractUserRoleFromToken(bearerToken).equals(UserRole.ADMIN.name()) &&
            !tokenUtil.extractUserRoleFromToken(bearerToken)
                .equals(UserRole.INSTITUTIONAL_EDITOR.name())) {
            courseDTO.setSerialEvent(false);
        }

        var newCourse = courseService.createCourse(courseDTO, true);
        courseDTO.setId(newCourse.getId());
        return courseDTO;
    }

    @PutMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_COURSES')")
    public void updateCourse(@PathVariable Integer courseId,
                             @RequestBody @Valid CourseDTO courseDTO) {
        courseService.updateCourse(courseId, courseDTO);
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('EDIT_COURSES')")
    public void deleteCourse(@PathVariable Integer courseId) {
        courseService.deleteCourse(courseId);
        deduplicationService.deleteSuggestion(courseId, EntityType.EVENT);
    }

    @DeleteMapping("/force/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('FORCE_DELETE_ENTITIES')")
    public void forceDeleteCourse(@PathVariable Integer courseId) {
        courseService.forceDeleteCourse(courseId);
        deduplicationService.deleteSuggestion(courseId, EntityType.EVENT);
    }

    @PatchMapping("/{courseId}/reorder-contribution/{contributionId}")
    @PreAuthorize("hasAuthority('EDIT_COURSES')")
    void reorderCourseContributions(@PathVariable Integer courseId,
                                    @PathVariable Integer contributionId,
                                    @RequestBody ReorderContributionRequestDTO reorderRequest) {
        courseService.reorderCourseContributions(courseId, contributionId,
            reorderRequest.getOldContributionOrderNumber(),
            reorderRequest.getNewContributionOrderNumber());
    }

    @GetMapping("/identifier-usage/{courseId}")
    @PreAuthorize("hasAuthority('EDIT_COURSES')")
    public boolean checkIdentifierUsage(@PathVariable Integer courseId,
                                        @RequestParam String identifier) {
        return courseService.isIdentifierInUse(identifier, courseId);
    }
}
