package rs.teslaris.core.model.person;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ProfilePhotoOrLogo;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.util.deduplication.Mergeable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "persons", indexes = {
    @Index(name = "idx_person_apvnt", columnList = "apvnt"),
    @Index(name = "idx_person_e_cris_id", columnList = "e_cris_id"),
    @Index(name = "idx_person_e_nauka_id", columnList = "e_nauka_id"),
    @Index(name = "idx_person_orcid", columnList = "orcid"),
    @Index(name = "idx_person_scopus_author_id", columnList = "scopus_author_id"),
    @Index(name = "idx_wos_researcher_id", columnList = "web_of_science_researcher_id"),
    @Index(name = "idx_open_alex_id", columnList = "open_alex_id"),
    @Index(name = "idx_person_approve_status", columnList = "approve_status"),
    @Index(name = "idx_person_name_id", columnList = "name_id")
})
@SQLRestriction("deleted=false")
public class Person extends BaseEntity implements Mergeable {

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private PersonName name;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<PersonName> otherNames = new HashSet<>();

    @Embedded
    private PersonalInfo personalInfo;

    @OneToOne(mappedBy = "person")
    private User user;

    @OneToMany(mappedBy = "personInvolved", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Involvement> involvements = new HashSet<>();

    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExpertiseOrSkill> expertisesAndSkills = new HashSet<>();

    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Prize> prizes = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MultiLingualContent> biography = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MultiLingualContent> keyword = new HashSet<>();

    @Embedded
    private ProfilePhotoOrLogo profilePhoto;

    @Column(name = "apvnt")
    private String apvnt;

    @Column(name = "e_cris_id")
    private String eCrisId;

    @Column(name = "e_nauka_id")
    private String eNaukaId;

    @Column(name = "orcid")
    private String orcid;

    @Column(name = "scopus_author_id")
    private String scopusAuthorId;

    @Column(name = "open_alex_id")
    private String openAlexId;

    @Column(name = "web_of_science_researcher_id")
    private String webOfScienceResearcherId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "old_ids")
    private Set<Integer> oldIds = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "merged_ids")
    private Set<Integer> mergedIds = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY)
    private Set<ResearchArea> researchAreas;

    @Column(name = "approve_status", nullable = false)
    private ApproveStatus approveStatus;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "person_employment_hierarchy",
        joinColumns = @JoinColumn(name = "person_id"))
    @Column(name = "institution_id")
    private Set<Integer> employmentInstitutionsIdHierarchy = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    private Set<String> accountingIds = new HashSet<>();

    @Column(name = "date_of_last_indicator_harvest")
    private LocalDate dateOfLastIndicatorHarvest;

    public void addInvolvement(Involvement involvement) {
        if (involvements == null) {
            involvements = new HashSet<>();
        }
        involvements.add(involvement);
        involvement.setPersonInvolved(this);
    }

    public void removeInvolvement(Involvement involvement) {
        involvements.remove(involvement);
        involvement.setPersonInvolved(null);
    }

    public void addPrize(Prize prize) {
        prizes.add(prize);
        prize.setPerson(this);
    }
}
