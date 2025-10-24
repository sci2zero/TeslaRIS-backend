package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.util.deduplication.Mergeable;


@Getter
@Setter
@Entity
@Table(name = "publication_series", indexes = {
    @Index(name = "idx_pub_series_e_issn", columnList = "e_issn"),
    @Index(name = "idx_pub_series_print_issn", columnList = "print_issn")
})
@SQLRestriction("deleted=false")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class PublicationSeries extends BaseEntity implements Mergeable {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> title = new HashSet<>();

    @Column(name = "e_issn")
    private String eISSN;

    @Column(name = "print_issn")
    private String printISSN;

    @Column(name = "open_alex_id")
    private String openAlexId;

    @OneToMany(mappedBy = "publicationSeries", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<PersonPublicationSeriesContribution> contributions = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    private Set<LanguageTag> languages = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> nameAbbreviation = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> subtitle = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "old_ids")
    private Set<Integer> oldIds = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "merged_ids")
    private Set<Integer> mergedIds = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "uris")
    private Set<String> uris = new HashSet<>();


    public void addContribution(PersonPublicationSeriesContribution contribution) {
        contributions.add(contribution);
        contribution.setPublicationSeries(this);
    }

    public void removeContribution(PersonPublicationSeriesContribution contribution) {
        contribution.setPublicationSeries(null);
        contributions.remove(contribution);
    }

    public String getIssnString() {
        if (Objects.nonNull(printISSN) && Objects.nonNull(eISSN)) {
            return MessageFormat.format("({0}, {1})", printISSN, eISSN);
        } else if (Objects.nonNull(printISSN)) {
            return MessageFormat.format("({0})", printISSN);
        } else if (Objects.nonNull(eISSN)) {
            return MessageFormat.format("({0})", eISSN);
        } else {
            return "";
        }
    }
}
