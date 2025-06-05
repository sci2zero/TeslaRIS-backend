package rs.teslaris.core.model.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jwt_token_store", indexes = {
    @Index(name = "idx_jti", columnList = "jti")
})
public class JwtToken extends BaseEntity {

    @Column(name = "jti")
    private String jti;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "expired_at")
    private Instant expiresAt;

    @Column(name = "revoked")
    private boolean revoked;
}
