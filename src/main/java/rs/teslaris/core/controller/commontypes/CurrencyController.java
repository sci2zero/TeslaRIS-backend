package rs.teslaris.core.controller.commontypes;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.commontypes.CurrencyDTO;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;

import java.util.List;

@RestController
@RequestMapping("/api/currency")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping
    public List<CurrencyDTO> getAllCurrencies() {
        return currencyService.getAllCurrencies();
    }

}
