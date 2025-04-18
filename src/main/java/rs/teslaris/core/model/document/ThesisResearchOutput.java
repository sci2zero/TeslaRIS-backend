package rs.teslaris.core.model.document;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "thesis_research_outputs", indexes = {
    @Index(name = "idx_thesis_research_output", columnList = "thesis_id,research_output_id")
})
@NoArgsConstructor
@AllArgsConstructor
public class ThesisResearchOutput extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thesis_id")
    private Thesis thesis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_output_id")
    private Document researchOutput;
}
