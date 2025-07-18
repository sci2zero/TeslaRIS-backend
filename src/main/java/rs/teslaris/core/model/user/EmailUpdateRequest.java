package rs.teslaris.core.model.user;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "user_email_update_requests", indexes = {
    @Index(name = "idx_email_update_token", columnList = "email_update_token")
})
@SQLRestriction("deleted=false")
public class EmailUpdateRequest extends BaseEntity {

    @Column(name = "email_update_token")
    private String emailUpdateToken;

    @Column(name = "new_email_address")
    private String newEmailAddress;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
    @JoinColumn(name = "user_id")
    private User user;
}
