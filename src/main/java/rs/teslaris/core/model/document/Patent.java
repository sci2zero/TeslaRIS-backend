package rs.teslaris.core.model.document;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "patents")
public class Patent extends Document {

    @Column(name = "number", nullable = false, unique = true)
    String number;
    Publisher publisher;
}
