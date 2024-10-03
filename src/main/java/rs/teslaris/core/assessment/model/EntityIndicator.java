package rs.teslaris.core.assessment.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Entity
@Table(name = "entity_indicators")
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
    private String source;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<DocumentFile> proofs = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "assessment_document_urls", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "url", nullable = false)
    private Set<String> urls = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indicator_id")
    private Indicator indicator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
