package rs.teslaris.core.model.commontypes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Set;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "research_area")
public class ResearchArea extends BaseEntity {
    Set<MultiLingualContent> name;
    Set<MultiLingualContent> description;
    ResearchArea superResearchArea;
}
