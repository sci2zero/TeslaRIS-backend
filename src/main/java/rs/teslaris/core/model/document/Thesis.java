package rs.teslaris.core.model.document;

import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.institution.OrganisationUnit;

@Getter
@Setter
@Entity
@Table(name = "theses")
public class Thesis extends Document {
    OrganisationUnit organisationUnit;

    @Column(name = "category", nullable = false)
    ThesisCategory category;

    @Column(name = "number_of_pages", nullable = false)
    Integer numberOfPages;
    Set<LanguageTag> languages;
    ResearchArea researchArea;
    Publisher publisher;
}
