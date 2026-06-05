package rs.teslaris.core.model.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.indexmodel.DocumentPublicationType;

@Getter
@Setter
@Entity
@Table(name = "genetic_materials")
@SQLRestriction("deleted=false")
public non-sealed class GeneticMaterial extends Document implements PublisherPublishable {

    @Column(name = "internal_number")
    private String internalNumber;

    @Column(name = "type")
    private GeneticMaterialType geneticMaterialType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;


    public GeneticMaterial() {
        super(DocumentPublicationType.GENETIC_MATERIAL);
    }
}
