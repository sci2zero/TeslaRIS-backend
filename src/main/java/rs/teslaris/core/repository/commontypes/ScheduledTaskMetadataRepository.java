package rs.teslaris.core.repository.commontypes;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;

@Repository
public interface ScheduledTaskMetadataRepository
    extends JpaRepository<ScheduledTaskMetadata, Integer> {

    @Query("SELECT st FROM ScheduledTaskMetadata st WHERE " +
        "st.taskId = :taskId")
    Optional<ScheduledTaskMetadata> findTaskByTaskId(String taskId);

    @Modifying
    @Query("DELETE FROM ScheduledTaskMetadata st WHERE " +
        "st.taskId = :taskId")
    void deleteTaskForTaskId(String taskId);

    @Query("SELECT st FROM ScheduledTaskMetadata st WHERE " +
        "st.type IN :types")
    List<ScheduledTaskMetadata> findTasksByTypes(List<ScheduledTaskType> types);
}
