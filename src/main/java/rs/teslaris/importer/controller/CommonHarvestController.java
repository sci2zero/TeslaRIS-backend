package rs.teslaris.importer.controller;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;
import rs.teslaris.importer.service.interfaces.ScopusHarvester;
import rs.teslaris.importer.utility.scopus.ScopusImportUtility;

@RestController
@RequestMapping("/api/import-common")
@RequiredArgsConstructor
@Traceable
public class CommonHarvestController {

    private final JwtUtil tokenUtil;

    private final ScopusHarvester scopusHarvester;

    private final NotificationService notificationService;

    private final UserService userService;

    private final PersonService personService;

    @Value("${scopus.api.key}")
    private String apiKey;


    @PostConstruct
    public void init() {
        ScopusImportUtility.headers.put("X-ELS-APIKey", apiKey);
    }

    @GetMapping("/can-perform")
    public boolean canPerformHarvest(@RequestHeader("Authorization") String bearerToken) {
        var userId = tokenUtil.extractUserIdFromToken(bearerToken);
        return personService.canPersonScanDataSources(personService.getPersonIdForUserId(userId));
    }

    @GetMapping("/documents-by-author")
    public Integer harvestPublicationsForAuthor(
        @RequestHeader("Authorization") String bearerToken, @RequestParam LocalDate dateFrom,
        @RequestParam LocalDate dateTo) {
        var startYear = dateFrom.getYear();
        var endYear = dateTo.getYear();

        var userId = tokenUtil.extractUserIdFromToken(bearerToken);
        var newEntriesCount = new HashMap<Integer, Integer>();

        var newDocumentImportCountByUser =
            scopusHarvester.harvestDocumentsForAuthor(userId, startYear, endYear, newEntriesCount);

        // TODO: All other harvesters are called here sequentially

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
}
