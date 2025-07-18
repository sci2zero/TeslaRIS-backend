package rs.teslaris.assessment.model.indicator;

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
import rs.teslaris.core.model.institution.OrganisationUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "external_indicator_configuration")
@SQLRestriction("deleted=false")
public class ExternalIndicatorConfiguration extends BaseEntity {

    @Column(name = "show_altmetric")
    private Boolean showAltmetric = true;

    @Column(name = "show_dimensions")
    private Boolean showDimensions = true;

    @Column(name = "show_open_citations")
    private Boolean showOpenCitations = true;

    @Column(name = "show_plum_x")
    private Boolean showPlumX = true;

    @Column(name = "show_unpaywall")
    private Boolean showUnpaywall = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private OrganisationUnit institution;
}
