package rs.teslaris.core.model.user;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;
import org.springframework.security.core.GrantedAuthority;
import rs.teslaris.core.model.commontypes.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "privileges")
@Where(clause = "deleted=false")
public class Privilege extends BaseEntity implements GrantedAuthority {

    @Column(name = "name")
    private String name;

    @Override
    public String getAuthority() {
        return this.name;
    }
}
