package rs.teslaris.core.unit.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.importer.service.impl.CommonHarvesterImpl;
import rs.teslaris.importer.service.interfaces.BibTexHarvester;
import rs.teslaris.importer.service.interfaces.CSVHarvester;
import rs.teslaris.importer.service.interfaces.EndNoteHarvester;
import rs.teslaris.importer.service.interfaces.OpenAlexHarvester;
import rs.teslaris.importer.service.interfaces.RefManHarvester;
import rs.teslaris.importer.service.interfaces.ScopusHarvester;
import rs.teslaris.importer.service.interfaces.WebOfScienceHarvester;

@SpringBootTest
public class CommonHarvesterTest {

    @Mock
    private ScopusHarvester scopusHarvester;

    @Mock
    private OpenAlexHarvester openAlexHarvester;

    @Mock
    private WebOfScienceHarvester webOfScienceHarvester;

    @Mock
    private BibTexHarvester bibTexHarvester;

    @Mock
    private RefManHarvester refManHarvester;

    @Mock
    private EndNoteHarvester endNoteHarvester;

    @Mock
    private CSVHarvester csvHarvester;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private CommonHarvesterImpl commonHarvester;

    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Integer userId;
    private Integer institutionId;


    @BeforeEach
    void setUp() {
        dateFrom = LocalDate.of(2023, 1, 1);
        dateTo = LocalDate.of(2023, 12, 31);
        userId = 1;
        institutionId = 100;
    }

    @Test
    void shouldReturnZeroWhenPerformHarvestWithUnsupportedUserRole() {
        // given
        String unsupportedRole = "UNSUPPORTED_ROLE";

        // when
        Integer result = commonHarvester.performHarvest(userId, unsupportedRole, dateFrom, dateTo,
            institutionId);

        // then
        assertEquals(0, result);
    }

    @Test
    void shouldPerformHarvestForResearcherRole() {
        // given
        HashMap<Integer, Integer> scopusResults = new HashMap<>(Map.of(userId, 5));
        HashMap<Integer, Integer> openAlexResults = new HashMap<>(Map.of(userId, 3));
        HashMap<Integer, Integer> wosResults = new HashMap<>(Map.of(userId, 2));

        when(scopusHarvester.harvestDocumentsForAuthor(eq(userId), eq(dateFrom), eq(dateTo), any()))
            .thenReturn(scopusResults);
        when(openAlexHarvester.harvestDocumentsForAuthor(eq(userId), eq(dateFrom), eq(dateTo),
            any()))
            .thenReturn(openAlexResults);
        when(webOfScienceHarvester.harvestDocumentsForAuthor(eq(userId), eq(dateFrom), eq(dateTo),
            any()))
            .thenReturn(wosResults);
        when(userService.findOne(userId)).thenReturn(new User() {{
            setPreferredUILanguage(new LanguageTag() {{
                setLanguageTag(LanguageAbbreviations.SERBIAN);
            }});
        }});

        // when
        Integer result =
            commonHarvester.performHarvest(userId, UserRole.RESEARCHER.name(), dateFrom, dateTo,
                institutionId);

        // then
        assertEquals(10, result);
        verify(scopusHarvester).harvestDocumentsForAuthor(userId, dateFrom, dateTo,
            new HashMap<>());
        verify(openAlexHarvester).harvestDocumentsForAuthor(userId, dateFrom, dateTo,
            new HashMap<>());
        verify(webOfScienceHarvester).harvestDocumentsForAuthor(userId, dateFrom, dateTo,
            new HashMap<>());
        verify(notificationService).createNotification(any());
    }

    @Test
    void shouldPerformHarvestForInstitutionalEditorRole() {
        // given
        HashMap<Integer, Integer> scopusResults = new HashMap<>(Map.of(userId, 4));
        HashMap<Integer, Integer> openAlexResults = new HashMap<>(Map.of(userId, 2));
        HashMap<Integer, Integer> wosResults = new HashMap<>(Map.of(userId, 1));

        when(scopusHarvester.harvestDocumentsForInstitutionalEmployee(eq(userId), eq(null),
            eq(dateFrom), eq(dateTo), any()))
            .thenReturn(scopusResults);
        when(openAlexHarvester.harvestDocumentsForInstitutionalEmployee(eq(userId), eq(null),
            eq(dateFrom), eq(dateTo), any()))
            .thenReturn(openAlexResults);
        when(webOfScienceHarvester.harvestDocumentsForInstitutionalEmployee(eq(userId), eq(null),
            eq(dateFrom), eq(dateTo), any()))
            .thenReturn(wosResults);
        when(userService.findOne(userId)).thenReturn(new User() {{
            setPreferredUILanguage(new LanguageTag() {{
                setLanguageTag(LanguageAbbreviations.SERBIAN);
            }});
        }});

        // when
        Integer result =
            commonHarvester.performHarvest(userId, UserRole.INSTITUTIONAL_EDITOR.name(), dateFrom,
                dateTo, institutionId);

        // then
        assertEquals(7, result);
        verify(scopusHarvester).harvestDocumentsForInstitutionalEmployee(userId, null, dateFrom,
            dateTo, new HashMap<>());
        verify(openAlexHarvester).harvestDocumentsForInstitutionalEmployee(userId, null, dateFrom,
            dateTo, new HashMap<>());
        verify(webOfScienceHarvester).harvestDocumentsForInstitutionalEmployee(userId, null,
            dateFrom, dateTo, new HashMap<>());
    }

    @Test
    void shouldPerformHarvestForAdminRole() {
        // given
        HashMap<Integer, Integer> scopusResults = new HashMap<>(Map.of(userId, 6));
        HashMap<Integer, Integer> openAlexResults = new HashMap<>(Map.of(userId, 4));
        HashMap<Integer, Integer> wosResults = new HashMap<>(Map.of(userId, 3));

        when(scopusHarvester.harvestDocumentsForInstitutionalEmployee(eq(userId), eq(institutionId),
            eq(dateFrom), eq(dateTo), any()))
            .thenReturn(scopusResults);
        when(openAlexHarvester.harvestDocumentsForInstitutionalEmployee(eq(userId),
            eq(institutionId), eq(dateFrom), eq(dateTo), any()))
            .thenReturn(openAlexResults);
        when(webOfScienceHarvester.harvestDocumentsForInstitutionalEmployee(eq(userId),
            eq(institutionId), eq(dateFrom), eq(dateTo), any()))
            .thenReturn(wosResults);
        when(userService.findOne(userId)).thenReturn(new User() {{
            setPreferredUILanguage(new LanguageTag() {{
                setLanguageTag(LanguageAbbreviations.SERBIAN);
            }});
        }});

        // when
        Integer result =
            commonHarvester.performHarvest(userId, UserRole.ADMIN.name(), dateFrom, dateTo,
                institutionId);

        // then
        assertEquals(13, result);
        verify(scopusHarvester).harvestDocumentsForInstitutionalEmployee(userId, institutionId,
            dateFrom, dateTo, new HashMap<>());
        verify(openAlexHarvester).harvestDocumentsForInstitutionalEmployee(userId, institutionId,
            dateFrom, dateTo, new HashMap<>());
        verify(webOfScienceHarvester).harvestDocumentsForInstitutionalEmployee(userId,
            institutionId, dateFrom, dateTo, new HashMap<>());
    }

    @Test
    void shouldReturnZeroWhenNoDocumentsImported() {
        // given
        when(scopusHarvester.harvestDocumentsForAuthor(eq(userId), eq(dateFrom), eq(dateTo), any()))
            .thenReturn(new HashMap<>());
        when(openAlexHarvester.harvestDocumentsForAuthor(eq(userId), eq(dateFrom), eq(dateTo),
            any()))
            .thenReturn(new HashMap<>());
        when(webOfScienceHarvester.harvestDocumentsForAuthor(eq(userId), eq(dateFrom), eq(dateTo),
            any()))
            .thenReturn(new HashMap<>());
        when(userService.findOne(userId)).thenReturn(new User() {{
            setPreferredUILanguage(new LanguageTag() {{
                setLanguageTag(LanguageAbbreviations.SERBIAN);
            }});
        }});

        // when
        Integer result =
            commonHarvester.performHarvest(userId, UserRole.RESEARCHER.name(), dateFrom, dateTo,
                institutionId);

        // then
        assertEquals(0, result);
        verify(notificationService).createNotification(any());
    }

    @Test
    void shouldReturnZeroWhenPerformAuthorCentricHarvestWithUnsupportedUserRole() {
        // given
        String unsupportedRole = "UNSUPPORTED_ROLE";
        List<Integer> authorIds = List.of(1, 2, 3);

        // when
        Integer result =
            commonHarvester.performAuthorCentricHarvest(userId, unsupportedRole, dateFrom, dateTo,
                authorIds, false, institutionId);

        // then
        assertEquals(0, result);
    }

    @Test
    void shouldPerformAuthorCentricHarvestForInstitutionalEditorRole() {
        // given
        List<Integer> authorIds = List.of(1, 2, 3);
        HashMap<Integer, Integer> scopusResults = new HashMap<>(Map.of(userId, 5));
        HashMap<Integer, Integer> openAlexResults = new HashMap<>(Map.of(userId, 3));
        HashMap<Integer, Integer> wosResults = new HashMap<>(Map.of(userId, 2));

        when(scopusHarvester.harvestDocumentsForInstitution(eq(userId), eq(null), eq(dateFrom),
            eq(dateTo), eq(authorIds), eq(false), any()))
            .thenReturn(scopusResults);
        when(openAlexHarvester.harvestDocumentsForInstitution(eq(userId), eq(null), eq(dateFrom),
            eq(dateTo), eq(authorIds), eq(false), any()))
            .thenReturn(openAlexResults);
        when(
            webOfScienceHarvester.harvestDocumentsForInstitution(eq(userId), eq(null), eq(dateFrom),
                eq(dateTo), eq(authorIds), eq(false), any()))
            .thenReturn(wosResults);
        when(userService.findOne(userId)).thenReturn(new User() {{
            setPreferredUILanguage(new LanguageTag() {{
                setLanguageTag(LanguageAbbreviations.SERBIAN);
            }});
        }});

        // when
        Integer result = commonHarvester.performAuthorCentricHarvest(userId,
            UserRole.INSTITUTIONAL_EDITOR.name(), dateFrom, dateTo, authorIds, false,
            institutionId);

        // then
        assertEquals(10, result);
        verify(scopusHarvester).harvestDocumentsForInstitution(userId, null, dateFrom, dateTo,
            authorIds, false, new HashMap<>());
        verify(openAlexHarvester).harvestDocumentsForInstitution(userId, null, dateFrom, dateTo,
            authorIds, false, new HashMap<>());
        verify(webOfScienceHarvester).harvestDocumentsForInstitution(userId, null, dateFrom, dateTo,
            authorIds, false, new HashMap<>());
    }

    @Test
    void shouldPerformAuthorCentricHarvestForAdminRole() {
        // given
        List<Integer> authorIds = List.of(1, 2, 3);
        HashMap<Integer, Integer> scopusResults = new HashMap<>(Map.of(userId, 7));
        HashMap<Integer, Integer> openAlexResults = new HashMap<>(Map.of(userId, 4));
        HashMap<Integer, Integer> wosResults = new HashMap<>(Map.of(userId, 3));

        when(scopusHarvester.harvestDocumentsForInstitution(eq(userId), eq(institutionId),
            eq(dateFrom), eq(dateTo), eq(authorIds), eq(true), any()))
            .thenReturn(scopusResults);
        when(openAlexHarvester.harvestDocumentsForInstitution(eq(userId), eq(institutionId),
            eq(dateFrom), eq(dateTo), eq(authorIds), eq(true), any()))
            .thenReturn(openAlexResults);
        when(webOfScienceHarvester.harvestDocumentsForInstitution(eq(userId), eq(institutionId),
            eq(dateFrom), eq(dateTo), eq(authorIds), eq(true), any()))
            .thenReturn(wosResults);
        when(userService.findOne(userId)).thenReturn(new User() {{
            setPreferredUILanguage(new LanguageTag() {{
                setLanguageTag(LanguageAbbreviations.SERBIAN);
            }});
        }});

        // when
        Integer result =
            commonHarvester.performAuthorCentricHarvest(userId, UserRole.ADMIN.name(), dateFrom,
                dateTo, authorIds, true, institutionId);

        // then
        assertEquals(14, result);
        verify(scopusHarvester).harvestDocumentsForInstitution(userId, institutionId, dateFrom,
            dateTo, authorIds, true, new HashMap<>());
        verify(openAlexHarvester).harvestDocumentsForInstitution(userId, institutionId, dateFrom,
            dateTo, authorIds, true, new HashMap<>());
        verify(webOfScienceHarvester).harvestDocumentsForInstitution(userId, institutionId,
            dateFrom, dateTo, authorIds, true, new HashMap<>());
    }

    @Test
    void shouldProcessBibTexFile() {
        // given
        String filename = "publications.bib";
        HashMap<Integer, Integer> counts = new HashMap<>();

        // when
        commonHarvester.processVerifiedFile(userId, multipartFile, filename, counts);

        // then
        verify(bibTexHarvester).harvestDocumentsForAuthor(userId, multipartFile, counts);
    }

    @Test
    void shouldProcessRisFile() {
        // given
        String filename = "publications.ris";
        HashMap<Integer, Integer> counts = new HashMap<>();

        // when
        commonHarvester.processVerifiedFile(userId, multipartFile, filename, counts);

        // then
        verify(refManHarvester).harvestDocumentsForAuthor(userId, multipartFile, counts);
    }

    @Test
    void shouldProcessEndNoteFile() {
        // given
        String filename = "publications.enw";
        HashMap<Integer, Integer> counts = new HashMap<>();

        // when
        commonHarvester.processVerifiedFile(userId, multipartFile, filename, counts);

        // then
        verify(endNoteHarvester).harvestDocumentsForAuthor(userId, multipartFile, counts);
    }

    @Test
    void shouldProcessCsvFile() {
        // given
        String filename = "publications.csv";
        HashMap<Integer, Integer> counts = new HashMap<>();

        // when
        commonHarvester.processVerifiedFile(userId, multipartFile, filename, counts);

        // then
        verify(csvHarvester).harvestDocumentsForAuthor(userId, multipartFile, counts);
    }
}
