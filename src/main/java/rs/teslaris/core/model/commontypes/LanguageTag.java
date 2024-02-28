package rs.teslaris.core.model.commontypes;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "language_tag")
@Where(clause = "deleted=false")
public class LanguageTag extends BaseEntity {

    @Column(name = "language_tag", nullable = false)
    private String languageTag;

    @Column(name = "display", nullable = false)
    private String display;
}
