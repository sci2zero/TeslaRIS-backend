package rs.teslaris.core.model.document;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;


@Getter
@Setter
@Entity
@Table(name = "publication_series")
@Where(clause = "deleted=false")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class PublicationSeries extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> title;

    @Column(name = "e_issn", unique = true)
    private String eISSN;

    @Column(name = "print_issn", unique = true)
    private String printISSN;

    @OneToMany(mappedBy = "publicationSeries", fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<PersonPublicationSeriesContribution> contributions;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<LanguageTag> languages;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> nameAbbreviation;

    public void addContribution(PersonPublicationSeriesContribution contribution) {
        contributions.add(contribution);
        contribution.setPublicationSeries(this);
    }

    public void removeContribution(PersonPublicationSeriesContribution contribution) {
        contribution.setPublicationSeries(null);
        contributions.remove(contribution);
    }
}
