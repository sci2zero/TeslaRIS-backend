package rs.teslaris.core.model.user;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tutorial_walkthroughs", indexes = {
    @Index(name = "idx_tutorial_walkthrough_user", columnList = "user_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_tutorial_walkthrough_user_key",
        columnNames = {"user_id", "tutorial_key"})
})
@SQLRestriction("deleted=false")
public class TutorialWalkthrough extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tutorial_key", nullable = false)
    private String tutorialKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TutorialStatus status;

    @Column(name = "step")
    private Integer step;
}
