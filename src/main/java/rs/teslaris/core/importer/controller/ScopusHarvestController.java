package rs.teslaris.core.importer.controller;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.importer.service.interfaces.ScopusHarvester;
import rs.teslaris.core.importer.utility.scopus.ScopusImportUtility;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/import-scopus")
@RequiredArgsConstructor
public class ScopusHarvestController {

    private final JwtUtil tokenUtil;

    private final ScopusHarvester scopusHarvester;

    @Value("${scopus.api.key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        ScopusImportUtility.headers.put("X-ELS-APIKey", apiKey);
    }

    @GetMapping("/documents-by-author")
    public Integer harvestPublicationsForAuthor(
        @RequestHeader("Authorization") String bearerToken, @RequestParam Integer startYear,
        @RequestParam Integer endYear) {
        var userId = tokenUtil.extractUserIdFromToken(bearerToken.split(" ")[1]);

        return scopusHarvester.harvestDocumentsForAuthor(userId, startYear, endYear);
    }
}
