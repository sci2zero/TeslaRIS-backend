package rs.teslaris.importer.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;
import rs.teslaris.importer.service.interfaces.BibTexHarvester;
import rs.teslaris.importer.service.interfaces.CSVHarvester;
import rs.teslaris.importer.service.interfaces.EndNoteHarvester;
import rs.teslaris.importer.service.interfaces.OpenAlexHarvester;
import rs.teslaris.importer.service.interfaces.RefManHarvester;
import rs.teslaris.importer.service.interfaces.ScopusHarvester;

@RestController
@RequestMapping("/api/import-common")
@RequiredArgsConstructor
@Traceable
@Slf4j
public class CommonHarvestController {

    private final JwtUtil tokenUtil;

    private final ScopusHarvester scopusHarvester;

    private final OpenAlexHarvester openAlexHarvester;

    private final BibTexHarvester bibTexHarvester;

    private final RefManHarvester refManHarvester;

    private final EndNoteHarvester endNoteHarvester;

    private final CSVHarvester csvHarvester;

    private final NotificationService notificationService;

    private final UserService userService;

    private final PersonService personService;

    private final OrganisationUnitService organisationUnitService;


    @GetMapping("/can-perform")
    public boolean canPerformHarvest(@RequestHeader("Authorization") String bearerToken) {
        var userId = tokenUtil.extractUserIdFromToken(bearerToken);
        var userRole = tokenUtil.extractUserRoleFromToken(bearerToken);

        if (userRole.equals(UserRole.RESEARCHER.name())) {
            return personService.canPersonScanDataSources(
                personService.getPersonIdForUserId(userId));
        } else if (userRole.equals(UserRole.INSTITUTIONAL_EDITOR.name())) {
            return organisationUnitService.canOUEmployeeScanDataSources(
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

        var newEntriesCount = new HashMap<Integer, Integer>();
        Map<Integer, Integer> newDocumentImportCountByUser = new HashMap<>();

        if (userRole.equals(UserRole.RESEARCHER.name())) {
            scopusHarvester.harvestDocumentsForAuthor(userId, dateFrom, dateTo, newEntriesCount)
                .forEach((key, value) ->
                    newDocumentImportCountByUser.merge(key, value, Integer::sum)
                );
            openAlexHarvester.harvestDocumentsForAuthor(userId, dateFrom, dateTo, newEntriesCount)
                .forEach((key, value) ->
                    newDocumentImportCountByUser.merge(key, value, Integer::sum)
                );
        } else if (userRole.equals(UserRole.INSTITUTIONAL_EDITOR.name())) {
            scopusHarvester.harvestDocumentsForInstitutionalEmployee(userId, null, dateFrom,
                dateTo,
                newEntriesCount).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            openAlexHarvester.harvestDocumentsForInstitutionalEmployee(userId, null, dateFrom,
                dateTo,
                newEntriesCount).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
        } else if (userRole.equals(UserRole.ADMIN.name())) {
            scopusHarvester.harvestDocumentsForInstitutionalEmployee(userId, institutionId,
                dateFrom, dateTo,
                newEntriesCount).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
            openAlexHarvester.harvestDocumentsForInstitutionalEmployee(userId, institutionId,
                dateFrom, dateTo,
                newEntriesCount).forEach((key, value) ->
                newDocumentImportCountByUser.merge(key, value, Integer::sum)
            );
        } else {
            return 0;
        }

        newDocumentImportCountByUser.keySet().forEach(key -> {
            if (Objects.equals(key, userId)) {
                return;
            }

            var notificationValues = new HashMap<String, String>();
            notificationValues.put("newImportCount", newDocumentImportCountByUser.get(key) + "");
            notificationService.createNotification(
                NotificationFactory.contructNewImportsNotification(notificationValues,
                    userService.findOne(key)));
        });

        return newDocumentImportCountByUser.getOrDefault(userId, 0);
    }

    @GetMapping("/csv-file-format")
    public Pair<String, String> getCSVFormatDescription(@RequestParam String language) {
        return csvHarvester.getFormatDescription(language);
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
