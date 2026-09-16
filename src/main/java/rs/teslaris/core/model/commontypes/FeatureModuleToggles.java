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
@Table(name = "feature_module_toggles")
public class FeatureModuleToggles extends BaseEntity {

    @Column(name = "toggle_assessment_module")
    private Boolean toggleAssessmentModule = true;

    @Column(name = "toggle_digital_library")
    private Boolean toggleDigitalLibrary = false;

    @Column(name = "toggle_digital_repository")
    private Boolean toggleDigitalRepository = false;
}
