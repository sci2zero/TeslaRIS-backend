package rs.teslaris.core.assessment.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.assessment.converter.IndicatorConverter;
import rs.teslaris.core.assessment.dto.IndicatorDTO;
import rs.teslaris.core.assessment.dto.IndicatorResponseDTO;
import rs.teslaris.core.assessment.model.ApplicableEntityType;
import rs.teslaris.core.assessment.service.interfaces.IndicatorService;

@RestController
@RequestMapping("api/assessment/indicator")
@RequiredArgsConstructor
public class IndicatorController {

    private final IndicatorService indicatorService;


    @GetMapping
    public Page<IndicatorResponseDTO> readIndicators(@RequestParam("lang") String language,
                                                     Pageable pageable) {
        return indicatorService.readAllIndicators(pageable, language.toUpperCase());
    }

    @GetMapping("/{indicatorId}")
    public IndicatorResponseDTO readIndicator(@PathVariable Integer indicatorId) {
        return indicatorService.readIndicatorById(indicatorId);
    }

    @GetMapping("/access-level/{indicatorId}")
    @PreAuthorize("hasAuthority('EDIT_INDICATORS')")
    public String readIndicatorAccessLevel(@PathVariable Integer indicatorId) {
        return indicatorService.readIndicatorAccessLEvel(indicatorId).name();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_INDICATORS')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public IndicatorResponseDTO createIndicator(@RequestBody IndicatorDTO indicatorDTO) {
        var createdIndicator = indicatorService.createIndicator(indicatorDTO);

        return IndicatorConverter.toDTO(createdIndicator);
    }

    @PutMapping("/{indicatorId}")
    @PreAuthorize("hasAuthority('EDIT_INDICATORS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateIndicator(@RequestBody IndicatorDTO indicatorDTO,
                                @PathVariable Integer indicatorId) {
        indicatorService.updateIndicator(indicatorId,
            indicatorDTO);
    }

    @DeleteMapping("/{indicatorId}")
    @PreAuthorize("hasAuthority('EDIT_INDICATORS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIndicator(@PathVariable Integer indicatorId) {
        indicatorService.deleteIndicator(indicatorId);
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('SUBMIT_ENTITY_INDICATORS')")
    public List<IndicatorResponseDTO> getIndicatorsApplicableToEntity(
        @RequestParam("applicableType") List<ApplicableEntityType> applicableEntityTypes) {
        return indicatorService.getIndicatorsApplicableToEntity(applicableEntityTypes);
    }
}
