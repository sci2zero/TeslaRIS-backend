package rs.teslaris.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.commontypes.CountryDTO;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;

@RestController
@RequestMapping("/api/country")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;

    @GetMapping("/{countryId}")
    public CountryDTO readCountry(@PathVariable Integer countryId) {
        return countryService.readCountryById(countryId);
    }
}
