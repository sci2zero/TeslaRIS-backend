package rs.teslaris.core.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.ScheduledTaskResponseDTO;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/scheduled-task")
@RequiredArgsConstructor
@Traceable
public class TaskManagerController {

    private final TaskManagerService taskManagerService;

    private final JwtUtil tokenUtil;

    @GetMapping
    @PreAuthorize("hasAuthority('SCHEDULE_TASK')")
    public List<ScheduledTaskResponseDTO> listScheduledTasks() {
        return taskManagerService.listScheduledTasks();
    }

    @GetMapping("/report-generation")
    @PreAuthorize("hasAuthority('SCHEDULE_REPORT_GENERATION')")
    public List<ScheduledTaskResponseDTO> listScheduledReportGenerationTasks(
        @RequestHeader(value = "Authorization") String bearerToken) {
        return taskManagerService.listScheduledReportGenerationTasks(
            tokenUtil.extractUserIdFromToken(bearerToken),
            tokenUtil.extractUserRoleFromToken(bearerToken));
    }

    @GetMapping("/harvest")
    @PreAuthorize("hasAuthority('SCHEDULE_DOCUMENT_HARVEST')")
    public List<ScheduledTaskResponseDTO> listScheduledHarvestTasks(
        @RequestHeader(value = "Authorization") String bearerToken) {
        return taskManagerService.listScheduledHarvestTasks(
            tokenUtil.extractUserIdFromToken(bearerToken),
            tokenUtil.extractUserRoleFromToken(bearerToken));
    }

    @GetMapping("/document-backup")
    @PreAuthorize("hasAuthority('GENERATE_OUTPUT_BACKUP')")
    public List<ScheduledTaskResponseDTO> listScheduledDocumentBackupTasks(
        @RequestHeader(value = "Authorization") String bearerToken) {
        return taskManagerService.listScheduledDocumentBackupGenerationTasks(
            tokenUtil.extractUserIdFromToken(bearerToken),
            tokenUtil.extractUserRoleFromToken(bearerToken));
    }

    @GetMapping("/thesis-library-backup")
    @PreAuthorize("hasAuthority('GENERATE_THESIS_LIBRARY_BACKUP')")
    public List<ScheduledTaskResponseDTO> listScheduledThesisLibraryBackupTasks(
        @RequestHeader(value = "Authorization") String bearerToken) {
        return taskManagerService.listScheduledThesisLibraryBackupGenerationTasks(
            tokenUtil.extractUserIdFromToken(bearerToken),
            tokenUtil.extractUserRoleFromToken(bearerToken));
    }

    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasAnyAuthority('SCHEDULE_TASK', 'SCHEDULE_REPORT_GENERATION', 'SCHEDULE_DOCUMENT_HARVEST')")
    public void cancelTask(@PathVariable String taskId) {
        taskManagerService.cancelTask(taskId);
    }
}
