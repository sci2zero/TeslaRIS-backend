package rs.teslaris.core.model.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.indexmodel.DocumentPublicationType;

@Getter
@Setter
@AllArgsConstructor
@Entity
@Table(name = "datasets")
@SQLRestriction("deleted=false")
public non-sealed class Dataset extends Document implements PublisherPublishable {

    @Column(name = "internal_number")
    private String internalNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;


    public Dataset() {
        super(DocumentPublicationType.DATASET);
    }
}
