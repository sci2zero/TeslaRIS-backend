package rs.teslaris.core.model.document;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
@Table(name = "conferences")
@Where(clause = "deleted=false")
public class Conference extends Event {

    @Column(name = "number")
    private String number;

    @Column(name = "fee")
    private String fee;
}
