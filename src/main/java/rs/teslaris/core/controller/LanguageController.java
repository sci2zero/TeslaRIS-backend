package rs.teslaris.core.controller;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.LanguageResponseDTO;
import rs.teslaris.core.dto.commontypes.LanguageTagResponseDTO;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;

@RestController
@RequestMapping("/api/language")
@RequiredArgsConstructor
public class LanguageController {

    private final LanguageService languageService;

    private final LanguageTagService languageTagService;

    @GetMapping
    public List<LanguageResponseDTO> getAllLanguages() {
        var allLanguages = languageService.findAll();
        return allLanguages.stream().map(language -> new LanguageResponseDTO(
            language.getId(),
            language.getLanguageCode(),
            MultilingualContentConverter.getMultilingualContentDTO(language.getName()))).collect(
            Collectors.toList());
    }

    @GetMapping("/tags")
    public List<LanguageTagResponseDTO> getAllLanguageTags() {
        var allLanguageTags = languageTagService.findAll();
        return allLanguageTags.stream().map(language -> new LanguageTagResponseDTO(
            language.getId(),
            language.getLanguageTag(),
            language.getDisplay())).collect(
            Collectors.toList());
    }
}
