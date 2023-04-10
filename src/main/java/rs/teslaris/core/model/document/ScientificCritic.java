package rs.teslaris.core.model.document;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "scientific_critics")
public class ScientificCritic extends Document {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    Journal journal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monograph_id")
    Monograph monograph;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proceedings_id")
    Proceedings proceedings;
}
