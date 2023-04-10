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
@Table(name = "multi_lingual_content")
public class MultiLingualContent extends BaseEntity {
    LanguageTag language;
    String content;
    int priority;
}
