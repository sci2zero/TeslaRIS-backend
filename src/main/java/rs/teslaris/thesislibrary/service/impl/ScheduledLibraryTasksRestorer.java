package rs.teslaris.thesislibrary.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.dto.commontypes.RelativeDateDTO;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.model.document.FileSection;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.repository.commontypes.ScheduledTaskMetadataRepository;
import rs.teslaris.core.service.impl.commontypes.ScheduledTasksRestorer;
import rs.teslaris.thesislibrary.service.interfaces.RegistryBookReportService;
import rs.teslaris.thesislibrary.service.interfaces.ThesisLibraryBackupService;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScheduledLibraryTasksRestorer {

    private final ThesisLibraryBackupService thesisLibraryBackupService;

    private final RegistryBookReportService registryBookReportService;

    private final ScheduledTaskMetadataRepository metadataRepository;

    private final ObjectMapper objectMapper;

    private final List<Class<? extends Enum<?>>> FILE_SECTION_ENUMS = List.of(
        tryLoadEnum("rs.teslaris.thesislibrary.model.ThesisFileSection"),
        tryLoadEnum("rs.teslaris.core.model.document.DocumentFileSection")
    );


    @EventListener(ApplicationReadyEvent.class)
    protected void restoreTasksOnStartup() {
        List<ScheduledTaskMetadata> allMetadata = metadataRepository.findTasksByTypes(
            List.of(
                ScheduledTaskType.THESIS_LIBRARY_BACKUP,
                ScheduledTaskType.REGISTRY_BOOK_REPORT_GENERATION
            ));

        for (ScheduledTaskMetadata metadata : allMetadata) {
            try {
                synchronized (ScheduledTasksRestorer.lock) {
                    restoreTaskFromMetadata(metadata);
                }
            } catch (Exception e) {
                log.error("Failed to restore thesis library scheduled task: {}",
                    metadata.getTaskId(), e);
            }
        }
    }

    private void restoreTaskFromMetadata(ScheduledTaskMetadata metadata) {
        if (metadata.getType().equals(ScheduledTaskType.THESIS_LIBRARY_BACKUP)) {
            restoreThesisLibraryBackupCreation(metadata);
        } else if (metadata.getType().equals(ScheduledTaskType.REGISTRY_BOOK_REPORT_GENERATION)) {
            restoreRegistryBookReportGeneration(metadata);
        }

        metadataRepository.deleteTaskForTaskId(metadata.getTaskId());
    }

    private void restoreThesisLibraryBackupCreation(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var institutionId = (Integer) data.get("institutionId");
        var from = RelativeDateDTO.parse((String) data.get("from"));
        var to = RelativeDateDTO.parse((String) data.get("to"));

        var fileSectionNames = objectMapper.convertValue(
            data.get("thesisFileSections"), new TypeReference<ArrayList<String>>() {
            }
        );
        var fileSections = resolveFileSections(fileSectionNames);

        var types = objectMapper.convertValue(
            data.get("types"), new TypeReference<ArrayList<ThesisType>>() {
            }
        );

        var defended = (Boolean) data.get("defended");
        var putOnReview = (Boolean) data.get("putOnReview");
        var language = (String) data.get("language");
        var userId = (Integer) data.get("userId");
        var metadataFormat = ExportFileType.valueOf((String) data.get("metadataFormat"));

        thesisLibraryBackupService.scheduleBackupGeneration(institutionId, from, to, types,
            fileSections, defended, putOnReview, userId, language, metadataFormat,
            metadata.getRecurrenceType());
    }

    private void restoreRegistryBookReportGeneration(ScheduledTaskMetadata metadata) {
        Map<String, Object> data = metadata.getMetadata();

        var institutionId = (Integer) data.get("institutionId");
        var from = RelativeDateDTO.parse((String) data.get("from"));
        var to = RelativeDateDTO.parse((String) data.get("to"));

        var authorName = (String) data.get("authorName");
        var authorTitle = (String) data.get("authorTitle");

        var lang = (String) data.get("lang");
        var userId = (Integer) data.get("userId");

        registryBookReportService.scheduleReportGeneration(
            from, to, institutionId, lang, userId,
            authorName, authorTitle, metadata.getRecurrenceType()
        );
    }

    public List<FileSection> resolveFileSections(List<String> names) {
        return names.stream()
            .map(this::resolveSingle)
            .toList();
    }

    @SuppressWarnings("unchecked")
    public FileSection resolveSingle(String name) {
        for (Class<? extends Enum<?>> enumClass : FILE_SECTION_ENUMS) {
            try {
                Enum<?> enumValue = Enum.valueOf((Class) enumClass, name);
                return (FileSection) enumValue;
            } catch (IllegalArgumentException ignored) {
                // Try next enum class
            }
        }
        throw new IllegalArgumentException("No FileSection enum found for name: " + name);
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Enum<?>> tryLoadEnum(String className) {
        try {
            return (Class<? extends Enum<?>>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load enum class: " + className, e);
        }
    }
}
