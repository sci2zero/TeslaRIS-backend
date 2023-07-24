package rs.teslaris.core.model.commontypes;

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
import org.hibernate.annotations.Where;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "countries")
@Where(clause = "deleted=false")
public class Country extends BaseEntity {
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> name;
}
