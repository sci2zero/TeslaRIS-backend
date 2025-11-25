package rs.teslaris.core.model.institution;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailConfiguration {

    private Boolean validateEmailDomain = false;

    private Boolean allowSubdomains = false;

    private String institutionEmailDomain;
}
