package rs.teslaris.core.model.person;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.user.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "persons")
@SQLRestriction("deleted=false")
public class Person extends BaseEntity {

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private PersonName name;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<PersonName> otherNames = new HashSet<>();

    @Embedded
    private PersonalInfo personalInfo;

    @OneToOne(mappedBy = "person")
    private User user;

    @OneToMany(mappedBy = "personInvolved", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Involvement> involvements = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ExpertiseOrSkill> expertisesAndSkills = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Prize> prizes = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MultiLingualContent> biography = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MultiLingualContent> keyword = new HashSet<>();

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

    @Column(name = "cris_uns_id")
    private Integer oldId;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<ResearchArea> researchAreas;

    @Column(name = "approve_status", nullable = false)
    private ApproveStatus approveStatus;

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
    }
}
