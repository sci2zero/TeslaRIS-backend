package rs.teslaris.core.dto.person;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.MembershipDTO;

/**
 * The shape a person revision is captured in.
 * <p>
 * {@link PersonResponseDTO} is the read endpoint's body and deliberately carries involvements as
 * bare IDs, because the frontend fetches them separately. A revision cannot work from IDs - it has
 * to hold the involvements themselves, or every restore would silently leave them behind. Keeping
 * that on a subclass means the read endpoint stays as narrow as it is, while
 * {@code DataQualityCalculator.resolveAssessor} still finds the person assessor by walking up to
 * the superclass.
 * <p>
 * A null involvement list means "this revision predates involvement capture" and is left untouched
 * by a restore; an empty one means the person genuinely had none.
 */
@Getter
@Setter
@NoArgsConstructor
public class PersonSnapshotDTO extends PersonResponseDTO {

    private List<EmploymentDTO> employments;

    private List<EducationDTO> educations;

    private List<MembershipDTO> memberships;


    public PersonSnapshotDTO(PersonResponseDTO base) {
        super(base);
    }

    public PersonSnapshotDTO(PersonSnapshotDTO other) {
        super(other);

        this.employments = copy(other.employments);
        this.educations = copy(other.educations);
        this.memberships = copy(other.memberships);
    }

    /**
     * Involvement DTOs are only ever read after a copy, so sharing the elements is safe - what has
     * to be independent is the list itself, so that a copy taken before an edit does not follow the
     * original when an involvement is added or removed.
     */
    private static <T> List<T> copy(List<T> source) {
        return Objects.nonNull(source) ? new ArrayList<>(source) : null;
    }
}
