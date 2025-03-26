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
@DiscriminatorValue("PUB_SERIES_ASSESSMENT_CLASS")
public class PublicationSeriesAssessmentClassification extends EntityAssessmentClassification {


    @Column(name = "category_identifier")
    private String categoryIdentifier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_series_id")
    private PublicationSeries publicationSeries;
}
