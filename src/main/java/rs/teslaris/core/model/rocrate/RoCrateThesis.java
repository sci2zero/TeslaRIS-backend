package rs.teslaris.core.model.rocrate;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoCrateThesis extends RoCratePublicationBase {

    private ContextualEntity sourceOrganization;

    private String archivedAt;

    private String displayLocation;

    private String inSupportOf;

    private String educationalLevel;

    private String inLanguage;

    private Boolean isAccessibleForFree = false;

    private ContextualEntity license;

    public RoCrateThesis() {
        this.setType("Thesis");
    }
}
