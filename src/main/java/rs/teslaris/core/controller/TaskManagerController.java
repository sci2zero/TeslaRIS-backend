package rs.teslaris.core.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.commontypes.ScheduledTaskResponseDTO;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;

@RestController
@RequestMapping("/api/scheduled-task")
@RequiredArgsConstructor
public class TaskManagerController {

    private final TaskManagerService taskManagerService;

    @GetMapping
    public List<ScheduledTaskResponseDTO> listScheduledTasks() {
        return taskManagerService.listScheduledTasks();
    }

    @GetMapping("/report-generation")
    public List<ScheduledTaskResponseDTO> listScheduledReportGenerationTasks() {
        return taskManagerService.listScheduledReportGenerationTasks();
    }

    @DeleteMapping("/{taskId}")
    public void cancelTask(@PathVariable String taskId) {
        taskManagerService.cancelTask(taskId);
    }
}
