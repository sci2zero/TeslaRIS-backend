package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import rs.teslaris.core.controller.utility.ApplicationConfigurationController;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.util.SchedulingTime;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.core.service.interfaces.commontypes.ApplicationConfigurationService;

class SchedulingTimeTest {

    @ParameterizedTest
    @CsvSource({
        "Europe/Belgrade,2026-03-28T15:00:00,23",
        "Europe/Belgrade,2026-10-24T15:00:00,25",
        "Asia/Shanghai,2026-03-28T15:00:00,24",
        "America/New_York,2026-03-07T15:00:00,23"
    })
    void dailyRecurrenceKeepsLocalHourAcrossOffsetChanges(String zoneName, String local,
                                                         long elapsedHours) {
        var zone = ZoneId.of(zoneName);
        var start = LocalDateTime.parse(local).atZone(zone).toInstant();
        var next = SchedulingTime.nextExecution(start, RecurrenceType.DAILY, zone);
        assertEquals(15, next.atZone(zone).getHour());
        assertEquals(elapsedHours, Duration.between(start, next).toHours());
    }

    @Test
    void restoringSchedulePreservesTimeAndZoneAndClearsContextOnFailure() {
        var zone = ZoneId.of("Asia/Shanghai");
        var time = Instant.now().plusSeconds(3600);
        assertThrows(IllegalStateException.class, () ->
            SchedulingTime.restoreInZone(zone, time, () -> {
                assertEquals(zone, SchedulingTime.requestZone());
                assertEquals(time, SchedulingTime.restoredExecutionTime());
                throw new IllegalStateException("restore failed");
            }));
        assertNull(SchedulingTime.restoredExecutionTime());
        assertEquals(ZoneId.systemDefault(), SchedulingTime.requestZone());
    }

    @Test
    void timezoneIsReadFromBrowserRequest() {
        var request = new MockHttpServletRequest();
        request.setParameter("timezone", "Asia/Shanghai");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            assertEquals(ZoneId.of("Asia/Shanghai"), SchedulingTime.requestZone());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void legacyRowsUseOriginalServerZoneAndNewRowsAreIndependentOfServerZone() {
        var original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Belgrade"));
            var metadata = new ScheduledTaskMetadata();
            metadata.setLegacyTimeToRun(LocalDateTime.parse("2026-12-01T15:00:00"));
            assertEquals(Instant.parse("2026-12-01T14:00:00Z"), metadata.getTimeToRun());
            metadata.setTimeToRun(metadata.getTimeToRun());
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
            assertEquals(Instant.parse("2026-12-01T14:00:00Z"), metadata.getTimeToRun());
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @ParameterizedTest
    @CsvSource({"2026-12-01T14:00:00Z", "2026-12-01T15:00:00+01:00"})
    void schedulingEndpointBindsExplicitInstants(String timestamp) throws Exception {
        var service = mock(ApplicationConfigurationService.class);
        var jwt = mock(JwtUtil.class);
        when(jwt.extractUserIdFromToken("Bearer test")).thenReturn(1);
        var mvc = MockMvcBuilders.standaloneSetup(
            new ApplicationConfigurationController(service, jwt)).build();
        mvc.perform(post("/api/app-configuration/maintenance/schedule")
                .header("Authorization", "Bearer test")
                .param("timestamp", timestamp)
                .param("approximateEndMoment", "one hour"))
            .andExpect(status().isAccepted());
        verify(service).scheduleMaintenanceMode(Instant.parse("2026-12-01T14:00:00Z"),
            "one hour", 1);
    }

    @Test
    void schedulingEndpointRejectsTimestampWithoutOffset() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ApplicationConfigurationController(
            mock(ApplicationConfigurationService.class), mock(JwtUtil.class))).build();
        mvc.perform(post("/api/app-configuration/maintenance/schedule")
                .header("Authorization", "Bearer test")
                .param("timestamp", "2026-12-01T15:00:00")
                .param("approximateEndMoment", "one hour"))
            .andExpect(status().isBadRequest());
    }
}
