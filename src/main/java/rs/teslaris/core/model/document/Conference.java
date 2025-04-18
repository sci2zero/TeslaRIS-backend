package rs.teslaris.core.model.document;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "conferences", indexes = {
    @Index(name = "idx_conference_conf_id", columnList = "conf_id")
})
@SQLRestriction("deleted=false")
public class Conference extends Event {

    @Column(name = "number")
    private String number;

    @Column(name = "fee")
    private String fee;

    @Column(name = "conf_id")
    private String confId;
}
