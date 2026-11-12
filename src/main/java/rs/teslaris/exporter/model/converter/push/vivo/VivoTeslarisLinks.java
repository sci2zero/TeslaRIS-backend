package rs.teslaris.exporter.model.converter.push.vivo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rs.teslaris.exporter.model.converter.push.vivo.VivoRdfSupport.Link;

@Component
public class VivoTeslarisLinks {

    private final String frontendAddress;

    private final String frontendLanguage;

    private final String repositoryName;


    public VivoTeslarisLinks(@Value("${frontend.application.address}") String frontendAddress,
                             @Value("${client.localization.languages}") String clientLanguages,
                             @Value("${export.repo.name}") String repositoryName) {
        this.frontendAddress = frontendAddress;
        this.frontendLanguage = clientLanguages.split(",")[0].trim();
        this.repositoryName = repositoryName;
    }

    public Link profileLink(String entityPath, Integer databaseId) {
        return new Link("teslaris", repositoryName,
            frontendAddress + frontendLanguage + "/" + entityPath + "/" + databaseId);
    }
}
