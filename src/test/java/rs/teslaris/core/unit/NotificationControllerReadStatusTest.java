package rs.teslaris.core.unit;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rs.teslaris.core.controller.utility.NotificationController;
import rs.teslaris.core.dto.commontypes.NotificationDTO;
import rs.teslaris.core.model.commontypes.NotificationReadStatus;
import rs.teslaris.core.model.commontypes.NotificationSentiment;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.util.jwt.JwtUtil;

@ExtendWith(MockitoExtension.class)
class NotificationControllerReadStatusTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private JwtUtil tokenUtil;

    private MockMvc mockMvc;


    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
            new NotificationController(notificationService, tokenUtil))
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();
    }

    @Test
    void shouldDefaultToUnreadNotifications() throws Exception {
        when(tokenUtil.extractUserIdFromToken("Bearer token")).thenReturn(42);
        when(notificationService.getUserNotifications(42, NotificationReadStatus.UNREAD, PageRequest.of(0, 10)))
            .thenReturn(Page.empty(PageRequest.of(0, 10)));

        mockMvc.perform(get("/api/notification").header("Authorization", "Bearer token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty());

        verify(notificationService).getUserNotifications(42, NotificationReadStatus.UNREAD, PageRequest.of(0, 10));
    }

    @ParameterizedTest
    @EnumSource(NotificationReadStatus.class)
    void shouldAcceptReadStatusFilter(NotificationReadStatus readStatus) throws Exception {
        when(tokenUtil.extractUserIdFromToken("Bearer token")).thenReturn(42);
        when(notificationService.getUserNotifications(42, readStatus, PageRequest.of(0, 10))).thenReturn(Page.empty(PageRequest.of(0, 10)));

        mockMvc.perform(get("/api/notification")
                .header("Authorization", "Bearer token")
                .param("readStatus", readStatus.name()))
            .andExpect(status().isOk());

        verify(notificationService).getUserNotifications(42, readStatus, PageRequest.of(0, 10));
    }

    @Test
    void shouldExposeReadTimestamp() throws Exception {
        when(tokenUtil.extractUserIdFromToken("Bearer token")).thenReturn(42);
        var notification = new NotificationDTO(1, "Notification", "", List.of(),
            LocalDateTime.of(2026, 10, 8, 10, 0), LocalDateTime.of(2026, 10, 8, 12, 0), "Duration: 2s", NotificationSentiment.SUCCESS);
        when(notificationService.getUserNotifications(42, NotificationReadStatus.READ, PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(notification), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/notification")
                .header("Authorization", "Bearer token")
                .param("readStatus", "READ"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].readAt").exists())
            .andExpect(jsonPath("$.content[0].details").value("Duration: 2s"))
            .andExpect(jsonPath("$.content[0].sentiment").value("SUCCESS"))
            .andExpect(jsonPath("$.content[0].possibleActions").isEmpty());
    }

    @Test
    void shouldPassPaginationAndExposePageMetadata() throws Exception {
        when(tokenUtil.extractUserIdFromToken("Bearer token")).thenReturn(42);
        var pageable = PageRequest.of(1, 2);
        var notification = new NotificationDTO(3, "Older notification", "", List.of(),
            LocalDateTime.of(2026, 10, 7, 10, 0), LocalDateTime.of(2026, 10, 8, 12, 0), null, NotificationSentiment.INFO);
        when(notificationService.getUserNotifications(42, NotificationReadStatus.READ, pageable))
            .thenReturn(new PageImpl<>(List.of(notification), pageable, 3));

        mockMvc.perform(get("/api/notification")
                .header("Authorization", "Bearer token")
                .param("readStatus", "READ").param("page", "1").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(3))
            .andExpect(jsonPath("$.number").value(1))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2));

        verify(notificationService).getUserNotifications(42, NotificationReadStatus.READ, pageable);
    }

    @Test
    void shouldRejectInvalidReadStatus() throws Exception {
        mockMvc.perform(get("/api/notification").param("readStatus", "INVALID"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(notificationService);
    }
}
