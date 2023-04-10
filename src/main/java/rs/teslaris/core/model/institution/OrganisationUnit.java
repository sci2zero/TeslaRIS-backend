package rs.teslaris.core.model.institution;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organisational_units")
public class OrganisationUnit extends BaseEntity {

//    @Column(name = "dummy", nullable = false)
//    private String dummy;

    Set<MultiLingualContent> name;
    String acronym;
    Set<MultiLingualContent> keyword;
    Set<ResearchArea> researchAreas;
    GeoLocation location;
    ApproveStatus approveStatus;
}
