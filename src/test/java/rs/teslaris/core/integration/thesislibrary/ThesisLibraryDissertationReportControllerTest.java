package rs.teslaris.core.integration.thesislibrary;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import rs.teslaris.core.integration.BaseTest;

@SpringBootTest
public class ThesisLibraryDissertationReportControllerTest extends BaseTest {

    static Stream<Arguments> requestParamCombinations() {
        return Stream.of(
            // No parameters
            Arguments.of(null, null, null),
            // Only institutionId
            Arguments.of(1, null, null),
            // Only year
            Arguments.of(null, 2024, null),
            // Only notDefendedOnly
            Arguments.of(null, null, true),
            Arguments.of(null, null, false),
            // institutionId + year
            Arguments.of(2, 2022, null),
            // institutionId + notDefendedOnly
            Arguments.of(2, null, true),
            // year + notDefendedOnly
            Arguments.of(null, 2023, false),
            // All three params
            Arguments.of(3, 2021, true),
            Arguments.of(4, 2020, false)
        );
    }

    @ParameterizedTest
    @MethodSource("requestParamCombinations")
    void testFetchPublicReviewDissertations(Integer institutionId, Integer year,
                                            Boolean notDefendedOnly) throws Exception {
        var requestBuilder = MockMvcRequestBuilders
            .get("http://localhost:8081/api/thesis-library/dissertation-report")
            .contentType(MediaType.APPLICATION_JSON)
            .param("page", "0")
            .param("size", "10");

        if (Objects.nonNull(institutionId)) {
            requestBuilder.param("institutionId", institutionId.toString());
        }
        if (Objects.nonNull(year)) {
            requestBuilder.param("year", year.toString());
        }
        if (Objects.nonNull(notDefendedOnly)) {
            requestBuilder.param("notDefendedOnly", notDefendedOnly.toString());
        }

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk());
    }
}
