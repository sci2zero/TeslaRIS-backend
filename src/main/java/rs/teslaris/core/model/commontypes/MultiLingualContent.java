package rs.teslaris.core.model.commontypes;


import com.google.common.base.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "multi_lingual_content")
@SQLRestriction("deleted=false")
public class MultiLingualContent extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "language_tag_id", nullable = false)
    private LanguageTag language;

    @Length(max = 15000)
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
        return Objects.equal(content, that.content) &&
            Objects.equal(language.getLanguageTag(), that.language.getLanguageTag());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), content);
    }
}
