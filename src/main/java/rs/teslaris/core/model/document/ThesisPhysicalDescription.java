package rs.teslaris.core.model.document;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ThesisPhysicalDescription {

    @Column(name = "number_of_pages")
    private Integer numberOfPages;

    @Column(name = "number_of_chapters")
    private Integer numberOfChapters;

    @Column(name = "number_of_references")
    private Integer numberOfReferences;

    @Column(name = "number_of_tables")
    private Integer numberOfTables;

    @Column(name = "number_of_illustrations")
    private Integer numberOfIllustrations;

    @Column(name = "number_of_graphs")
    private Integer numberOfGraphs;

    @Column(name = "number_of_appendices")
    private Integer numberOfAppendices;
}
