package rs.teslaris.revisioner.model.fixator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import rs.teslaris.core.model.commontypes.BaseEntity;

@Entity
@Table(name = "data_quality_fixator_pipeline")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataQualityFixatorPipeline extends BaseEntity {

    @Column(name = "pipeline_name", nullable = false, unique = true)
    private String pipelineName;

    /**
     * Incremented on every edit made through the UI, and only ever overwritten by a configuration
     * file carrying a strictly greater version.
     */
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_remark_keys", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> matchedRemarkKeys = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applied_fixators", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> appliedFixators = new ArrayList<>();

    @Column(name = "create_version_on_every_step", nullable = false)
    @Builder.Default
    private Boolean createVersionOnEveryStep = false;
}
