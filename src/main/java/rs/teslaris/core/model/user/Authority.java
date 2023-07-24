package rs.teslaris.core.model.user;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
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
@Table(name = "authorities")
@Where(clause = "deleted=false")
public class Authority extends BaseEntity implements GrantedAuthority {

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(name = "authority_privileges", joinColumns = @JoinColumn(name = "authority_id"), inverseJoinColumns = @JoinColumn(name = "privilege_id"))
    private Set<Privilege> privileges = new HashSet<>();

    @Override
    public String getAuthority() {
        return this.name;
    }

    public Authority addPrivilege(Privilege privilege) {
        this.privileges.add(privilege);
        return this;
    }
}
