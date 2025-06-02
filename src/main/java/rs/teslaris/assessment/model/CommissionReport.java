package rs.teslaris.assessment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.institution.Commission;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "commission_report")
@SQLRestriction("deleted=false")
public class CommissionReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_commission_id", nullable = false)
    private Commission commission;

    @Column(name = "report_file_name")
    private String reportFileName;
}
