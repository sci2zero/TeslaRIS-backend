package rs.teslaris.core.model.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.indexmodel.EventType;

@Getter
@Setter
@AllArgsConstructor
@Entity
@Table(name = "exhibitions")
@SQLRestriction("deleted=false")
public class Exhibition extends Event {

    @Column(name = "number")
    private String number;

    @Column(name = "fee")
    private String fee;

    public Exhibition() {
        this.setEventType(EventType.EXHIBITION);
    }
}
