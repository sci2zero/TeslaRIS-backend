package rs.teslaris.core.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organisational_units")
public class OrganisationalUnit extends BaseEntity {

    @Column(name = "dummy", nullable = false)
    private String dummy;
}
