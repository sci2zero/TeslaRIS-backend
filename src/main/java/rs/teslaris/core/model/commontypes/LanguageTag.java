package rs.teslaris.core.model.commontypes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "language_tag")
public class LanguageTag extends BaseEntity {
    String languageTag;
    String display;
}
