package rs.teslaris.importer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoadingConfigurationDTO {

    @NotNull(message = "You have to specify whether smart loading is enabled.")
    private Boolean smartLoadingByDefault;

    @NotNull(message = "You have to specify whether loaded entities are unmanaged.")
    private Boolean loadedEntitiesAreUnmanaged;
}
