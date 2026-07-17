package rs.teslaris.core.model.document;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.commontypes.FlexibleDate;

@Getter
@Setter
@Entity
@Table(name = "intellectual_property")
@SQLRestriction("deleted=false")
public non-sealed class IntellectualProperty extends Document implements PublisherPublishable {

    @Column(name = "type")
    private IntellectualPropertyType type;

    @Column(name = "application_status")
    private IntellectualPropertyApplicationStatus applicationStatus;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "year", column = @Column(name = "date_requested_year")),
        @AttributeOverride(name = "month", column = @Column(name = "date_requested_month")),
        @AttributeOverride(name = "day", column = @Column(name = "date_requested_day")),
        @AttributeOverride(name = "text", column = @Column(name = "date_requested_text"))
    })
    private FlexibleDate dateRequested;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "year", column = @Column(name = "date_filing_priority_year")),
        @AttributeOverride(name = "month", column = @Column(name = "date_filing_priority_month")),
        @AttributeOverride(name = "day", column = @Column(name = "date_filing_priority_day")),
        @AttributeOverride(name = "text", column = @Column(name = "date_filing_priority_text"))
    })
    private FlexibleDate dateFilingPriority;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "year", column = @Column(name = "date_to_year")),
        @AttributeOverride(name = "month", column = @Column(name = "date_to_month")),
        @AttributeOverride(name = "day", column = @Column(name = "date_to_day")),
        @AttributeOverride(name = "text", column = @Column(name = "date_to_text"))
    })
    private FlexibleDate dateTo;

    @Column(name = "number")
    private String number;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;


    public IntellectualProperty() {
        super(DocumentPublicationType.INTELLECTUAL_PROPERTY);
    }
}
