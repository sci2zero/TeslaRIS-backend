package rs.teslaris.core.controller.assessment;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.converter.assessment.IndicatorConverter;
import rs.teslaris.core.dto.assessment.IndicatorDTO;
import rs.teslaris.core.service.interfaces.assessment.IndicatorService;

@RestController
@RequestMapping("api/assessment/indicator")
@RequiredArgsConstructor
public class IndicatorController {

    private final IndicatorService indicatorService;


    @GetMapping
    public Page<IndicatorDTO> readIndicators(Pageable pageable) {
        return indicatorService.readAllIndicators(pageable);
    }

    @GetMapping("/{indicatorId}")
    public IndicatorDTO readIndicator(@PathVariable Integer indicatorId) {
        return indicatorService.readIndicatorById(indicatorId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IndicatorDTO createIndicator(@RequestBody IndicatorDTO indicatorDTO) {
        var createdIndicator = indicatorService.createIndicator(indicatorDTO);

        return IndicatorConverter.toDTO(createdIndicator);
    }

    @PutMapping("/{indicatorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateIndicator(@RequestBody IndicatorDTO indicatorDTO,
                                @PathVariable Integer indicatorId) {
        indicatorService.updateIndicator(indicatorId,
            indicatorDTO);
    }

    @DeleteMapping("/{indicatorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIndicator(@PathVariable Integer indicatorId) {
        indicatorService.deleteIndicator(indicatorId);
    }
}
