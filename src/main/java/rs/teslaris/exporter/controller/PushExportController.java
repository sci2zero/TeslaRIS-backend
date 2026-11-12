package rs.teslaris.exporter.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.UnsupportedEntityTypeException;
import rs.teslaris.exporter.model.push.PushEntityType;
import rs.teslaris.exporter.service.interfaces.PushExportService;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader;
import rs.teslaris.exporter.util.PushTargetsConfigurationLoader.PushTarget;

@RestController
@RequestMapping("/api/push-export")
@RequiredArgsConstructor
@Traceable
public class PushExportController {

    private final PushExportService pushExportService;


    @PostMapping("/{targetIdentifier}")
    @PreAuthorize("hasAuthority('PUSH_EXPORT_DATA')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void pushToTarget(@PathVariable String targetIdentifier, @RequestParam String set,
                             @RequestParam(defaultValue = "false") boolean force,
                             HttpServletRequest request) {
        var target = PushTargetsConfigurationLoader.getTargetByIdentifier(targetIdentifier)
            .filter(PushTarget::enabled)
            .orElseThrow(() -> new NotFoundException(
                "Push target '" + targetIdentifier + "' does not exist or is disabled."));

        var entityType = PushEntityType.fromCode(set)
            .filter(type -> target.entityTypes().contains(type.getCode()))
            .orElseThrow(() -> new UnsupportedEntityTypeException(
                "Set '" + set + "' is not supported by push target '" + targetIdentifier + "'.",
                request.getRequestURL().toString()));

        pushExportService.pushEntities(target, entityType, force);
    }
}
