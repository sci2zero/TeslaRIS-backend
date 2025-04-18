package rs.teslaris.assessment.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.document.PublicationSeries;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DiscriminatorValue("PUBLICATION_SERIES_INDICATOR")
public class PublicationSeriesIndicator extends EntityIndicator {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_series_id")
    private PublicationSeries publicationSeries;

    @Column(name = "category_identifier")
    private String categoryIdentifier;

    @Column(name = "edition")
    private String edition;
}
