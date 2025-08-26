package rs.teslaris.assessment.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.service.impl.document.PersonChartServiceImpl;
import rs.teslaris.core.service.interfaces.document.PersonChartService;

@RestController
@RequestMapping("/api/visualization-data")
@RequiredArgsConstructor
public class ChartDataController {

    private final PersonChartService personChartService;

    @GetMapping("/person/publication-count/{personId}")
    public List<PersonChartServiceImpl.YearlyCounts> getPublicationCountsForPerson(
        @PathVariable Integer personId) {
        return personChartService.getPublicationCountsForPerson(personId, 2010, 2025);
    }
}
