package rs.teslaris.core.model.institution;

import java.time.LocalDate;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organisation_units_relations")
public class OrganisationUnitsRelation extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> sourceAffiliationStatement;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> targetAffiliationStatement;

    @Column(name = "relation_type", nullable = false)
    OrganisationUnitRelationType relationType;

    @Column(name = "from", nullable = false)
    LocalDate from;

    @Column(name = "to", nullable = false)
    LocalDate to;

    @Column(name = "approve_status", nullable = false)
    ApproveStatus approveStatus;

    @OneToMany(mappedBy = "relation_id", fetch = FetchType.LAZY)
    Set<DocumentFile> proofs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_organisation_unit_id")
    OrganisationUnit sourceOrganisationUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_organisation_unit_id")
    OrganisationUnit targetOrganisationUnit;
}
