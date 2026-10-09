package rs.teslaris.core.model.commontypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
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
@Entity
@DynamicUpdate
@EqualsAndHashCode(callSuper = false)
@Table(name = "scheduled_tasks")
@SQLRestriction("deleted=false")
public class ScheduledTaskMetadata extends BaseEntity {

    @Column(name = "task_id")
    private String taskId;

    @Column(name = "time_to_run")
    private LocalDateTime legacyTimeToRun;

    @Column(name = "execution_time")
    private Instant timeToRun;

    public ScheduledTaskMetadata(String taskId, Instant timeToRun, ScheduledTaskType type,
                                 Map<String, Object> metadata, RecurrenceType recurrenceType) {
        this.taskId = taskId;
        setTimeToRun(timeToRun);
        this.type = type;
        this.metadata = metadata;
        this.recurrenceType = recurrenceType;
    }

    // Used by old database rows until the task is restored and saved as an instant.
    public Instant getTimeToRun() {
        return timeToRun != null ? timeToRun :
            (legacyTimeToRun == null ? null :
                legacyTimeToRun.atZone(ZoneId.systemDefault()).toInstant());
    }

    public void setTimeToRun(Instant timeToRun) {
        this.timeToRun = timeToRun;
        this.legacyTimeToRun = timeToRun == null ? null :
            timeToRun.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    @Column(name = "task_type")
    private ScheduledTaskType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "metadata")
    private Map<String, Object> metadata;

    @Column(name = "recurrence_type")
    private RecurrenceType recurrenceType;
}
