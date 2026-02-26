package rs.teslaris.core.model.project;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Event;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_events")
@SQLRestriction("deleted=false")
public class ProjectEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @OneToMany(mappedBy = "projectEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<FundingPart> fundingParts;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> textualDescription = new HashSet<>();

    @Column(name = "relation_type")
    private ProjectEventType relationType;
}
