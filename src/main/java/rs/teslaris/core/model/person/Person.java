package rs.teslaris.core.model.person;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
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
    PersonName name;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    Set<PersonName> otherNames;

    @OneToMany(fetch = FetchType.LAZY)
    Set<Involvement> involvements;

    @OneToMany(fetch = FetchType.LAZY)
    Set<ExpertiseOrSkill> expertisesAndSkills;

    @OneToMany(fetch = FetchType.LAZY)
    Set<Prize> prizes;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> biography;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> keyword;

    @Column(name = "apvnt", unique = true)
    String apvnt;

    @Column(name = "mnid", unique = true)
    String mnid;

    @Column(name = "orcid", unique = true)
    String orcid;

    @Column(name = "acopus_author_id", unique = true)
    String scopusAuthorId;

    @Column(name = "cris_uns_id")
    int oldId;

    @OneToMany(fetch = FetchType.LAZY)
    Set<ResearchArea> researchAreas;

    @Column(name = "approve_status", nullable = false)
    ApproveStatus approveStatus;
}
