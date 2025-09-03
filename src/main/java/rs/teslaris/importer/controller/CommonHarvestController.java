package rs.teslaris.importer.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.RelativeDateDTO;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;
import rs.teslaris.importer.dto.AuthorCentricInstitutionHarvestRequestDTO;
import rs.teslaris.importer.service.interfaces.BibTexHarvester;
import rs.teslaris.importer.service.interfaces.CSVHarvester;
import rs.teslaris.importer.service.interfaces.CommonHarvestService;
import rs.teslaris.importer.service.interfaces.EndNoteHarvester;
import rs.teslaris.importer.service.interfaces.OpenAlexHarvester;
import rs.teslaris.importer.service.interfaces.RefManHarvester;
import rs.teslaris.importer.service.interfaces.ScopusHarvester;
import rs.teslaris.importer.service.interfaces.WebOfScienceHarvester;

@RestController
@RequestMapping("/api/import-common")
@RequiredArgsConstructor
@Traceable
@Slf4j
public class CommonHarvestController {

    private final JwtUtil tokenUtil;

    private final ScopusHarvester scopusHarvester;

    private final OpenAlexHarvester openAlexHarvester;

    private final WebOfScienceHarvester webOfScienceHarvester;

    private final BibTexHarvester bibTexHarvester;

    private final RefManHarvester refManHarvester;

    private final EndNoteHarvester endNoteHarvester;

    private final CSVHarvester csvHarvester;

    private final NotificationService notificationService;

    private final UserService userService;

    private final CommonHarvestService commonHarvestService;

    private final TaskManagerService taskManagerService;


    @GetMapping("/can-perform")
    public boolean canPerformHarvest(@RequestHeader("Authorization") String bearerToken) {
        var userId = tokenUtil.extractUserIdFromToken(bearerToken);
        var userRole = tokenUtil.extractUserRoleFromToken(bearerToken);

        if (userRole.equals(UserRole.RESEARCHER.name())) {
            return commonHarvestService.canPersonScanDataSources(userId);
        } else if (userRole.equals(UserRole.INSTITUTIONAL_EDITOR.name())) {
            return commonHarvestService.canOUEmployeeScanDataSources(
                userService.getUserOrganisationUnitId(userId));
        }

        return userRole.equals(UserRole.ADMIN.name());
    }

    @GetMapping("/documents-by-author-or-institution")
    public Integer harvestPublicationsForAuthor(
        @RequestHeader("Authorization") String bearerToken, @RequestParam LocalDate dateFrom,
        @RequestParam LocalDate dateTo, @RequestParam(required = false) Integer institutionId) {
        var userId = tokenUtil.extractUserIdFromToken(bearerToken);
        var userRole = tokenUtil.extractUserRoleFromToken(bearerToken);

        return performHarvest(userId, userRole, dateFrom, dateTo, institutionId);
    }

    @PostMapping("/schedule/documents-by-author-or-institution")
    @PreAuthorize("hasAuthority('SCHEDULE_DOCUMENT_HARVEST')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void scheduleHarvestPublicationsForAuthor(
        @RequestHeader("Authorization") String bearerToken, @RequestParam RelativeDateDTO dateFrom,
        @RequestParam RelativeDateDTO dateTo, @RequestParam(required = false) Integer institutionId,
        @RequestParam("timestamp") LocalDateTime timestamp,
        @RequestParam("recurrence") RecurrenceType recurrenceType) {
        var userId = tokenUtil.extractUserIdFromToken(bearerToken);
        var userRole = tokenUtil.extractUserRoleFromToken(bearerToken);

        var taskId = taskManagerService.scheduleTask(
            "Harvest-" +
                ((Objects.nonNull(institutionId) && institutionId > 0) ? institutionId :
                    userService.getUserOrganisationUnitId(userId)) +
                "-" + dateFrom + "_" + dateTo +
                "-" + UUID.randomUUID(), timestamp,
            () -> performHarvest(userId, userRole, dateFrom.computeDate(), dateTo.computeDate(),
                institutionId),
            userId, recurrenceType);

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timestamp,
                ScheduledTaskType.PUBLICATION_HARVEST, new HashMap<>() {{
                put("userRole", userRole);
                put("dateFrom", dateFrom.toString());
                put("dateTo", dateTo.toString());
                put("userId", userId);
                put("institutionId", institutionId);
            }}, recurrenceType));
    }

    public Integer performHarvest(Integer userId, String userRole, LocalDate dateFrom,
                                  LocalDate dateTo, Integer institutionId) {
        Map<Integer, Integer> newDocumentImportCountByUser = new HashMap<>();

        if (userRole.equals(UserRole.RESEARCHER.name())) {
            scopusHarvester.harvestDocumentsForAuthor(userId, dateFrom, dateTo, new HashMap<>())
                .forEach((key, value) ->
                    newDocumentImportCountByUser.merge(key, value, Integer::sum)
                );
            openAlexHarvester.harvestDocumentsForAuthor(userId, dateFrom, dateTo, new HashMap<>())
                .forEach((key, value) ->
                    newDocumentImportCountByUser.merge(key, value, Integer::sum)
                );
            webOfScienceHarvester.harvestDocumentsForAuthor(userId, dateFrom, dateTo,
                    new HashMap<>())
                .forEach((key, value) ->
                    newDocumentImportCountByUser.merge(key, value, Integer::sum)
                );
        } else if (userRole.equals(UserRole.INSTITUTIONAL_EDITOR.name())) {
            scopusHarvester.harvestDocumentsForInstitutionalEmployee(userId, null, dateFrom,
                dateTo,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            openAlexHarvester.harvestDocumentsForInstitutionalEmployee(userId, null, dateFrom,
                dateTo,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            webOfScienceHarvester.harvestDocumentsForInstitutionalEmployee(userId, null, dateFrom,
                dateTo,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
        } else if (userRole.equals(UserRole.ADMIN.name())) {
            scopusHarvester.harvestDocumentsForInstitutionalEmployee(userId, institutionId,
                dateFrom, dateTo,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            openAlexHarvester.harvestDocumentsForInstitutionalEmployee(userId, institutionId,
                dateFrom, dateTo,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            webOfScienceHarvester.harvestDocumentsForInstitutionalEmployee(userId, institutionId,
                dateFrom, dateTo,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
        } else {
            return 0;
        }

        dispatchNotifications(newDocumentImportCountByUser, userId);
        return newDocumentImportCountByUser.getOrDefault(userId, 0);
    }

    @PostMapping("/author-centric-for-institution")
    public Integer performAuthorCentricHarvestForInstitution(
        @RequestHeader("Authorization") String bearerToken, @RequestParam LocalDate dateFrom,
        @RequestParam LocalDate dateTo,
        @RequestBody AuthorCentricInstitutionHarvestRequestDTO request) {
        var userId = tokenUtil.extractUserIdFromToken(bearerToken);
        var userRole = tokenUtil.extractUserRoleFromToken(bearerToken);

        return performAuthorCentricLoading(userId, userRole, dateFrom, dateTo, request.authorIds(),
            request.allAuthors(), request.institutionId());
    }

    @PostMapping("/schedule/author-centric-for-institution")
    @PreAuthorize("hasAuthority('SCHEDULE_DOCUMENT_HARVEST')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void scheduleAuthorCentricHarvestForInstitution(
        @RequestHeader("Authorization") String bearerToken, @RequestParam RelativeDateDTO dateFrom,
        @RequestParam RelativeDateDTO dateTo, @RequestParam("timestamp") LocalDateTime timestamp,
        @RequestParam("recurrence") RecurrenceType recurrenceType,
        @RequestBody AuthorCentricInstitutionHarvestRequestDTO request) {
        var userId = tokenUtil.extractUserIdFromToken(bearerToken);
        var userRole = tokenUtil.extractUserRoleFromToken(bearerToken);

        var taskId = taskManagerService.scheduleTask(
            "Harvest-" +
                ((Objects.nonNull(request.institutionId()) && request.institutionId() > 0) ?
                    request.institutionId() : userService.getUserOrganisationUnitId(userId)) +
                "-" + dateFrom + "_" + dateTo +
                "-" + UUID.randomUUID(), timestamp,
            () -> performAuthorCentricLoading(userId, userRole, dateFrom.computeDate(),
                dateTo.computeDate(), request.authorIds(), request.allAuthors(),
                request.institutionId()),
            userId, recurrenceType);

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timestamp,
                ScheduledTaskType.AUTHOR_CENTRIC_PUBLICATION_HARVEST, new HashMap<>() {{
                put("userRole", userRole);
                put("dateFrom", dateFrom.toString());
                put("dateTo", dateTo.toString());
                put("authorIds", request.authorIds());
                put("allAuthors", request.allAuthors());
                put("userId", userId);
                put("institutionId", request.institutionId());
            }}, recurrenceType));
    }

    public Integer performAuthorCentricLoading(Integer userId, String userRole, LocalDate dateFrom,
                                               LocalDate dateTo, List<Integer> authorIds,
                                               Boolean allAuthors, Integer institutionId) {
        Map<Integer, Integer> newDocumentImportCountByUser = new HashMap<>();
        if (userRole.equals(UserRole.INSTITUTIONAL_EDITOR.name())) {
            scopusHarvester.harvestDocumentsForInstitution(userId, null, dateFrom,
                dateTo, authorIds, allAuthors,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            openAlexHarvester.harvestDocumentsForInstitution(userId, null, dateFrom,
                dateTo, authorIds, allAuthors,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            webOfScienceHarvester.harvestDocumentsForInstitution(userId, null, dateFrom,
                dateTo, authorIds, allAuthors,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
        } else if (userRole.equals(UserRole.ADMIN.name())) {
            scopusHarvester.harvestDocumentsForInstitution(userId, institutionId,
                dateFrom, dateTo, authorIds, allAuthors,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            openAlexHarvester.harvestDocumentsForInstitution(userId, institutionId,
                dateFrom, dateTo, authorIds, allAuthors,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            webOfScienceHarvester.harvestDocumentsForInstitution(userId, institutionId,
                dateFrom, dateTo, authorIds, allAuthors,
                new HashMap<>()).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
        } else {
            return 0;
        }

        dispatchNotifications(newDocumentImportCountByUser, userId);
        return newDocumentImportCountByUser.getOrDefault(userId, 0);
    }

    private void dispatchNotifications(Map<Integer, Integer> newDocumentImportCountByUser,
                                       Integer userId) {
        newDocumentImportCountByUser.keySet().forEach(key -> {
            if (Objects.equals(key, userId)) {
                return;
            }

            var notificationValues = new HashMap<String, String>();
            notificationValues.put("newImportCount",
                String.valueOf(newDocumentImportCountByUser.get(key)));
            notificationService.createNotification(
                NotificationFactory.contructNewImportsNotification(notificationValues,
                    userService.findOne(key)));
        });
    }

    @GetMapping("/csv-file-format")
    public Pair<String, String> getCSVFormatDescription(@RequestParam String language) {
        return csvHarvester.getFormatDescription(language);
    }

    @GetMapping("/testWebOfScience")
    public void testWebOfScience() {
        webOfScienceHarvester.harvestDocumentsForAuthor(1, LocalDate.of(2000, 1, 2),
            LocalDate.now(), null);
    }

    @PostMapping("/documents-from-file")
    @Idempotent
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Integer harvestPublicationsFromFile(
        @RequestParam("files") List<MultipartFile> publicationsFiles,
        @RequestHeader("Authorization") String bearerToken) throws IOException {

        var userId = tokenUtil.extractUserIdFromToken(bearerToken);
        var newDocumentImportCountByUser = new HashMap<Integer, Integer>();

        for (MultipartFile publicationsFile : publicationsFiles) {
            try {
                validateFileMetadata(publicationsFile);

                String detectedType = detectContentTypeWithTika(publicationsFile);
                validateContentType(detectedType,
                    Objects.requireNonNull(publicationsFile.getOriginalFilename()));

                var tempFile = Files.createTempFile("upload-", ".tmp");
                try {
                    publicationsFile.transferTo(tempFile);
                    validateFileContent(tempFile, detectedType);

                    processVerifiedFile(userId, publicationsFile,
                        publicationsFile.getOriginalFilename(),
                        newDocumentImportCountByUser);
                } finally {
                    Files.deleteIfExists(tempFile);
                }
            } catch (SecurityException e) {
                log.warn("Rejected suspicious file upload from user {}: {}", userId,
                    e.getMessage());
            }
        }

        return newDocumentImportCountByUser.getOrDefault(userId, 0);
    }

    private void validateFileMetadata(MultipartFile file) {
        if (file.isEmpty()) {
            throw new SecurityException("Empty file");
        }

        String filename = Objects.requireNonNull(file.getOriginalFilename());
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new SecurityException("Invalid filename");
        }
    }

    private String detectContentTypeWithTika(MultipartFile file) throws IOException {
        try (var stream = file.getInputStream()) {
            var tika = new Tika();
            return tika.detect(stream);
        }
    }

    private void validateContentType(String detectedType, String filename) {
        Map<String, String> allowedTypes = Map.of(
            ".bib", "application/x-bibtex-text-file",
            ".ris", "text/plain",
            ".enw", "text/x-matlab",
            ".csv", "text/plain"
        );

        String extension = filename.substring(filename.lastIndexOf('.')).toLowerCase();
        String expectedType = allowedTypes.get(extension);

        if (!detectedType.equals(expectedType)) {
            throw new SecurityException("Unexpected MIME type: " + detectedType);
        }
    }

    private void validateFileContent(Path file, String detectedType) throws IOException {
        if (detectedType.startsWith("text/") && isBinaryFile(file)) {
            throw new SecurityException("Binary content in text file");
        }
    }

    private boolean isBinaryFile(Path file) throws IOException {
        try (var is = Files.newInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead = is.read(buffer);
            return bytesRead > 0 && containsNullByte(buffer, bytesRead);
        }
    }

    private boolean containsNullByte(byte[] buffer, int length) {
        for (int i = 0; i < length; i++) {
            if (buffer[i] == 0) {
                return true;
            }
        }
        return false;
    }

    private void processVerifiedFile(Integer userId, MultipartFile file, String filename,
                                     HashMap<Integer, Integer> counts) {
        if (filename.endsWith(".bib")) {
            bibTexHarvester.harvestDocumentsForAuthor(userId, file, counts);
        } else if (filename.endsWith(".ris")) {
            refManHarvester.harvestDocumentsForAuthor(userId, file, counts);
        } else if (filename.endsWith(".enw")) {
            endNoteHarvester.harvestDocumentsForAuthor(userId, file, counts);
        } else if (filename.endsWith(".csv")) {
            csvHarvester.harvestDocumentsForAuthor(userId, file, counts);
        }
    }
}
