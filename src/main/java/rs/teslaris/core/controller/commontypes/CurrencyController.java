package rs.teslaris.core.controller.commontypes;

import jakarta.validation.Valid;
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
import rs.teslaris.core.dto.commontypes.CurrencyDTO;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;

@RestController
@RequestMapping("/api/currency")
@RequiredArgsConstructor
@Traceable
public class CurrencyController {

    private final CurrencyService currencyService;


    @GetMapping("/{currencyId}")
    public CurrencyDTO readCurrency(@PathVariable Integer currencyId) {
        return currencyService.readCurrencyById(currencyId);
    }

    @GetMapping
    public List<CurrencyDTO> getAllCurrencies() {
        return currencyService.getAllCurrencies();
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('EDIT_CURRENCIES')")
    public Page<CurrencyDTO> searchCurrencies(Pageable pageable,
                                              @RequestParam("tokens") List<String> tokens,
                                              @RequestParam("lang") String language) {
        return currencyService.searchCurrencies(pageable, Strings.join(tokens, ' '),
            language.toUpperCase());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_CURRENCIES')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public CurrencyDTO createCurrency(@RequestBody @Valid CurrencyDTO currencyDTO) {
        return currencyService.createCurrency(currencyDTO);
    }

    @PutMapping("/{currencyId}")
    @PreAuthorize("hasAuthority('EDIT_CURRENCIES')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCurrency(@RequestBody @Valid CurrencyDTO currencyDTO,
                               @PathVariable Integer currencyId) {
        currencyService.updateCurrency(currencyId, currencyDTO);
    }

    @DeleteMapping("/{currencyId}")
    @PreAuthorize("hasAuthority('EDIT_CURRENCIES')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCurrency(@PathVariable Integer currencyId) {
        currencyService.deleteCurrency(currencyId);
    }
}
