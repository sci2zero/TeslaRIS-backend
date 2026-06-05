package rs.teslaris.core.model.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "journals")
@SQLRestriction("deleted=false")
public class Journal extends PublicationSeries {

    @Column(name = "type", nullable = false)
    private ArticleCollectionSeriesType type;


    public Journal() {
        this.type = ArticleCollectionSeriesType.JOURNAL;
    }
}
