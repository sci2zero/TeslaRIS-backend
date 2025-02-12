package rs.teslaris.core.assessment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "commission_relations")
@SQLRestriction("deleted=false")
public class CommissionRelation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_commission", nullable = false)
    private Commission sourceCommission;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "commission_relation_targets",
        joinColumns = @JoinColumn(name = "commission_relation_id"),
        inverseJoinColumns = @JoinColumn(name = "target_commission_id")
    )
    private Set<Commission> targetCommissions;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_calculation_method", nullable = false)
    private ResultCalculationMethod resultCalculationMethod;

    @Column(name = "priority", nullable = false)
    private Integer priority;
}
