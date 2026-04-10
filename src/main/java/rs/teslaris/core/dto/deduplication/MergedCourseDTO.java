package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.CourseDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedCourseDTO {

    private CourseDTO leftCourse;

    private CourseDTO rightCourse;
}
