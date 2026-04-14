package rs.teslaris.core.converter.document;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.CourseDTO;
import rs.teslaris.core.model.document.Course;

public class CourseConverter extends EventConverter {

    public static CourseDTO toDTO(Course course) {
        var dto = fillCommonFields(course, new CourseDTO());

        dto.setCourseLevel(course.getCourseLevel());
        dto.setCourseCode(course.getCourseCode());
        dto.setNumberOfCredits(course.getNumberOfCredits());
        dto.setAcademicYear(course.getAcademicYear());
        dto.setNumberOfStudents(course.getNumberOfStudents());

        dto.setGroupName(
            MultilingualContentConverter.getMultilingualContentDTO(course.getGroupName())
        );

        return dto;
    }
}
