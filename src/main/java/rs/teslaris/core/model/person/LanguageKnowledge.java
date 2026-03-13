package rs.teslaris.core.model.person;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.Language;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "language_knowledges")
@SQLRestriction("deleted=false")
public class LanguageKnowledge extends ExpertiseOrSkill {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Column(name = "mother-tongue")
    private Boolean motherTongue;

    @Column(name = "read")
    private LanguageLevel read;

    @Column(name = "write")
    private LanguageLevel write;

    @Column(name = "speak")
    private LanguageLevel speak;

    @Column(name = "understand-spoken")
    private LanguageLevel understandSpoken;

    @Column(name = "peer-review")
    private LanguageLevel peerReview;
}
