package rs.teslaris.core.unit;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.service.impl.commontypes.MultilingualContentServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;

@SpringBootTest
public class MultilingualContentServiceTest {

    @Mock
    private LanguageTagService languageTagService;

    @InjectMocks
    private MultilingualContentServiceImpl multilingualContentService;


    @Test
    public void shouldGetMultilingualContentSetWhenProvidedWithValidData() {
        var multilingualContentDTO =
            new ArrayList<>(List.of(new MultilingualContentDTO(1, "aaa", 1)));

        var languageTag = new LanguageTag();
        when(languageTagService.findOne(1)).thenReturn(languageTag);

        var result = multilingualContentService.getMultilingualContent(multilingualContentDTO);

        assertEquals(result.size(), 1);
        assertEquals(
            result.stream().map(mc -> mc.getLanguage()).collect(Collectors.toList()).get(0),
            languageTag);
    }
}
