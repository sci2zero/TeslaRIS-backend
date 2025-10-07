package rs.teslaris.reporting.dto.configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FullChartDisplaySettingsDTO {

    private PersonChartDisplaySettingsDTO personChartDisplaySettings;

    private OUChartDisplaySettingsDTO ouChartDisplaySettings;

    private DocumentChartDisplaySettingsDTO documentChartDisplaySettings;
}
