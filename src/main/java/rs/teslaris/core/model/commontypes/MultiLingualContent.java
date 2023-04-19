package rs.teslaris.core.model.commontypes;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "multi_lingual_content")
public class MultiLingualContent extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "language_tag_id", nullable = false)
    private LanguageTag language;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "priority", nullable = false)
    private int priority;
}
