package rs.teslaris.core.dto.document;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO extends EventDTO {

    private String courseLevel;

    private String courseCode;

    private String numberOfCredits;

    private String academicYear;

    @NotNull(message = "You have to provide group name.")
    private List<MultilingualContentDTO> groupName = new ArrayList<>();

    private Integer numberOfStudents;
}
