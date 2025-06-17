package rs.teslaris.core.controller;

import java.util.List;
import java.util.stream.Collectors;
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
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.LanguageResponseDTO;
import rs.teslaris.core.dto.commontypes.LanguageTagDTO;
import rs.teslaris.core.dto.commontypes.LanguageTagResponseDTO;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;

@RestController
@RequestMapping("/api/language")
@RequiredArgsConstructor
@Traceable
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

    @GetMapping("/ui-languages")
    public List<LanguageResponseDTO> getUISupportedLanguages() {
        return languageService.getUISupportedLanguages();
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

    @GetMapping("/tags/search")
    @PreAuthorize("hasAuthority('EDIT_LANGUAGE_TAGS')")
    public Page<LanguageTagResponseDTO> searchLanguageTags(Pageable pageable,
                                                           @RequestParam("tokens")
                                                           List<String> tokens) {
        return languageTagService.searchLanguageTags(pageable, Strings.join(tokens, ' '));
    }

    @PostMapping("/tag")
    @PreAuthorize("hasAuthority('EDIT_LANGUAGE_TAGS')")
    @ResponseStatus(HttpStatus.CREATED)
    public LanguageTagResponseDTO createLanguageTag(@RequestBody LanguageTagDTO languageTagDTO) {
        var createdLanguageTag = languageTagService.createLanguageTag(languageTagDTO);
        return new LanguageTagResponseDTO(createdLanguageTag.getId(),
            createdLanguageTag.getLanguageTag(), languageTagDTO.getDisplay());
    }

    @PutMapping("/tag/{languageTagId}")
    @PreAuthorize("hasAuthority('EDIT_LANGUAGE_TAGS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLanguageTag(@PathVariable Integer languageTagId,
                                  @RequestBody LanguageTagDTO languageTagDto) {
        languageTagService.updateLanguageTag(languageTagId, languageTagDto);
    }

    @DeleteMapping("/tag/{languageTagId}")
    @PreAuthorize("hasAuthority('EDIT_LANGUAGE_TAGS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLanguageTag(@PathVariable Integer languageTagId) {
        languageTagService.deleteLanguageTag(languageTagId);
    }
}
