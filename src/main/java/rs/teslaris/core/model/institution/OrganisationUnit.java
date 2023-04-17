package rs.teslaris.core.model.institution;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.GeoLocation;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.person.Contact;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organisation_units")
public class OrganisationUnit extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> name;

    @Column(name = "name_abbreviation", nullable = false)
    String nameAbbreviation;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    Set<MultiLingualContent> keyword;

    @ManyToMany(fetch = FetchType.LAZY)
    Set<ResearchArea> researchAreas;

    @Embedded
    GeoLocation location;

    @Column(name = "approve_status", nullable = false)
    ApproveStatus approveStatus;

    @Embedded
    Contact contact;
}
