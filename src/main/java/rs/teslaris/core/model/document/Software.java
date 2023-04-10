package rs.teslaris.core.model.document;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "softwares")
public class Software extends Document {

    @Column(name = "internal_number", nullable = false)
    String internalNumber;
    Publisher publisher;
}
