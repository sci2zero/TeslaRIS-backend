package rs.teslaris.core.model.person;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "persons")
public class Person extends BaseEntity {

    @OneToOne(fetch = FetchType.EAGER)
    private PersonName name;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<PersonName> otherNames;

    @Embedded
    private PersonalInfo personalInfo;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<Involvement> involvements;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<ExpertiseOrSkill> expertisesAndSkills;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<Prize> prizes;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> biography;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> keyword;

    @Column(name = "apvnt", unique = true)
    private String apvnt;

    @Column(name = "mnid", unique = true)
    private String mnid;

    @Column(name = "orcid", unique = true)
    private String orcid;

    @Column(name = "acopus_author_id", unique = true)
    private String scopusAuthorId;

    @Column(name = "cris_uns_id", unique = true)
    private int oldId;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<ResearchArea> researchAreas;

    @Column(name = "approve_status", nullable = false)
    private ApproveStatus approveStatus;
}
