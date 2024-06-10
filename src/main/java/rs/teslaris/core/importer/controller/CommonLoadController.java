package rs.teslaris.core.importer.controller;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.importer.service.interfaces.CommonLoader;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/load")
@RequiredArgsConstructor
public class CommonLoadController {

    private final CommonLoader loader;

    private final JwtUtil tokenUtil;


    @PatchMapping("/skip")
    public void skipRecord(@RequestHeader("Authorization") String bearerToken) {
        loader.skipRecord(tokenUtil.extractUserIdFromToken(bearerToken.split(" ")[1]));
    }

    @PatchMapping("/mark-as-loaded")
    public void markRecordAsLoaded(@RequestHeader("Authorization") String bearerToken) {
        loader.markRecordAsLoaded(tokenUtil.extractUserIdFromToken(bearerToken.split(" ")[1]));
    }

    @GetMapping("/load-wizard/count-remaining")
    public Integer getRemainingRecordsCount(
        @RequestHeader("Authorization") String bearerToken) {
        return loader.countRemainingDocumentsForLoading(
            tokenUtil.extractUserIdFromToken(bearerToken.split(" ")[1]));
    }

    @GetMapping("/load-wizard")
    @SuppressWarnings("unchecked")
    private <R> R loadUsingWizard(@RequestHeader("Authorization") String bearerToken) {
        var returnDto =
            loader.loadRecordsWizard(tokenUtil.extractUserIdFromToken(bearerToken.split(" ")[1]));

        if (Objects.isNull(returnDto)) {
            return loader.loadSkippedRecordsWizard(
                tokenUtil.extractUserIdFromToken(bearerToken.split(" ")[1]));
        }

        return (R) returnDto;
    }
}
