package rs.teslaris.core.model.institution;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.GeoLocation;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ProfilePhotoOrLogo;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.person.Contact;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organisation_units", indexes = {
    @Index(name = "idx_org_unit_scopus_afid", columnList = "scopus_afid"),
    @Index(name = "idx_org_unit_old_id", columnList = "cris_uns_id")
})
@SQLRestriction("deleted=false")
public class OrganisationUnit extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> name = new HashSet<>();

    @Column(name = "name_abbreviation", nullable = false)
    private String nameAbbreviation;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> keyword = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<ResearchArea> researchAreas = new HashSet<>();

    @Column(name = "scopus_afid")
    private String scopusAfid;

    @Embedded
    private GeoLocation location;

    @Column(name = "approve_status", nullable = false)
    private ApproveStatus approveStatus;

    @Column(name = "cris_uns_id")
    private Integer oldId;

    @Embedded
    private Contact contact;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> uris = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    private Set<String> accountingIds = new HashSet<>();

    @Embedded
    private ProfilePhotoOrLogo logo;
}
