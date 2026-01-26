package rs.teslaris.core.model.commontypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicUpdate
@EqualsAndHashCode(callSuper = false)
@Table(name = "scheduled_tasks")
@SQLRestriction("deleted=false")
public class ScheduledTaskMetadata extends BaseEntity {

    @Column(name = "task_id")
    private String taskId;

    @Column(name = "time_to_run")
    private LocalDateTime timeToRun;

    @Column(name = "task_type")
    private ScheduledTaskType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "metadata")
    private Map<String, Object> metadata;

    @Column(name = "recurrence_type")
    private RecurrenceType recurrenceType;
}
