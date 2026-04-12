package rs.teslaris.core.repository.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {

    @Query(value = "SELECT * FROM courses c WHERE " +
        "(:allTime = TRUE OR c.last_modification >= CURRENT_TIMESTAMP - INTERVAL '1 DAY') " +
        " ORDER BY c.id", nativeQuery = true)
    Page<Course> findAllModified(Pageable pageable, boolean allTime);
}
