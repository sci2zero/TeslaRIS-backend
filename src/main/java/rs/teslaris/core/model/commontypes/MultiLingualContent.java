package rs.teslaris.core.model.commontypes;


import com.google.common.base.Objects;
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
import org.hibernate.annotations.Where;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "multi_lingual_content")
@Where(clause = "deleted=false")
public class MultiLingualContent extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "language_tag_id", nullable = false)
    private LanguageTag language;

    @Length(max = 5120)
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        MultiLingualContent that = (MultiLingualContent) o;
        return Objects.equal(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), content);
    }
}
