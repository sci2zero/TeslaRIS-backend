package rs.teslaris.core.controller;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.LanguageResponseDTO;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;

@RestController
@RequestMapping("/api/language")
@RequiredArgsConstructor
public class LanguageController {

    private final LanguageService languageService;

    @GetMapping
    public List<LanguageResponseDTO> getAllLanguages() {
        var allLanguages = languageService.findAll();
        return allLanguages.stream().map(language -> new LanguageResponseDTO(
            language.getId(),
            language.getLanguageCode(),
            MultilingualContentConverter.getMultilingualContentDTO(language.getName()))).collect(
            Collectors.toList());
    }
}
