package rs.teslaris.importer.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.person.InternalIdentifierMigrationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentMigrationDTO;
import rs.teslaris.core.dto.person.involvement.ExtraEmploymentMigrationDTO;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.InvolvementService;

@RestController
@RequestMapping("/api/extra-migration")
@RequiredArgsConstructor
public class ExtraMigrationController {

    private final EventService eventService;

    private final InvolvementService involvementService;

    private final DocumentPublicationService documentPublicationService;

    private final OrganisationUnitService organisationUnitService;


    @PatchMapping("/event")
    @PreAuthorize("hasAuthority('PERFORM_EXTRA_MIGRATION_OPERATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enrichEventInformationFromExternalSource(@RequestParam Integer oldId,
                                                         @RequestParam LocalDate dateFrom,
                                                         @RequestParam LocalDate dateTo) {
        eventService.enrichEventInformationFromExternalSource(oldId, dateFrom, dateTo);
    }

    @PatchMapping("/person-internal-identifier")
    @PreAuthorize("hasAuthority('PERFORM_EXTRA_MIGRATION_OPERATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enrichPersonInternalIdsFromExternalSource(@RequestBody
                                                          InternalIdentifierMigrationDTO dto) {
        involvementService.migrateEmployeeInternalIdentifiers(dto);
    }

    @PatchMapping("/org-unit-internal-identifier")
    @PreAuthorize("hasAuthority('PERFORM_EXTRA_MIGRATION_OPERATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enrichOrganisationUnitInternalIdsFromExternalSource(@RequestBody
                                                                    InternalIdentifierMigrationDTO dto) {
        organisationUnitService.migrateInstitutionInternalIdentifiers(dto);
    }

    @GetMapping("/check-existence/{oldId}")
    @PreAuthorize("hasAuthority('PERFORM_EXTRA_MIGRATION_OPERATIONS')")
    public boolean checkIfDocumentExists(@PathVariable Integer oldId) {
        return Objects.nonNull(documentPublicationService.findDocumentByOldId(oldId));
    }

    @PostMapping("/migrate-employment")
    @PreAuthorize("hasAuthority('PERFORM_EXTRA_MIGRATION_OPERATIONS')")
    @ResponseStatus(HttpStatus.CREATED)
    public EmploymentDTO migrateEmploymentOld(
        @Valid @RequestBody EmploymentMigrationDTO employmentMigrationDTO) {
        return involvementService.migrateEmployment(employmentMigrationDTO);
    }

    @PostMapping("/migrate-employment/new")
    @PreAuthorize("hasAuthority('PERFORM_EXTRA_MIGRATION_OPERATIONS')")
    @ResponseStatus(HttpStatus.CREATED)
    public void migrateEmploymentNew(
        @Valid @RequestBody List<ExtraEmploymentMigrationDTO> employmentMigrationDTO) {
        involvementService.migrateEmployment(employmentMigrationDTO);
    }
}
