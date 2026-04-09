package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.Course;
import rs.teslaris.core.repository.document.CourseRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class CourseJPAServiceImpl extends JPAServiceImpl<Course> {

    private final CourseRepository courseRepository;


    @Autowired
    public CourseJPAServiceImpl(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    protected JpaRepository<Course, Integer> getEntityRepository() {
        return courseRepository;
    }
}
