package rs.teslaris.core.model.commontypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "api_keys")
public class ApiKey extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> name = new HashSet<>();

    @Column(name = "usage_type")
    private ApiKeyType usageType;

    @Column(name = "value", nullable = false, unique = true)
    private String value;

    @Column(name = "lookup_hash")
    private String lookupHash;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "client_email")
    private String clientEmail;

    @Column(name = "preferred_language")
    private String preferredLanguage;

    @Column(name = "daily_requests")
    private Integer dailyRequests;

    @Column(name = "times_used_today")
    private Integer timesUsedToday = 0;
}
