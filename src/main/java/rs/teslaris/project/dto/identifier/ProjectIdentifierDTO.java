package rs.teslaris.project.dto.identifier;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.identifier.EntityIdentifierDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectIdentifierDTO extends EntityIdentifierDTO {

    @NotNull(message = "You have to provide project ID.")
    @Positive(message = "Project ID must be a positive number.")
    private Integer projectId;


    public ProjectIdentifierDTO(String value, Integer identifierId, Integer projectId) {
        super(value, identifierId);
        this.projectId = projectId;
    }
}
