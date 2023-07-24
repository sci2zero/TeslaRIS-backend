package rs.teslaris.core.model.document;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@Table(name = "softwares")
@Where(clause = "deleted=false")
public class Software extends Document {

    @Column(name = "internal_number")
    private String internalNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;
}
