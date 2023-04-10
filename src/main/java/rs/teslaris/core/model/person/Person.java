package rs.teslaris.core.model.person;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "persons")
public class Person extends BaseEntity {

    @Column(name = "dummy", nullable = false)
    private String dummy;

    PersonName name;
    Set<PersonName> otherNames;
    Set<Involvement> involvements;
    Set<ExpertiseOrSkill> expertisesAndSkills;
    Set<Prize> prizes;
    Set<MultiLingualContent> biography;
    Set<MultiLingualContent> keyword;
    String apvnt;
    String mnid;
    String ORCID;
    String scopusAuthorId;
    int oldId;
    Set<ResearchArea> researchAreas;
    ApproveStatus approveStatus;
}
