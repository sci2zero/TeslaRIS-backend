package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@Entity
@Table(name = "performance_related_outputs")
@SQLRestriction("deleted=false")
public class PerformanceRelatedOutput extends Document {

    @Column(name = "performance_related_output_type")
    private PerformanceRelatedOutputType type;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<LanguageTag> languages = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> producer = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> distributor = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> sourceTitle = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> otherActors = new HashSet<>();

    public PerformanceRelatedOutput() {
        super(DocumentPublicationType.PERFORMANCE_RELATED_OUTPUT);
    }
}
