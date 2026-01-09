package rs.teslaris.core.model.commontypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "application_configuration")
public class ApplicationConfiguration extends BaseEntity {

    @Column(name = "is_in_maintenance_mode")
    private Boolean isInMaintenanceMode = false;
}
