package rs.teslaris.thesislibrary.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.service.interfaces.commontypes.ReCaptchaService;
import rs.teslaris.core.util.exceptionhandling.exception.CaptchaException;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.thesislibrary.annotation.PromotionEditAndUsageCheck;
import rs.teslaris.thesislibrary.annotation.RegistryBookEntryEditCheck;
import rs.teslaris.thesislibrary.dto.AttendanceCancellationRequest;
import rs.teslaris.thesislibrary.dto.InstitutionCountsReportDTO;
import rs.teslaris.thesislibrary.dto.PhdThesisPrePopulatedDataDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookEntryDTO;
import rs.teslaris.thesislibrary.service.interfaces.RegistryBookService;

@RestController
@RequestMapping("/api/registry-book")
@RequiredArgsConstructor
@Traceable
public class RegistryBookController {

    private final RegistryBookService registryBookService;

    private final JwtUtil tokenUtil;

    private final ReCaptchaService reCaptchaService;


    @GetMapping("/can-edit/{registryBookEntryId}")
    @PreAuthorize("hasAuthority('UPDATE_REGISTRY_BOOK')")
    @RegistryBookEntryEditCheck
    public boolean canEditEntry(@PathVariable Integer registryBookEntryId) {
        return registryBookService.canEdit(registryBookEntryId);
    }

    @GetMapping("/can-add/{documentId}")
    @PreAuthorize("hasAuthority('ADD_TO_REGISTRY_BOOK')")
    @PublicationEditCheck("THESIS")
    public Integer getCanAdd(@PathVariable Integer documentId) {
        return registryBookService.hasThesisRegistryBookEntry(documentId);
    }

    @GetMapping("/{registryBookEntryId}")
    @PreAuthorize("hasAuthority('UPDATE_REGISTRY_BOOK')")
    public RegistryBookEntryDTO getRegistryBookEntriesForPromotion(
        @PathVariable Integer registryBookEntryId) {
        return registryBookService.readRegistryBookEntry(registryBookEntryId);
    }

    @GetMapping("/for-promotion/{promotionId}")
    @PreAuthorize("hasAnyAuthority('UPDATE_REGISTRY_BOOK', 'REMOVE_FROM_PROMOTION')")
    @PromotionEditAndUsageCheck
    public Page<RegistryBookEntryDTO> getRegistryBookEntriesForPromotion(
        @PathVariable Integer promotionId, Pageable pageable) {
        return registryBookService.getRegistryBookEntriesForPromotion(promotionId, pageable);
    }

    @GetMapping("/non-promoted")
    @PreAuthorize("hasAuthority('ADD_TO_PROMOTION')")
    public Page<RegistryBookEntryDTO> getNonPromotedRegistryBookEntries(
        @RequestHeader("Authorization") String bearerToken, Pageable pageable) {
        return registryBookService.getNonPromotedRegistryBookEntries(
            tokenUtil.extractUserIdFromToken(bearerToken), pageable);
    }

    @PostMapping("/{documentId}")
    @PreAuthorize("hasAuthority('ADD_TO_REGISTRY_BOOK')")
    @PublicationEditCheck("THESIS")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public RegistryBookEntryDTO createRegistryBookEntry(
        @RequestBody @Valid RegistryBookEntryDTO registryBookEntryDTO,
        @PathVariable Integer documentId) {
        var newRegistryBookEntry =
            registryBookService.createRegistryBookEntry(registryBookEntryDTO, documentId);
        registryBookEntryDTO.setId(newRegistryBookEntry.getId());

        return registryBookEntryDTO;
    }

    @PostMapping("/migrate/{documentId}")
    @PreAuthorize("hasAuthority('PERFORM_MIGRATION')")
    @PublicationEditCheck("THESIS")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public RegistryBookEntryDTO migrateRegistryBookEntry(
        @RequestBody RegistryBookEntryDTO registryBookEntryDTO,
        @PathVariable Integer documentId) {
        var newRegistryBookEntry =
            registryBookService.migrateRegistryBookEntry(registryBookEntryDTO, documentId);
        registryBookEntryDTO.setId(newRegistryBookEntry.getId());

        return registryBookEntryDTO;
    }

    @PutMapping("/{registryBookEntryId}")
    @PreAuthorize("hasAuthority('UPDATE_REGISTRY_BOOK')")
    @RegistryBookEntryEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateRegistryBookEntry(@PathVariable Integer registryBookEntryId,
                                        @RequestBody
                                        @Valid RegistryBookEntryDTO registryBookEntryDTO) {
        registryBookService.updateRegistryBookEntry(registryBookEntryId, registryBookEntryDTO);
    }

    @DeleteMapping("/{registryBookEntryId}")
    @PreAuthorize("hasAuthority('REMOVE_FROM_REGISTRY_BOOK')")
    @RegistryBookEntryEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRegistryBookEntry(@PathVariable Integer registryBookEntryId) {
        registryBookService.deleteRegistryBookEntry(registryBookEntryId);
    }

    @GetMapping("/pre-populate/{documentId}")
    @PreAuthorize("hasAuthority('ADD_TO_REGISTRY_BOOK')")
    @PublicationEditCheck("THESIS")
    public PhdThesisPrePopulatedDataDTO getEntryPrePopulatedData(@PathVariable Integer documentId) {
        return registryBookService.getPrePopulatedPHDThesisInformation(documentId);
    }

    @PatchMapping("/add/{registryBookEntryId}/{promotionId}")
    @PreAuthorize("hasAuthority('ADD_TO_PROMOTION')")
    @PromotionEditAndUsageCheck
    @RegistryBookEntryEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addRegistryBookEntryForPromotion(@PathVariable Integer registryBookEntryId,
                                                 @PathVariable Integer promotionId) {
        registryBookService.addToPromotion(registryBookEntryId, promotionId);
    }

    @PatchMapping("/remove/{registryBookEntryId}")
    @PreAuthorize("hasAuthority('REMOVE_FROM_PROMOTION')")
    @RegistryBookEntryEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRegistryBookEntryFromPromotion(@PathVariable Integer registryBookEntryId) {
        registryBookService.removeFromPromotion(registryBookEntryId);
    }

    @PatchMapping("/cancel-attendance")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelAttendance(@RequestBody AttendanceCancellationRequest request) {
        if (!reCaptchaService.isCaptchaValid(request.captchaToken())) {
            throw new CaptchaException("Invalid captcha solution.");
        }

        registryBookService.removeFromPromotion(request.attendanceIdentifier());
    }

    @PatchMapping("/promote-all/{promotionId}")
    @PromotionEditAndUsageCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void promoteAllFromPromotion(@PathVariable Integer promotionId) {
        registryBookService.promoteAll(promotionId);
    }

    @PatchMapping("/preview-promote-all/{promotionId}")
    @PromotionEditAndUsageCheck
    public List<List<String>> previewPromoteAllFromPromotion(@PathVariable Integer promotionId,
                                                             @RequestParam String lang) {
        return registryBookService.previewPromotedEntries(promotionId, lang);
    }

    @GetMapping("/addresses/{promotionId}")
    @PromotionEditAndUsageCheck
    public List<String> getAddressList(@PathVariable Integer promotionId) {
        return registryBookService.getAddressesList(promotionId);
    }

    @GetMapping("/promotees/{promotionId}")
    @PromotionEditAndUsageCheck
    public List<String> getPromoteesList(@PathVariable Integer promotionId) {
        return registryBookService.getPromoteesList(promotionId);
    }

    @GetMapping("/count-report")
    @PreAuthorize("hasAuthority('GENERATE_PROMOTION_REPORT')")
    public List<InstitutionCountsReportDTO> getInstitutionCountsReport(
        @RequestParam
        LocalDate from,
        @RequestParam
        LocalDate to,
        @RequestHeader("Authorization")
        String bearerToken) {
        return registryBookService.institutionCountsReport(
            tokenUtil.extractUserIdFromToken(bearerToken), from, to);
    }

    @GetMapping("/promoted/{institutionId}")
    @PreAuthorize("hasAuthority('GENERATE_PROMOTION_REPORT')")
    public Page<RegistryBookEntryDTO> getRegistryBookContent(@PathVariable Integer institutionId,
                                                             @RequestParam(required = false)
                                                             LocalDate from,
                                                             @RequestParam(required = false)
                                                             LocalDate to,
                                                             @RequestParam(required = false, defaultValue = "")
                                                             String authorName,
                                                             @RequestParam(required = false, defaultValue = "")
                                                             String authorTitle,
                                                             Pageable pageable,
                                                             @RequestHeader("Authorization")
                                                             String bearerToken) {
        return registryBookService.getRegistryBookForInstitutionAndPeriod(
            tokenUtil.extractUserIdFromToken(bearerToken), institutionId,
            Objects.requireNonNullElse(from, LocalDate.of(1000, 1, 1)),
            Objects.requireNonNullElse(to, LocalDate.now()), authorName, authorTitle, pageable);
    }

    @PatchMapping("/allow-single-update/{registryBookEntryId}")
    @PreAuthorize("hasAuthority('ALLOW_REG_ENTRY_SINGLE_UPDATE')")
    @RegistryBookEntryEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void allowSingleUpdate(@PathVariable Integer registryBookEntryId) {
        registryBookService.allowSingleUpdate(registryBookEntryId);
    }

    @GetMapping("/is-attendance-cancellable/{attendanceIdentifier}")
    public boolean isAttendanceNotCanceled(@PathVariable String attendanceIdentifier) {
        return registryBookService.isAttendanceNotCancelled(attendanceIdentifier);
    }
}
