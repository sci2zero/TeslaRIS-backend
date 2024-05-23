package rs.teslaris.core.model.commontypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "language_tag")
@SQLRestriction("deleted=false")
public class LanguageTag extends BaseEntity {

    @Column(name = "language_tag", nullable = false)
    private String languageTag;

    @Column(name = "display", nullable = false)
    private String display;
}
