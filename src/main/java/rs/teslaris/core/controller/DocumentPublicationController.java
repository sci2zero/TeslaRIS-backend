package rs.teslaris.core.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.commontypes.ReorderContributionRequestDTO;
import rs.teslaris.core.dto.document.CitationResponseDTO;
import rs.teslaris.core.dto.document.DocumentAffiliationRequestDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.model.document.BibliographicFormat;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.Triple;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.util.search.SearchRequestType;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.signposting.FairSignpostingL2Utility;
import rs.teslaris.core.util.signposting.LinksetFormat;

@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
@Traceable
public class DocumentPublicationController {

    private final DocumentPublicationService documentPublicationService;

    private final DeduplicationService deduplicationService;

    private final JwtUtil tokenUtil;

    private final PersonService personService;

    private final CitationService citationService;

    private final UserService userService;


    @GetMapping("/{documentId}/can-edit")
    @PublicationEditCheck
    public boolean canEditDocumentPublication() {
        return true;
    }

    @GetMapping("/{documentId}")
    public DocumentDTO readDocumentPublication(@PathVariable Integer documentId) {
        return documentPublicationService.readDocumentPublication(documentId);
    }

    @GetMapping("/linkset/{documentId}/{linksetFormat}")
    public ResponseEntity<String> getDocumentLinkset(@PathVariable Integer documentId,
                                                     @PathVariable LinksetFormat linksetFormat) {
        var dto = documentPublicationService.readDocumentPublication(documentId);

        var headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, linksetFormat.getValue());
        return ResponseEntity.ok()
            .headers(headers)
            .body(FairSignpostingL2Utility.createLinksetForDocument(dto, linksetFormat));
    }

    @GetMapping("/metadata/{documentId}/{format}")
    public String readBibliographicMetadata(@PathVariable Integer documentId,
                                            @PathVariable BibliographicFormat format) {
        return documentPublicationService.readBibliographicMetadataById(documentId, format);
    }

    @GetMapping("/{documentId}/cite")
    public CitationResponseDTO getDocumentCitations(@PathVariable Integer documentId,
                                                    @RequestParam("lang") String lang) {
        return citationService.craftCitations(documentId, lang.toUpperCase());
    }

    @GetMapping("/simple-search")
    public Page<DocumentPublicationIndex> simpleSearch(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        @RequestParam(required = false) Integer institutionId,
        @RequestParam(value = "unclassified", defaultValue = "false") Boolean unclassified,
        @RequestParam(value = "authorReprint", defaultValue = "false") Boolean authorReprint,
        @RequestParam(value = "unmanaged", defaultValue = "false") Boolean unmanaged,
        @RequestParam(value = "allowedTypes", required = false)
        List<DocumentPublicationType> allowedTypes,
        @RequestHeader(value = "Authorization", defaultValue = "") String bearerToken,
        Pageable pageable) {
        StringUtil.sanitizeTokens(tokens);

        var isCommission = !bearerToken.isEmpty() &&
            tokenUtil.extractUserRoleFromToken(bearerToken).equals(UserRole.COMMISSION.name());

        return documentPublicationService.searchDocumentPublications(tokens, pageable,
            SearchRequestType.SIMPLE, institutionId, (isCommission && unclassified) ?
                userService.getUserCommissionId(tokenUtil.extractUserIdFromToken(bearerToken)) :
                null, authorReprint, unmanaged, allowedTypes);
    }

    @GetMapping("/advanced-search")
    public Page<DocumentPublicationIndex> advancedSearch(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        @RequestParam(required = false) Integer institutionId,
        @RequestParam(value = "unclassified", defaultValue = "false") Boolean unclassified,
        @RequestParam(value = "allowedTypes", required = false)
        List<DocumentPublicationType> allowedTypes,
        @RequestHeader(value = "Authorization", defaultValue = "") String bearerToken,
        Pageable pageable) {
        var isCommission = !bearerToken.isEmpty() &&
            tokenUtil.extractUserRoleFromToken(bearerToken).equals(UserRole.COMMISSION.name());

        return documentPublicationService.searchDocumentPublications(tokens, pageable,
            SearchRequestType.ADVANCED, institutionId, (isCommission && unclassified) ?
                userService.getUserCommissionId(tokenUtil.extractUserIdFromToken(bearerToken)) :
                null, null, null, allowedTypes);
    }

    @GetMapping("/deduplication-search")
    public Page<DocumentPublicationIndex> deduplicationSearch(
        @RequestParam("titles") List<String> titles, @RequestParam("doi") String doi,
        @RequestParam("scopusId") String scopusId, @RequestParam("openAlexId") String openAlexId,
        @RequestParam("webOfScienceId") String webOfScienceId,
        @RequestParam(value = "internalIdentifiers", required = false)
        List<String> internalIdentifiers) {
        return documentPublicationService.findDocumentDuplicates(titles, doi, scopusId, openAlexId,
            webOfScienceId, internalIdentifiers);
    }

    @GetMapping("/for-researcher/{personId}")
    public Page<DocumentPublicationIndex> findResearcherPublications(
        @RequestParam(value = "tokens", required = false)
        List<String> tokens,
        @RequestParam(value = "allowedTypes", required = false)
        List<DocumentPublicationType> allowedTypes,
        @PathVariable Integer personId,
        @RequestParam(value = "ignore", required = false)
        List<Integer> ignore,
        Pageable pageable) {
        return documentPublicationService.findResearcherPublications(personId,
            Objects.requireNonNullElse(ignore, Collections.emptyList()), tokens, allowedTypes,
            pageable);
    }

    @GetMapping("/research-output/{documentId}")
    public List<Integer> findResearchOutputForDocument(@PathVariable Integer documentId) {
        return documentPublicationService.getResearchOutputIdsForDocument(documentId);
    }

    @GetMapping("/non-affiliated/{organisationUnitId}")
    public Page<DocumentPublicationIndex> findNonAffiliatedPublications(
        @PathVariable Integer organisationUnitId,
        @RequestHeader("Authorization") String bearerToken,
        Pageable pageable) {
        return documentPublicationService.findNonAffiliatedDocuments(organisationUnitId,
            personService.getPersonIdForUserId(tokenUtil.extractUserIdFromToken(bearerToken)),
            pageable);
    }

    @PatchMapping("/add-affiliation/{organisationUnitId}")
    public void addInstitutionToDocuments(
        @RequestBody @Valid DocumentAffiliationRequestDTO documentAffiliationRequest,
        @PathVariable Integer organisationUnitId,
        @RequestHeader("Authorization") String bearerToken) {
        documentPublicationService.massAssignContributionInstitution(organisationUnitId,
            personService.getPersonIdForUserId(tokenUtil.extractUserIdFromToken(bearerToken)),
            documentAffiliationRequest.documentIds(), documentAffiliationRequest.deleteOthers());
    }

    @GetMapping("/for-publisher/{publisherId}")
    public Page<DocumentPublicationIndex> findPublicationsForPublisher(
        @PathVariable Integer publisherId,
        Pageable pageable) {
        return documentPublicationService.findPublicationsForPublisher(publisherId, pageable);
    }

    @GetMapping("/for-organisation-unit/{organisationUnitId}")
    public Page<DocumentPublicationIndex> findPublicationsForOrganisationUnit(
        @RequestParam("tokens")
        @NotNull(message = "You have to provide a valid search input.") List<String> tokens,
        @RequestParam(value = "allowedTypes", required = false)
        List<DocumentPublicationType> allowedTypes,
        @PathVariable Integer organisationUnitId,
        Pageable pageable) {
        return documentPublicationService.findPublicationsForOrganisationUnit(organisationUnitId,
            tokens, allowedTypes, pageable);
    }

    @GetMapping("/count")
    public Long countAll() {
        return documentPublicationService.getPublicationCount();
    }

    @PatchMapping("/{documentId}/approval")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('APPROVE_DOCUMENT')")
    void updateDocumentApprovalStatus(@PathVariable Integer documentId,
                                      @RequestParam Boolean isApproved) {
        documentPublicationService.updateDocumentApprovalStatus(documentId, isApproved);
    }

    @PatchMapping("/{documentId}")
    @PublicationEditCheck
    @Idempotent
    DocumentFileResponseDTO addDocumentFile(@PathVariable Integer documentId,
                                            @ModelAttribute @Valid DocumentFileDTO documentFile,
                                            @RequestParam Boolean isProof) {
        return documentPublicationService.addDocumentFile(documentId, documentFile, isProof);
    }

    @DeleteMapping("/{documentId}/{documentFileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteDocumentFile(@PathVariable Integer documentId,
                                   @PathVariable Integer documentFileId) {
        documentPublicationService.deleteDocumentFile(documentId, documentFileId);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteDocumentPublication(@PathVariable Integer documentId) {
        documentPublicationService.deleteDocumentPublication(documentId);
        deduplicationService.deleteSuggestion(documentId, EntityType.PUBLICATION);
    }

    @PatchMapping("/{documentId}/reorder-contribution/{contributionId}")
    @PublicationEditCheck
    public void reorderContributions(@PathVariable Integer documentId,
                                     @PathVariable Integer contributionId,
                                     @RequestBody ReorderContributionRequestDTO reorderRequest) {
        documentPublicationService.reorderDocumentContributions(documentId, contributionId,
            reorderRequest.getOldContributionOrderNumber(),
            reorderRequest.getNewContributionOrderNumber());
    }

    @PatchMapping("/unbind-researcher/{documentId}")
    @PreAuthorize("hasAuthority('UNBIND_YOURSELF_FROM_PUBLICATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void unbindResearcherFromDocument(@PathVariable Integer documentId,
                                             @RequestHeader(value = "Authorization")
                                             String bearerToken) {
        var userId = tokenUtil.extractUserIdFromToken(bearerToken);
        var personId = personService.getPersonIdForUserId(userId);

        documentPublicationService.unbindResearcherFromContribution(personId, documentId);
    }

    @GetMapping("/identifier-usage/{documentId}")
    @PublicationEditCheck
    public boolean checkIdentifierUsage(@PathVariable Integer documentId,
                                        @RequestParam String identifier) {
        return documentPublicationService.isIdentifierInUse(identifier, documentId);
    }

    @GetMapping("/doi-usage")
    public boolean checkDoiUsage(@RequestParam String doi) {
        return documentPublicationService.isDoiInUse(doi);
    }

    @GetMapping("/fields")
    public List<Triple<String, List<MultilingualContentDTO>, String>> getSearchFields(
        @RequestParam("export") Boolean onlyExportFields) {
        return documentPublicationService.getSearchFields(onlyExportFields);
    }

    @GetMapping("/wordcloud/{documentId}")
    public List<Pair<String, Long>> getWordCloudForSingleDocument(@PathVariable Integer documentId,
                                                                  @RequestParam
                                                                  String language) {
        return documentPublicationService.getWordCloudForSingleDocument(documentId,
            language.toUpperCase());
    }

    @PatchMapping("/archive/{documentId}")
    @PreAuthorize("hasAuthority('ARCHIVE_DOCUMENT')")
    @PublicationEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveDocument(@PathVariable Integer documentId) {
        documentPublicationService.archiveDocument(documentId);
    }

    @PatchMapping("/unarchive/{documentId}")
    @PreAuthorize("hasAuthority('ARCHIVE_DOCUMENT')")
    @PublicationEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unarchiveDocument(@PathVariable Integer documentId) {
        documentPublicationService.unarchiveDocument(documentId);
    }
}
