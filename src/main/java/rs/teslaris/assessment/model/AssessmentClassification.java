package rs.teslaris.assessment.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "assessment_classifications", indexes = {
    @Index(name = "idx_assessment_classification_code", columnList = "code")
})
@SQLRestriction("deleted=false")
public class AssessmentClassification extends BaseEntity {

    @Column(name = "formal_description_of_rule")
    private String formalDescriptionOfRule;

    @Column(name = "code")
    private String code;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<ApplicableEntityType> applicableTypes = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> title = new HashSet<>();
}
