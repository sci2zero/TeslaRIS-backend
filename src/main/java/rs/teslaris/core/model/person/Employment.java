package rs.teslaris.core.model.person;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employments")
public class Employment extends Involvement {

    @Column(name = "employment_position", nullable = false)
    EmploymentPosition employmentPosition;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> role;
}
