package rs.teslaris.core.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
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
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.CountryDTO;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;

@RestController
@RequestMapping("/api/country")
@RequiredArgsConstructor
@Traceable
public class CountryController {

    private final CountryService countryService;


    @GetMapping("/{countryId}")
    public CountryDTO readCountry(@PathVariable Integer countryId) {
        return countryService.readCountryById(countryId);
    }

    @GetMapping
    public List<CountryDTO> readAll() {
        return countryService.readAllCountries();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('EDIT_COUNTRIES')")
    public Page<CountryDTO> searchCountries(Pageable pageable,
                                            @RequestParam("tokens") List<String> tokens,
                                            @RequestParam("lang") String language) {
        return countryService.searchCountries(pageable, Strings.join(tokens, ' '),
            language.toUpperCase());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_COUNTRIES')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public CountryDTO createCountry(@RequestBody CountryDTO countryDTO) {
        return countryService.createCountry(countryDTO);
    }

    @PutMapping("/{countryId}")
    @PreAuthorize("hasAuthority('EDIT_COUNTRIES')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCountry(@RequestBody CountryDTO countryDTO, @PathVariable Integer countryId) {
        countryService.updateCountry(countryId, countryDTO);
    }

    @DeleteMapping("/{countryId}")
    @PreAuthorize("hasAuthority('EDIT_COUNTRIES')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCountry(@PathVariable Integer countryId) {
        countryService.deleteCountry(countryId);
    }
}
