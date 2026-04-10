package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "courses")
@SQLRestriction("deleted=false")
public class Course extends Event {

    @Column(name = "course_level")
    private String courseLevel;

    @Column(name = "course_code")
    private String courseCode;

    @Column(name = "number_of_credits")
    private String numberOfCredits;

    @Column(name = "academic_year")
    private String academicYear;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> groupName = new HashSet<>();

    @Column(name = "number_of_students")
    private Integer numberOfStudents;
}
