package rs.teslaris.assessment.model.indicator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.user.User;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(
    callSuper = false,
    exclude = {
        "timestamp", "proofs", "user", "numericValue", "booleanValue", "textualValue"
    }
)
@Entity
@Table(
    name = "entity_indicators",
    indexes = {
        @Index(name = "idx_entity_type_pubseries_source_fromdate", columnList = "entity_type, publication_series_id, source, from_date"),
        @Index(name = "idx_entity_type_pubseries_source_from_to", columnList = "entity_type, publication_series_id, source, from_date, to_date"),
        @Index(name = "idx_entity_type_category_source_fromdate", columnList = "entity_type, category_identifier, source, from_date"),
        @Index(name = "idx_entity_type_pubseries_source_date_category", columnList = "entity_type, publication_series_id, source, from_date, category_identifier"),
        @Index(name = "idx_ei_type_document", columnList = "entity_type, document_id")
    }
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "entity_type", discriminatorType = DiscriminatorType.STRING)
public abstract class EntityIndicator extends BaseEntity {

    @Column(name = "numeric_value")
    private Double numericValue;

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    @Column(name = "textual_value")
    private String textualValue;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "source")
    private EntityIndicatorSource source;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<DocumentFile> proofs = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "indicator_id")
    private Indicator indicator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
