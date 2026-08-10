package rs.teslaris.revisioner.restorer.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.CourseDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.document.CourseService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class CourseRevisionRestorer implements RevisionRestorer<CourseDTO> {

    private final CourseService courseService;


    @Override
    public String entityType() {
        return EntityType.COURSE.name();
    }

    @Override
    public Class<CourseDTO> dtoClass() {
        return CourseDTO.class;
    }

    @Override
    public void restore(Integer entityId, CourseDTO dto) {
        courseService.updateCourse(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return courseService.readCourse(entityId);
    }
}
