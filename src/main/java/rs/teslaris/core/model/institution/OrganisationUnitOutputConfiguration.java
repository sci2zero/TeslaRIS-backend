package rs.teslaris.core.model.institution;

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
import rs.teslaris.core.model.commontypes.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organisation_unit_outputs")
public class OrganisationUnitOutputConfiguration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_unit_id")
    private OrganisationUnit organisationUnit;

    @Column(name = "show_outputs")
    private Boolean showOutputs = true;

    @Column(name = "show_by_specified_affiliation")
    private Boolean showBySpecifiedAffiliation = true;

    @Column(name = "show_by_publication_year_employments")
    private Boolean showByPublicationYearEmployments = true;

    @Column(name = "show_by_current_employments")
    private Boolean showByCurrentEmployments = true;
}
