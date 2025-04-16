package rs.teslaris.thesislibrary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.institution.OrganisationUnit;

@Getter
@Setter
@Entity
@Table(name = "registry_book_entries")
@SQLRestriction("deleted=false")
public class RegistryBookEntry extends BaseEntity {

    @Embedded
    private DissertationInformation dissertationInformation;

    @Embedded
    private RegistryBookPersonalInformation personalInformation;

    @Embedded
    private RegistryBookContactInformation contactInformation;

    @Embedded
    private PreviousTitleInformation previousTitleInformation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thesis_id")
    private Thesis thesis;

    @Column(name = "attendance_identifier", unique = true)
    private String attendanceIdentifier;

    @Column(name = "promotion_school_year")
    private String promotionSchoolYear;

    @Column(name = "registry_book_number")
    private Integer registryBookNumber;

    @Column(name = "promotion_ordinal_number")
    private Integer promotionOrdinalNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registry_book_institution_id")
    private OrganisationUnit registryBookInstitution;

    @Column(name = "allow_single_edit")
    private Boolean allowSingleEdit = false;
}
